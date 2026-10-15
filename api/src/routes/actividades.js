const router = require('express').Router();
const db = require('../db');
const { wrap, HttpError, auth, requireFields } = require('../util');

const DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

function validarHorarios(horarios) {
  if (!Array.isArray(horarios) || !horarios.length) throw new HttpError(400, 'Indica al menos un horario');
  for (const h of horarios) {
    if (!DIAS.includes(h.dia_semana) || !h.hora_inicio || !h.hora_fin || h.hora_fin <= h.hora_inicio) {
      throw new HttpError(400, 'Horario inválido (día, hora_inicio y hora_fin)');
    }
  }
}

async function guardarHorarios(conn, idActividad, horarios) {
  await conn.query('DELETE FROM horarios WHERE id_actividad = ?', [idActividad]);
  for (const h of horarios) {
    await conn.query(
      'INSERT INTO horarios (id_actividad, dia_semana, hora_inicio, hora_fin, lugar) VALUES (?,?,?,?,?)',
      [idActividad, h.dia_semana, h.hora_inicio, h.hora_fin, h.lugar || 'ITSM']);
  }
}

// GET /api/actividades  — catálogo con cupo y horario; el alumno recibe además si ya está inscrito
router.get('/actividades', auth(), wrap(async (req, res) => {
  const [rows] = await db.query(
    `SELECT v.*, EXISTS(SELECT 1 FROM inscripciones i
                         WHERE i.id_actividad = v.id AND i.id_alumno = ? AND i.estado = 'activa') AS inscrito
       FROM v_actividades v WHERE v.activa = 1 ORDER BY v.nombre`, [req.user.id]);
  res.json(rows.map((r) => ({ ...r, inscrito: !!r.inscrito })));
}));

router.get('/actividades/:id', auth(), wrap(async (req, res) => {
  const [[act]] = await db.query('SELECT * FROM v_actividades WHERE id = ?', [req.params.id]);
  if (!act) throw new HttpError(404, 'Actividad no encontrada');
  const [horarios] = await db.query(
    'SELECT id, dia_semana, hora_inicio, hora_fin, lugar FROM horarios WHERE id_actividad = ?', [req.params.id]);
  res.json({ ...act, horarios });
}));

// POST /api/actividades  { nombre, descripcion?, cupo, id_docente, horarios:[{dia_semana,hora_inicio,hora_fin}] }
router.post('/actividades', auth('admin'), wrap(async (req, res) => {
  requireFields(req.body, ['nombre', 'cupo']);
  validarHorarios(req.body.horarios);
  const { nombre, descripcion, cupo, id_docente, horarios } = req.body;
  const [[periodo]] = await db.query('SELECT id FROM periodos WHERE activo = 1 ORDER BY inicio DESC LIMIT 1');
  if (!periodo) throw new HttpError(409, 'No hay un periodo activo');

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [r] = await conn.query(
      'INSERT INTO actividades (nombre, descripcion, cupo, id_docente, id_periodo) VALUES (?,?,?,?,?)',
      [nombre, descripcion ?? null, cupo, id_docente ?? null, periodo.id]);
    await guardarHorarios(conn, r.insertId, horarios);
    await conn.commit();
    res.status(201).json({ id: r.insertId });
  } catch (e) {
    await conn.rollback();
    throw e;
  } finally {
    conn.release();
  }
}));

router.put('/actividades/:id', auth('admin'), wrap(async (req, res) => {
  const { nombre, descripcion, cupo, id_docente, horarios } = req.body;
  if (horarios !== undefined) validarHorarios(horarios);

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [r] = await conn.query(
      `UPDATE actividades SET nombre = COALESCE(?, nombre), descripcion = COALESCE(?, descripcion),
              cupo = COALESCE(?, cupo), id_docente = COALESCE(?, id_docente) WHERE id = ?`,
      [nombre ?? null, descripcion ?? null, cupo ?? null, id_docente ?? null, req.params.id]);
    if (!r.affectedRows) throw new HttpError(404, 'Actividad no encontrada');
    if (horarios !== undefined) await guardarHorarios(conn, req.params.id, horarios);
    await conn.commit();
    res.json({ ok: true });
  } catch (e) {
    await conn.rollback();
    throw e;
  } finally {
    conn.release();
  }
}));

