const router = require('express').Router();
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../db');
const { wrap, HttpError, requireFields } = require('../util');

const firmar = (u) =>
  jwt.sign({ id: u.id, rol: u.rol, nombre: u.nombre_completo }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRA || '30d',
  });

// La API siempre entrega el nombre completo y sus partes por separado.
const datosUsuario = (u) => ({
  id: u.id,
  nombres: u.nombres,
  apellido_paterno: u.apellido_paterno,
  apellido_materno: u.apellido_materno,
  nombre: u.nombre_completo,
  correo: u.correo,
  rol: u.rol,
});

router.post('/login', wrap(async (req, res) => {
  requireFields(req.body, ['correo', 'contrasena', 'rol']);
  const { correo, contrasena, rol } = req.body;
  const [[u]] = await db.query('SELECT * FROM usuarios WHERE correo = ? AND activo = 1', [correo]);
  if (!u || u.rol !== rol || !(await bcrypt.compare(contrasena, u.contrasena_hash))) {
    throw new HttpError(401, 'Correo, contraseña o tipo de usuario incorrectos');
  }
  res.json({ token: firmar(u), usuario: datosUsuario(u) });
}));

router.post('/registro', wrap(async (req, res) => {
  requireFields(req.body, ['nombres', 'apellido_paterno', 'correo', 'contrasena', 'matricula', 'id_carrera', 'semestre']);
  const { nombres, apellido_paterno, apellido_materno, correo, contrasena, matricula, id_carrera, semestre } = req.body;
  if (String(contrasena).length < 8) throw new HttpError(400, 'La contraseña debe tener al menos 8 caracteres');
  const hash = await bcrypt.hash(contrasena, 10);

  const conn = await db.getConnection();
  try {
    await conn.beginTransaction();
    const [r] = await conn.query(
      "INSERT INTO usuarios (nombres, apellido_paterno, apellido_materno, correo, contrasena_hash, rol) VALUES (?,?,?,?,?,'alumno')",
      [nombres, apellido_paterno, apellido_materno || null, correo, hash]);
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

router.get('/carreras', wrap(async (req, res) => {
  const [rows] = await db.query('SELECT id, nombre FROM carreras ORDER BY nombre');
  res.json(rows);
}));

module.exports = router;
