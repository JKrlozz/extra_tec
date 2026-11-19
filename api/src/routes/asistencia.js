// pase de lista
const express = require('express');
const { consulta, transaccion } = require('../db');
const { solo } = require('../auth');
const { exigirTitular } = require('../acceso');
const { ErrorApi, ruta, revisar, idDe, hoy, NOMBRE_COMPLETO } = require('../util');
const V = require('../validar');
const { exigirVigente } = require('../semestre');

const router = express.Router();
const permiso = solo('docente', 'admin');

// lista de la fecha elegida
// sin registro todos salen presentes
router.get('/actividades/:id(\\d+)/asistencia', permiso, ruta(async (req, res) => {
  const idActividad = idDe(req.params.id);
  await exigirTitular(req, idActividad);
  const dia = V.texto(req.query.fecha).trim() || hoy();
  if (!V.esFechaReal(dia)) throw new ErrorApi('La fecha debe tener el formato AAAA-MM-DD.');
  const filas = await consulta(
    `SELECT u.id AS idAlumno, ${NOMBRE_COMPLETO('u')} AS nombre, a.matricula,
            COALESCE(asi.presente, 1) AS presente
       FROM inscripciones i
       JOIN alumnos a ON a.id_usuario = i.id_alumno
       JOIN usuarios u ON u.id = a.id_usuario
       LEFT JOIN sesiones s ON s.id_actividad = i.id_actividad AND s.fecha = @fecha
       LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
      WHERE i.id_actividad = @actividad AND i.estado = 'activa'
      ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres`,
    { fecha: dia, actividad: idActividad },
  );
  res.json({
    fecha: dia,
    alumnos: filas.map((f) => ({ idAlumno: f.idAlumno, nombre: f.nombre, matricula: f.matricula, presente: f.presente === 1 })),
  });
}));

router.put('/actividades/:id(\\d+)/asistencia', permiso, ruta(async (req, res) => {
  const idActividad = idDe(req.params.id);
  const fecha = V.texto(req.body.fecha);
  const marcas = req.body.marcas;
  await transaccion(async (q) => {
    await exigirTitular(req, idActividad, q);
    await exigirVigente(q, idActividad);
    if (!Array.isArray(marcas) || marcas.length === 0) throw new ErrorApi('La lista de asistencia está vacía.');
    revisar(V.revisarFecha(fecha));

    // solo alumnos inscritos en la actividad
    const inscritos = new Set(
      (await q('SELECT id_alumno FROM inscripciones WHERE id_actividad = @id', { id: idActividad })).map((f) => f.id_alumno),
    );
    const limpias = new Map();
    for (const m of marcas) {
      const idAlumno = Number(m.idAlumno);
      if (!inscritos.has(idAlumno)) throw new ErrorApi('El alumno no está inscrito en la actividad.');
      limpias.set(idAlumno, m.presente ? 1 : 0);
    }

    // crea la sesion si no existe
    await q(
      `IF NOT EXISTS (SELECT 1 FROM sesiones WITH (UPDLOCK, HOLDLOCK) WHERE id_actividad = @actividad AND fecha = @fecha)
         INSERT INTO sesiones (id_actividad, fecha) VALUES (@actividad, @fecha)`,
      { actividad: idActividad, fecha },
    );
    const sesion = await q('SELECT id FROM sesiones WHERE id_actividad = @actividad AND fecha = @fecha',
      { actividad: idActividad, fecha });
    if (sesion.length === 0) throw new ErrorApi('No se pudo abrir la sesión.');

    const json = JSON.stringify([...limpias].map(([idAlumno, presente]) => ({ idAlumno, presente })));
    const columnas = "WITH (idAlumno INT '$.idAlumno', presente INT '$.presente')";
    await q(
      `UPDATE x SET presente = m.presente
         FROM asistencias x JOIN OPENJSON(@marcas) ${columnas} m ON x.id_alumno = m.idAlumno
        WHERE x.id_sesion = @sesion`,
      { marcas: json, sesion: sesion[0].id },
    );
    await q(
      `INSERT INTO asistencias (id_sesion, id_alumno, presente)
       SELECT @sesion, m.idAlumno, m.presente FROM OPENJSON(@marcas) ${columnas} m
        WHERE NOT EXISTS (SELECT 1 FROM asistencias x WHERE x.id_sesion = @sesion AND x.id_alumno = m.idAlumno)`,
      { marcas: json, sesion: sesion[0].id },
    );
  });
  res.json({ ok: true });
}));

router.get('/actividades/:id(\\d+)/asistencia/historial', permiso, ruta(async (req, res) => {
  const idActividad = idDe(req.params.id);
  await exigirTitular(req, idActividad);
  const sesiones = await consulta(
    "SELECT id, CONVERT(VARCHAR(10), fecha, 23) AS fecha FROM sesiones WHERE id_actividad = @id ORDER BY sesiones.fecha DESC",
    { id: idActividad },
  );
  const detalle = await consulta(
    `SELECT asi.id_sesion AS idSesion, ${NOMBRE_COMPLETO('u')} AS nombre, a.matricula, asi.presente
       FROM asistencias asi
       JOIN sesiones s ON s.id = asi.id_sesion
       JOIN alumnos a ON a.id_usuario = asi.id_alumno
       JOIN usuarios u ON u.id = a.id_usuario
      WHERE s.id_actividad = @id
      ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres`,
    { id: idActividad },
  );
  res.json(sesiones.map((s) => {
    const alumnos = detalle
      .filter((d) => d.idSesion === s.id)
      .map((d) => ({ nombre: d.nombre, matricula: d.matricula, presente: d.presente === 1 }));
    return {
      id: s.id, fecha: s.fecha, presentes: alumnos.filter((a) => a.presente).length,
      total: alumnos.length, alumnos,
    };
  }));
}));

module.exports = router;
