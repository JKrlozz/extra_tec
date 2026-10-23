const router = require('express').Router();
const db = require('../db');
const { wrap, HttpError, auth } = require('../util');

// GET /api/reportes?tipo=actividad|alumno|docente&desde=YYYY-MM-DD&hasta=YYYY-MM-DD
// El rango filtra por fecha de sesión; sin rango se usa todo el historial.
router.get('/reportes', auth('admin'), wrap(async (req, res) => {
  const { tipo = 'actividad' } = req.query;
  const desde = req.query.desde || '1000-01-01';
  const hasta = req.query.hasta || '9999-12-31';
  if (desde > hasta) throw new HttpError(400, 'La fecha inicial es posterior a la final');

  const consultas = {
    actividad: `
      SELECT a.id, a.nombre AS actividad, d.nombre AS docente, a.cupo,
             (SELECT COUNT(*) FROM inscripciones i WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos,
             COUNT(DISTINCT s.id) AS sesiones,
             ROUND(100 * COALESCE(SUM(asi.presente), 0) / NULLIF(COUNT(asi.id), 0), 1) AS asistencia_pct
        FROM actividades a
        LEFT JOIN usuarios d ON d.id = a.id_docente
        LEFT JOIN sesiones s ON s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?
        LEFT JOIN asistencias asi ON asi.id_sesion = s.id
       WHERE a.activa = 1 GROUP BY a.id, d.nombre ORDER BY a.nombre`,
    alumno: `
      SELECT u.id, u.nombre AS alumno, al.matricula,
             COUNT(DISTINCT i.id_actividad) AS actividades,
             ROUND(100 * COALESCE(SUM(asi.presente), 0) / NULLIF(COUNT(asi.id), 0), 1) AS asistencia_pct
        FROM alumnos al JOIN usuarios u ON u.id = al.id_usuario
        LEFT JOIN inscripciones i ON i.id_alumno = al.id_usuario AND i.estado = 'activa'
        LEFT JOIN sesiones s ON s.id_actividad = i.id_actividad AND s.fecha BETWEEN ? AND ?
        LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = al.id_usuario
       WHERE u.activo = 1 GROUP BY u.id, al.matricula ORDER BY u.nombre`,
    docente: `
      SELECT u.id, u.nombre AS docente, COUNT(DISTINCT a.id) AS actividades,
             COUNT(DISTINCT s.id) AS sesiones_impartidas,
             (SELECT COUNT(*) FROM evidencias e WHERE e.id_docente = u.id
                 AND DATE(e.subida_en) BETWEEN ? AND ?) AS evidencias
        FROM docentes d JOIN usuarios u ON u.id = d.id_usuario
        LEFT JOIN actividades a ON a.id_docente = d.id_usuario AND a.activa = 1
        LEFT JOIN sesiones s ON s.id_actividad = a.id AND s.fecha BETWEEN ? AND ?
       WHERE u.activo = 1 GROUP BY u.id ORDER BY u.nombre`,
  };
  if (!consultas[tipo]) throw new HttpError(400, 'tipo debe ser actividad, alumno o docente');

  const params = tipo === 'docente' ? [desde, hasta, desde, hasta] : [desde, hasta];
  const [filas] = await db.query(consultas[tipo], params);
  res.json({ tipo, desde: req.query.desde || null, hasta: req.query.hasta || null, total: filas.length, filas });
}));

module.exports = router;
