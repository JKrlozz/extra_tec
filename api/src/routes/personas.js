const router = require('express').Router();
const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const db = require('../db');
const { wrap, HttpError, auth, requireFields } = require('../util');

const admin = auth('admin');

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

// Arma un UPDATE parcial solo con los campos de nombre que llegaron.
function camposNombre(body) {
  const campos = [];
  const valores = [];
  if (body.nombres !== undefined) { campos.push('nombres = ?'); valores.push(body.nombres); }
  if (body.apellido_paterno !== undefined) {
    campos.push('apellido_paterno = ?'); valores.push(body.apellido_paterno);
  }
  if (body.apellido_materno !== undefined) {
    campos.push('apellido_materno = ?'); valores.push(body.apellido_materno || null);
  }
  if (body.correo !== undefined) { campos.push('correo = ?'); valores.push(body.correo); }
  return { campos, valores };
}

const CAMPOS_ALUMNO = `u.id, u.nombres, u.apellido_paterno, u.apellido_materno,
       u.nombre_completo AS nombre, u.correo, a.matricula, a.semestre,
       c.id AS id_carrera, c.nombre AS carrera`;

router.get('/alumnos', admin, wrap(async (req, res) => {
  const q = `%${req.query.q || ''}%`;
  const [rows] = await db.query(
    `SELECT ${CAMPOS_ALUMNO}
       FROM alumnos a JOIN usuarios u ON u.id = a.id_usuario JOIN carreras c ON c.id = a.id_carrera
      WHERE u.activo = 1 AND (u.nombre_completo LIKE ? OR a.matricula LIKE ?)
      ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres`, [q, q]);
  res.json(rows);
}));

router.post('/alumnos', admin, wrap(async (req, res) => {
  requireFields(req.body, ['nombres', 'apellido_paterno', 'correo', 'matricula', 'id_carrera', 'semestre']);
  const { nombres, apellido_paterno, apellido_materno, correo, matricula, id_carrera, semestre } = req.body;
  const clave = await claveInicial(req.body);
  const id = await enTransaccion(async (conn) => {
    const [r] = await conn.query(
      "INSERT INTO usuarios (nombres, apellido_paterno, apellido_materno, correo, contrasena_hash, rol) VALUES (?,?,?,?,?,'alumno')",
      [nombres, apellido_paterno, apellido_materno || null, correo, clave.hash]);
    await conn.query(
      'INSERT INTO alumnos (id_usuario, matricula, id_carrera, semestre) VALUES (?,?,?,?)',
      [r.insertId, matricula, id_carrera, semestre]);
    return r.insertId;
  });
  res.status(201).json({ id, contrasena_inicial: clave.generada ? clave.plana : undefined });
}));

router.put('/alumnos/:id', admin, wrap(async (req, res) => {
  const { matricula, id_carrera, semestre } = req.body;
  const found = await enTransaccion(async (conn) => {
    const [[existe]] = await conn.query("SELECT 1 AS ok FROM usuarios WHERE id = ? AND rol = 'alumno'", [req.params.id]);
    if (!existe) return false;
    const { campos, valores } = camposNombre(req.body);
    if (campos.length) {
      await conn.query(`UPDATE usuarios SET ${campos.join(', ')} WHERE id = ?`, [...valores, req.params.id]);
    }
    await conn.query(
      `UPDATE alumnos SET matricula = COALESCE(?, matricula), id_carrera = COALESCE(?, id_carrera),
              semestre = COALESCE(?, semestre) WHERE id_usuario = ?`,
      [matricula ?? null, id_carrera ?? null, semestre ?? null, req.params.id]);
    return true;
  });
  if (!found) throw new HttpError(404, 'Alumno no encontrado');
  res.json({ ok: true });
}));

router.get('/docentes', auth('admin', 'alumno', 'docente'), wrap(async (req, res) => {
  const [rows] = await db.query(
    `SELECT u.id, u.nombres, u.apellido_paterno, u.apellido_materno,
            u.nombre_completo AS nombre, u.correo, d.especialidad, d.telefono
       FROM docentes d JOIN usuarios u ON u.id = d.id_usuario
      WHERE u.activo = 1 ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres`);
  res.json(rows);
}));

router.post('/docentes', admin, wrap(async (req, res) => {
  requireFields(req.body, ['nombres', 'apellido_paterno', 'correo']);
  const { nombres, apellido_paterno, apellido_materno, correo, especialidad, telefono } = req.body;
  const clave = await claveInicial(req.body);
  const id = await enTransaccion(async (conn) => {
    const [r] = await conn.query(
      "INSERT INTO usuarios (nombres, apellido_paterno, apellido_materno, correo, contrasena_hash, rol) VALUES (?,?,?,?,?,'docente')",
      [nombres, apellido_paterno, apellido_materno || null, correo, clave.hash]);
    await conn.query('INSERT INTO docentes (id_usuario, especialidad, telefono) VALUES (?,?,?)',
      [r.insertId, especialidad ?? null, telefono ?? null]);
    return r.insertId;
  });
  res.status(201).json({ id, contrasena_inicial: clave.generada ? clave.plana : undefined });
}));

router.put('/docentes/:id', admin, wrap(async (req, res) => {
  const { especialidad, telefono } = req.body;
  const found = await enTransaccion(async (conn) => {
    const [[existe]] = await conn.query("SELECT 1 AS ok FROM usuarios WHERE id = ? AND rol = 'docente'", [req.params.id]);
    if (!existe) return false;
    const { campos, valores } = camposNombre(req.body);
    if (campos.length) {
      await conn.query(`UPDATE usuarios SET ${campos.join(', ')} WHERE id = ?`, [...valores, req.params.id]);
    }
    await conn.query(
      'UPDATE docentes SET especialidad = COALESCE(?, especialidad), telefono = COALESCE(?, telefono) WHERE id_usuario = ?',
      [especialidad ?? null, telefono ?? null, req.params.id]);
    return true;
  });
  if (!found) throw new HttpError(404, 'Docente no encontrado');
  res.json({ ok: true });
}));

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
