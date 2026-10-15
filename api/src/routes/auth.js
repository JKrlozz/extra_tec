const router = require('express').Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../db');
const { wrap, HttpError, requireFields } = require('../util');

const firmar = (u) =>
  jwt.sign({ id: u.id, rol: u.rol, nombre: u.nombre }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRA || '30d',
  });

// POST /api/login  { correo, contrasena, rol }
router.post('/login', wrap(async (req, res) => {
  requireFields(req.body, ['correo', 'contrasena', 'rol']);
  const { correo, contrasena, rol } = req.body;
  const [[u]] = await db.query('SELECT * FROM usuarios WHERE correo = ? AND activo = 1', [correo]);
  // Mismo mensaje en todos los fallos para no revelar qué cuentas existen
  if (!u || u.rol !== rol || !(await bcrypt.compare(contrasena, u.contrasena_hash))) {
    throw new HttpError(401, 'Correo, contraseña o tipo de usuario incorrectos');
  }
  res.json({ token: firmar(u), usuario: { id: u.id, nombre: u.nombre, correo: u.correo, rol: u.rol } });
}));

// POST /api/registro  (solo alumnos)  { nombre, correo, contrasena, matricula, id_carrera, semestre }
router.post('/registro', wrap(async (req, res) => {
  requireFields(req.body, ['nombre', 'correo', 'contrasena', 'matricula', 'id_carrera', 'semestre']);
  const { nombre, correo, contrasena, matricula, id_carrera, semestre } = req.body;
  if (String(contrasena).length < 8) throw new HttpError(400, 'La contraseña debe tener al menos 8 caracteres');
  const hash = await bcrypt.hash(contrasena, 10);

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [r] = await conn.query(
      "INSERT INTO usuarios (nombre, correo, contrasena_hash, rol) VALUES (?,?,?,'alumno')",
      [nombre, correo, hash]);
    await conn.query(
      'INSERT INTO alumnos (id_usuario, matricula, id_carrera, semestre) VALUES (?,?,?,?)',
      [r.insertId, matricula, id_carrera, semestre]);
    await conn.commit();
    res.status(201).json({ id: r.insertId });
  } catch (e) {
    await conn.rollback();
    throw e;
  } finally {
    conn.release();
  }
}));

// GET /api/carreras  (público: lo necesita la pantalla de registro)
router.get('/carreras', wrap(async (req, res) => {
  const [rows] = await db.query('SELECT id, nombre FROM carreras ORDER BY nombre');
  res.json(rows);
}));

module.exports = router;
