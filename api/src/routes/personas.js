// CRUD de alumnos y docentes (solo administrador)
const router = require('express').Router();
const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const db = require('../db');
const { wrap, HttpError, auth, requireFields } = require('../util');

const admin = auth('admin');

// Contraseña inicial aleatoria si el admin no manda una; se devuelve una sola vez
async function claveInicial(body) {
  const plana = body.contrasena || crypto.randomBytes(5).toString('hex');
  return { plana, hash: await bcrypt.hash(plana, 10), generada: !body.contrasena };
}

async function enTransaccion(fn) {
  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const out = await fn(conn);
    await conn.commit();
    return out;
  } catch (e) {
    await conn.rollback();
    throw e;
  } finally {
    conn.release();
  }
}

// ---------- Alumnos ----------
router.get('/alumnos', admin, wrap(async (req, res) => {
  const q = `%${req.query.q || ''}%`;
  const [rows] = await db.query(
    `SELECT u.id, u.nombre, u.correo, a.matricula, a.semestre, c.id AS id_carrera, c.nombre AS carrera
       FROM alumnos a JOIN usuarios u ON u.id = a.id_usuario JOIN carreras c ON c.id = a.id_carrera
      WHERE u.activo = 1 AND (u.nombre LIKE ? OR a.matricula LIKE ?)
      ORDER BY u.nombre`, [q, q]);
  res.json(rows);
}));

router.post('/alumnos', admin, wrap(async (req, res) => {
  requireFields(req.body, ['nombre', 'correo', 'matricula', 'id_carrera', 'semestre']);
  const { nombre, correo, matricula, id_carrera, semestre } = req.body;
  const clave = await claveInicial(req.body);
  const id = await enTransaccion(async (conn) => {
    const [r] = await conn.query(
      "INSERT INTO usuarios (nombre, correo, contrasena_hash, rol) VALUES (?,?,?,'alumno')",
      [nombre, correo, clave.hash]);
    await conn.query(
      'INSERT INTO alumnos (id_usuario, matricula, id_carrera, semestre) VALUES (?,?,?,?)',
      [r.insertId, matricula, id_carrera, semestre]);
    return r.insertId;
  });
  res.status(201).json({ id, contrasena_inicial: clave.generada ? clave.plana : undefined });
}));

router.put('/alumnos/:id', admin, wrap(async (req, res) => {
  const { nombre, correo, matricula, id_carrera, semestre } = req.body;
  const found = await enTransaccion(async (conn) => {
    const [u] = await conn.query(
      "UPDATE usuarios SET nombre = COALESCE(?, nombre), correo = COALESCE(?, correo) WHERE id = ? AND rol = 'alumno'",
      [nombre ?? null, correo ?? null, req.params.id]);
    if (!u.affectedRows) return false;
    await conn.query(
      `UPDATE alumnos SET matricula = COALESCE(?, matricula), id_carrera = COALESCE(?, id_carrera),
              semestre = COALESCE(?, semestre) WHERE id_usuario = ?`,
      [matricula ?? null, id_carrera ?? null, semestre ?? null, req.params.id]);
    return true;
  });
  if (!found) throw new HttpError(404, 'Alumno no encontrado');
  res.json({ ok: true });
}));

// ---------- Docentes ----------
router.get('/docentes', auth('admin', 'alumno', 'docente'), wrap(async (req, res) => {
  const [rows] = await db.query(
    `SELECT u.id, u.nombre, u.correo, d.especialidad, d.telefono
       FROM docentes d JOIN usuarios u ON u.id = d.id_usuario
      WHERE u.activo = 1 ORDER BY u.nombre`);
  res.json(rows);
}));

router.post('/docentes', admin, wrap(async (req, res) => {
  requireFields(req.body, ['nombre', 'correo']);
  const { nombre, correo, especialidad, telefono } = req.body;
  const clave = await claveInicial(req.body);
  const id = await enTransaccion(async (conn) => {
    const [r] = await conn.query(
      "INSERT INTO usuarios (nombre, correo, contrasena_hash, rol) VALUES (?,?,?,'docente')",
      [nombre, correo, clave.hash]);
    await conn.query('INSERT INTO docentes (id_usuario, especialidad, telefono) VALUES (?,?,?)',
      [r.insertId, especialidad ?? null, telefono ?? null]);
    return r.insertId;
  });
  res.status(201).json({ id, contrasena_inicial: clave.generada ? clave.plana : undefined });
}));

router.put('/docentes/:id', admin, wrap(async (req, res) => {
  const { nombre, correo, especialidad, telefono } = req.body;
  const found = await enTransaccion(async (conn) => {
    const [u] = await conn.query(
      "UPDATE usuarios SET nombre = COALESCE(?, nombre), correo = COALESCE(?, correo) WHERE id = ? AND rol = 'docente'",
      [nombre ?? null, correo ?? null, req.params.id]);
    if (!u.affectedRows) return false;
    await conn.query(
      'UPDATE docentes SET especialidad = COALESCE(?, especialidad), telefono = COALESCE(?, telefono) WHERE id_usuario = ?',
      [especialidad ?? null, telefono ?? null, req.params.id]);
    return true;
  });
  if (!found) throw new HttpError(404, 'Docente no encontrado');
  res.json({ ok: true });
}));

// Baja lógica compartida: conserva historial de asistencias y constancias
router.delete('/alumnos/:id', admin, wrap(bajaUsuario('alumno')));
router.delete('/docentes/:id', admin, wrap(bajaUsuario('docente')));

function bajaUsuario(rol) {
  return async (req, res) => {
    const [r] = await db.query('UPDATE usuarios SET activo = 0 WHERE id = ? AND rol = ?', [req.params.id, rol]);
    if (!r.affectedRows) throw new HttpError(404, 'No encontrado');
    res.json({ ok: true });
  };
}

module.exports = router;
