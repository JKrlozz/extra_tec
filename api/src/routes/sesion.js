// inicio de sesion, registro y carreras
const express = require('express');
const { consulta, transaccion } = require('../db');
const { generarHash, verificarHash, firmar } = require('../auth');
const { ErrorApi, ruta, revisar } = require('../util');
const V = require('../validar');

const router = express.Router();

// hash de relleno para que el tiempo no delate si el correo existe
const HASH_FALSO = generarHash('relleno-no-valido');

router.post('/login', ruta(async (req, res) => {
  const correo = V.texto(req.body.correo).trim();
  const contrasena = V.texto(req.body.contrasena);
  const rol = V.texto(req.body.rol);
  const filas = await consulta(
    `SELECT id, nombres, apellido_paterno, apellido_materno, correo, rol, contrasena_hash
       FROM usuarios WHERE correo = @correo AND activo = 1`,
    { correo },
  );
  const u = filas[0];
  const clave = verificarHash(contrasena, u ? u.contrasena_hash : HASH_FALSO);
  if (!u || u.rol !== rol || !clave) {
    throw new ErrorApi('Correo, contraseña o tipo de usuario incorrectos.', 401);
  }
  res.json({
    token: firmar(u),
    usuario: {
      id: u.id,
      nombres: u.nombres,
      apellidoPaterno: u.apellido_paterno,
      apellidoMaterno: u.apellido_materno || '',
      correo: u.correo,
      rol: u.rol,
    },
  });
}));

router.post('/registro', ruta(async (req, res) => {
  const b = req.body;
  const matricula = V.normalizarMatricula(b.matricula);
  revisar(V.revisarPersona(b.nombres, b.apellidoPaterno, b.apellidoMaterno, b.correo));
  revisar(V.revisarClave(V.texto(b.contrasena), false));
  if (!V.matriculaRe.test(matricula)) throw new ErrorApi(V.MENSAJE_MATRICULA);
  revisar(V.revisarCorreoAlumno(b.correo, matricula));
  const semestre = Number(b.semestre);
  if (!Number.isInteger(semestre) || semestre < 1 || semestre > V.L.SEMESTRE_MAX) {
    throw new ErrorApi('El semestre debe estar entre 1 y 8.');
  }
  const idCarrera = Number(b.idCarrera);
  if (!Number.isInteger(idCarrera) || idCarrera < 1) throw new ErrorApi('Selecciona tu carrera.');

  await transaccion(async (q) => {
    const carrera = await q('SELECT id FROM carreras WHERE id = @id', { id: idCarrera });
    if (carrera.length === 0) throw new ErrorApi('Selecciona tu carrera.');
    const usuario = await q(
      `INSERT INTO usuarios (nombres, apellido_paterno, apellido_materno, correo, contrasena_hash, rol)
       OUTPUT INSERTED.id
       VALUES (@nombres, @paterno, @materno, @correo, @hash, 'alumno')`,
      {
        nombres: V.texto(b.nombres).trim(),
        paterno: V.texto(b.apellidoPaterno).trim(),
        materno: V.texto(b.apellidoMaterno).trim() || null,
        correo: V.texto(b.correo).trim().toLowerCase(),
        hash: generarHash(V.texto(b.contrasena)),
      },
    );
    await q(
      'INSERT INTO alumnos (id_usuario, matricula, id_carrera, semestre) VALUES (@id, @matricula, @carrera, @semestre)',
      { id: usuario[0].id, matricula, carrera: idCarrera, semestre },
    );
  });
  res.status(201).json({ ok: true });
}));

// carreras en orden de la escuela
router.get('/carreras', ruta(async (req, res) => {
  const filas = await consulta(
    `SELECT id, nombre FROM carreras
      ORDER BY CASE nombre
        WHEN N'Ingeniería Industrial' THEN 1
        WHEN N'Ingeniería en Sistemas Computacionales' THEN 2
        WHEN N'Ingeniería Bioquímica' THEN 3
        WHEN N'Ingeniería Electromecánica' THEN 4
        WHEN N'Ingeniería Civil' THEN 5
        WHEN N'Ingeniería en Tecnologías de la Información y Comunicaciones' THEN 6
        WHEN N'Ingeniería Ambiental' THEN 7
        WHEN N'Ingeniería en Gestión Empresarial' THEN 8
        WHEN N'Ingeniería Petrolera' THEN 9
        WHEN N'Licenciatura en Gastronomía' THEN 10
        ELSE 99 END, nombre`,
  );
  res.json(filas);
}));

module.exports = router;
