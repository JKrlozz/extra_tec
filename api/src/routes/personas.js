// alumnos y docentes (solo administrador)
const express = require('express');
const { consulta, transaccion } = require('../db');
const { solo, generarHash, claveInicial } = require('../auth');
const { ErrorApi, ruta, revisar, idDe } = require('../util');
const V = require('../validar');

const router = express.Router();
router.use('/alumnos', solo('admin'));
router.use('/docentes', solo('admin'));
router.use('/especialidades', solo('admin'));

// el texto buscado se toma literal
function patron(filtro) {
  return '%' + V.texto(filtro).trim().replace(/[\\%_[]/g, (c) => '\\' + c) + '%';
}

const NOMBRE_BUSCADO = "(u.nombres + ' ' + u.apellido_paterno + ' ' + COALESCE(u.apellido_materno, ''))";

router.get('/alumnos', ruta(async (req, res) => {
  const filas = await consulta(
    `SELECT u.id, u.nombres, u.apellido_paterno AS apellidoPaterno, COALESCE(u.apellido_materno, '') AS apellidoMaterno,
            u.correo, a.matricula, a.semestre, c.id AS idCarrera, c.nombre AS carrera
       FROM alumnos a
       JOIN usuarios u ON u.id = a.id_usuario
       JOIN carreras c ON c.id = a.id_carrera
      WHERE u.activo = 1
        AND (${NOMBRE_BUSCADO} LIKE @p ESCAPE '\\' OR a.matricula LIKE @p ESCAPE '\\')
      ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres`,
    { p: patron(req.query.filtro) },
  );
  res.json(filas);
}));

router.get('/docentes', ruta(async (req, res) => {
  const filas = await consulta(
    `SELECT u.id, u.nombres, u.apellido_paterno AS apellidoPaterno, COALESCE(u.apellido_materno, '') AS apellidoMaterno,
            u.correo, COALESCE(e.nombre, '') AS especialidad, COALESCE(d.telefono, '') AS telefono
       FROM docentes d
       JOIN usuarios u ON u.id = d.id_usuario
       LEFT JOIN especialidades e ON e.id = d.id_especialidad
      WHERE u.activo = 1
        AND (${NOMBRE_BUSCADO} LIKE @p ESCAPE '\\' OR u.correo LIKE @p ESCAPE '\\')
      ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres`,
    { p: patron(req.query.filtro) },
  );
  res.json(filas);
}));

// catalogo para sugerir especialidades
router.get('/especialidades', ruta(async (req, res) => {
  const filas = await consulta('SELECT nombre FROM especialidades ORDER BY nombre');
  res.json(filas.map((f) => f.nombre));
}));

const datosUsuario = (b) => ({
  nombres: V.texto(b.nombres).trim(),
  paterno: V.texto(b.apellidoPaterno).trim(),
  materno: V.texto(b.apellidoMaterno).trim() || null,
  correo: V.texto(b.correo).trim().toLowerCase(),
});

// busca la especialidad o la crea
async function buscarEspecialidad(q, nombre) {
  const t = V.texto(nombre).trim();
  if (!t) return null;
  const filas = await q('SELECT id FROM especialidades WHERE nombre = @n', { n: t });
  if (filas.length > 0) return filas[0].id;
  return (await q('INSERT INTO especialidades (nombre) OUTPUT INSERTED.id VALUES (@n)', { n: t }))[0].id;
}

// regresa la clave solo si se genero
async function guardarAlumno(b, id) {
  const matricula = V.normalizarMatricula(b.matricula);
  revisar(V.revisarPersona(b.nombres, b.apellidoPaterno, b.apellidoMaterno, b.correo));
  if (!V.matriculaRe.test(matricula)) throw new ErrorApi(V.MENSAJE_MATRICULA);
  revisar(V.revisarCorreoAlumno(b.correo, matricula));
  const semestre = Number(b.semestre);
  if (!Number.isInteger(semestre) || semestre < 1 || semestre > V.L.SEMESTRE_MAX) {
    throw new ErrorApi('El semestre debe estar entre 1 y 8.');
  }
  const idCarrera = Number(b.idCarrera);
  const contrasena = V.texto(b.contrasena);
  if (id === null) revisar(V.revisarClave(contrasena, true));

  return transaccion(async (q) => {
    if ((await q('SELECT 1 AS x FROM carreras WHERE id = @id', { id: idCarrera })).length === 0) {
      throw new ErrorApi('Selecciona una carrera válida.');
    }
    const u = datosUsuario(b);
    if (id === null) {
      const clave = contrasena || claveInicial();
      const nuevo = await q(
        `INSERT INTO usuarios (nombres, apellido_paterno, apellido_materno, correo, contrasena_hash, rol)
         OUTPUT INSERTED.id VALUES (@nombres, @paterno, @materno, @correo, @hash, 'alumno')`,
        { ...u, hash: generarHash(clave) },
      );
      await q(
        'INSERT INTO alumnos (id_usuario, matricula, id_carrera, semestre) VALUES (@id, @matricula, @carrera, @semestre)',
        { id: nuevo[0].id, matricula, carrera: idCarrera, semestre },
      );
      return contrasena ? null : clave;
    }
    const filas = await q(
      `UPDATE usuarios SET nombres = @nombres, apellido_paterno = @paterno, apellido_materno = @materno, correo = @correo
        WHERE id = @id AND rol = 'alumno' AND activo = 1`,
      { ...u, id },
    );
    if (filas.afectadas === 0) throw new ErrorApi('Alumno no encontrado.', 404);
    await q('UPDATE alumnos SET matricula = @matricula, id_carrera = @carrera, semestre = @semestre WHERE id_usuario = @id',
      { id, matricula, carrera: idCarrera, semestre });
    return null;
  });
}

async function guardarDocente(b, id) {
  revisar(V.revisarPersona(b.nombres, b.apellidoPaterno, b.apellidoMaterno, b.correo));
  revisar(V.revisarEspecialidad(b.especialidad));
  revisar(V.revisarTelefono(b.telefono));
  const contrasena = V.texto(b.contrasena);
  if (id === null) revisar(V.revisarClave(contrasena, true));
  const telefono = V.texto(b.telefono).trim() || null;

  return transaccion(async (q) => {
    const u = datosUsuario(b);
    const idEspecialidad = await buscarEspecialidad(q, b.especialidad);
    if (id === null) {
      const clave = contrasena || claveInicial();
      const nuevo = await q(
        `INSERT INTO usuarios (nombres, apellido_paterno, apellido_materno, correo, contrasena_hash, rol)
         OUTPUT INSERTED.id VALUES (@nombres, @paterno, @materno, @correo, @hash, 'docente')`,
        { ...u, hash: generarHash(clave) },
      );
      await q('INSERT INTO docentes (id_usuario, id_especialidad, telefono) VALUES (@id, @esp, @tel)',
        { id: nuevo[0].id, esp: idEspecialidad, tel: telefono });
      return contrasena ? null : clave;
    }
    const filas = await q(
      `UPDATE usuarios SET nombres = @nombres, apellido_paterno = @paterno, apellido_materno = @materno, correo = @correo
        WHERE id = @id AND rol = 'docente' AND activo = 1`,
      { ...u, id },
    );
    if (filas.afectadas === 0) throw new ErrorApi('Docente no encontrado.', 404);
    await q('UPDATE docentes SET id_especialidad = @esp, telefono = @tel WHERE id_usuario = @id',
      { id, esp: idEspecialidad, tel: telefono });
    return null;
  });
}

// baja logica para conservar historial
async function darDeBaja(id, rol) {
  const filas = await consulta('UPDATE usuarios SET activo = 0 WHERE id = @id AND rol = @rol AND activo = 1', { id, rol });
  if (filas.afectadas === 0) throw new ErrorApi('No encontrado.', 404);
}

router.post('/alumnos', ruta(async (req, res) => {
  res.status(201).json({ clave: await guardarAlumno(req.body, null) });
}));
router.put('/alumnos/:id(\\d+)', ruta(async (req, res) => {
  res.json({ clave: await guardarAlumno(req.body, idDe(req.params.id)) });
}));
router.delete('/alumnos/:id(\\d+)', ruta(async (req, res) => {
  await darDeBaja(idDe(req.params.id), 'alumno');
  res.json({ ok: true });
}));

router.post('/docentes', ruta(async (req, res) => {
  res.status(201).json({ clave: await guardarDocente(req.body, null) });
}));
router.put('/docentes/:id(\\d+)', ruta(async (req, res) => {
  res.json({ clave: await guardarDocente(req.body, idDe(req.params.id)) });
}));
router.delete('/docentes/:id(\\d+)', ruta(async (req, res) => {
  await darDeBaja(idDe(req.params.id), 'docente');
  res.json({ ok: true });
}));

module.exports = router;
