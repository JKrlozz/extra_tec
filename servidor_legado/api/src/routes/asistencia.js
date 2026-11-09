const router = require('express').Router();
const path = require('path');
const crypto = require('crypto');
const multer = require('multer');
const db = require('../db');
const { wrap, HttpError, auth, requireFields } = require('../util');

async function exigirTitular(user, idActividad) {
  const [[a]] = await db.query('SELECT id_docente FROM actividades WHERE id = ? AND activa = 1', [idActividad]);
  if (!a) throw new HttpError(404, 'Actividad no encontrada');
  if (user.rol === 'docente' && a.id_docente !== user.id) {
    throw new HttpError(403, 'No eres el docente de esta actividad');
  }
}

const hoy = () => new Date().toISOString().slice(0, 10);

router.get('/asistencia/:idActividad', auth('docente', 'admin'), wrap(async (req, res) => {
  await exigirTitular(req.user, req.params.idActividad);
  const fecha = req.query.fecha || hoy();
  const [rows] = await db.query(
    `SELECT u.id, u.nombre_completo AS nombre, a.matricula, asi.presente
       FROM inscripciones i
       JOIN alumnos a ON a.id_usuario = i.id_alumno
       JOIN usuarios u ON u.id = a.id_usuario
       LEFT JOIN sesiones s ON s.id_actividad = i.id_actividad AND s.fecha = ?
       LEFT JOIN asistencias asi ON asi.id_sesion = s.id AND asi.id_alumno = i.id_alumno
      WHERE i.id_actividad = ? AND i.estado = 'activa' ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres`, [fecha, req.params.idActividad]);
  res.json({ fecha, alumnos: rows.map((r) => ({ ...r, presente: r.presente === null ? true : !!r.presente })) });
}));

router.post('/asistencia', auth('docente', 'admin'), wrap(async (req, res) => {
  requireFields(req.body, ['id_actividad', 'asistencias']);
  const { id_actividad, asistencias } = req.body;
  const fecha = req.body.fecha || hoy();
  if (!Array.isArray(asistencias) || !asistencias.length) throw new HttpError(400, 'Lista vacía');
  await exigirTitular(req.user, id_actividad);

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    await conn.query('INSERT IGNORE INTO sesiones (id_actividad, fecha) VALUES (?,?)', [id_actividad, fecha]);
    const [[s]] = await conn.query('SELECT id FROM sesiones WHERE id_actividad = ? AND fecha = ?', [id_actividad, fecha]);
    for (const a of asistencias) {
      await conn.query(
        `INSERT INTO asistencias (id_sesion, id_alumno, presente)
         SELECT ?, i.id_alumno, ? FROM inscripciones i
          WHERE i.id_actividad = ? AND i.id_alumno = ? AND i.estado = 'activa'
         ON DUPLICATE KEY UPDATE presente = VALUES(presente)`,
        [s.id, a.presente ? 1 : 0, id_actividad, a.id_alumno]);
    }
    await conn.commit();
    res.json({ ok: true, fecha });
  } catch (e) {
    await conn.rollback();
    throw e;
  } finally {
    conn.release();
  }
}));

const subir = multer({
  storage: multer.diskStorage({
    destination: path.join(__dirname, '..', '..', 'uploads'),
    filename: (req, file, cb) => cb(null, crypto.randomUUID() + path.extname(file.originalname).toLowerCase()),
  }),
  limits: { fileSize: 8 * 1024 * 1024 },
  fileFilter: (req, file, cb) =>
    cb(/^image\/(jpeg|png|webp)$/.test(file.mimetype) ? null : new HttpError(400, 'Solo imágenes JPG, PNG o WebP'),
      /^image\/(jpeg|png|webp)$/.test(file.mimetype)),
});

router.post('/evidencias', auth('docente'), subir.single('foto'), wrap(async (req, res) => {
  requireFields(req.body, ['id_actividad']);
  if (!req.file) throw new HttpError(400, 'Falta la fotografía');
  await exigirTitular(req.user, req.body.id_actividad);
  const [r] = await db.query(
    'INSERT INTO evidencias (id_actividad, id_docente, descripcion, ruta_foto) VALUES (?,?,?,?)',
    [req.body.id_actividad, req.user.id, req.body.descripcion ?? null, '/uploads/' + req.file.filename]);
  res.status(201).json({ id: r.insertId, ruta_foto: '/uploads/' + req.file.filename });
}));

router.get('/evidencias/:idActividad', auth('docente', 'admin'), wrap(async (req, res) => {
  await exigirTitular(req.user, req.params.idActividad);
  const [rows] = await db.query(
    'SELECT id, descripcion, ruta_foto, subida_en FROM evidencias WHERE id_actividad = ? ORDER BY subida_en DESC',
    [req.params.idActividad]);
  res.json(rows);
}));

module.exports = router;