// Baja lógica: no se pierden asistencias ni constancias ya emitidas
router.delete('/actividades/:id', auth('admin'), wrap(async (req, res) => {
  const [r] = await db.query('UPDATE actividades SET activa = 0 WHERE id = ?', [req.params.id]);
  if (!r.affectedRows) throw new HttpError(404, 'Actividad no encontrada');
  res.json({ ok: true });
}));

// ---------- Docente ----------
// GET /api/mis-actividades  — "Mis clases"
router.get('/mis-actividades', auth('docente'), wrap(async (req, res) => {
  const [rows] = await db.query(
    'SELECT * FROM v_actividades WHERE id_docente = ? AND activa = 1 ORDER BY nombre', [req.user.id]);
  res.json(rows);
}));

// GET /api/actividades/:id/alumnos  — alumnos inscritos (docente titular o admin)
router.get('/actividades/:id/alumnos', auth('docente', 'admin'), wrap(async (req, res) => {
  if (req.user.rol === 'docente') {
    const [[own]] = await db.query('SELECT 1 AS ok FROM actividades WHERE id = ? AND id_docente = ?',
      [req.params.id, req.user.id]);
    if (!own) throw new HttpError(403, 'No eres el docente de esta actividad');
  }
  const [rows] = await db.query(
    `SELECT u.id, u.nombre, a.matricula FROM inscripciones i
       JOIN alumnos a ON a.id_usuario = i.id_alumno JOIN usuarios u ON u.id = a.id_usuario
      WHERE i.id_actividad = ? AND i.estado = 'activa' ORDER BY u.nombre`, [req.params.id]);
  res.json(rows);
}));

// ---------- Alumno ----------
// POST /api/inscripciones  { id_actividad }
router.post('/inscripciones', auth('alumno'), wrap(async (req, res) => {
  requireFields(req.body, ['id_actividad']);
  const [[act]] = await db.query('SELECT id FROM actividades WHERE id = ? AND activa = 1', [req.body.id_actividad]);
  if (!act) throw new HttpError(404, 'Actividad no disponible');
  // Si había una baja previa, se reactiva (el trigger solo corre en INSERT, así que se valida el cupo aquí)
  const [[prev]] = await db.query(
    'SELECT id, estado FROM inscripciones WHERE id_alumno = ? AND id_actividad = ?', [req.user.id, act.id]);
  if (prev && prev.estado === 'activa') throw new HttpError(409, 'Ya estás inscrito en esta actividad');
  if (prev) {
    const [[v]] = await db.query('SELECT cupo, inscritos FROM v_actividades WHERE id = ?', [act.id]);
    if (v.inscritos >= v.cupo) throw new HttpError(409, 'Cupo lleno para esta actividad');
    await db.query("UPDATE inscripciones SET estado = 'activa', fecha = NOW() WHERE id = ?", [prev.id]);
    return res.status(201).json({ id: prev.id });
  }
  const [r] = await db.query('INSERT INTO inscripciones (id_alumno, id_actividad) VALUES (?,?)',
    [req.user.id, act.id]);
  res.status(201).json({ id: r.insertId });
}));

router.delete('/inscripciones/:idActividad', auth('alumno'), wrap(async (req, res) => {
  const [r] = await db.query(
    "UPDATE inscripciones SET estado = 'baja' WHERE id_alumno = ? AND id_actividad = ? AND estado = 'activa'",
    [req.user.id, req.params.idActividad]);
  if (!r.affectedRows) throw new HttpError(404, 'No estás inscrito en esa actividad');
  res.json({ ok: true });
}));

// GET /api/mi-horario  — actividades inscritas con sus horarios
router.get('/mi-horario', auth('alumno'), wrap(async (req, res) => {
  const [rows] = await db.query(
    `SELECT a.id AS id_actividad, a.nombre, h.dia_semana, h.hora_inicio, h.hora_fin, h.lugar
       FROM inscripciones i JOIN actividades a ON a.id = i.id_actividad
       JOIN horarios h ON h.id_actividad = a.id
      WHERE i.id_alumno = ? AND i.estado = 'activa' AND a.activa = 1
      ORDER BY FIELD(h.dia_semana,'Lunes','Martes','Miércoles','Jueves','Viernes','Sábado'), h.hora_inicio`,
    [req.user.id]);
  res.json(rows);
}));

module.exports = router;
