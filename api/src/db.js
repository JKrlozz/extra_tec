// conexion a Azure SQL
const sql = require('mssql');
const { ErrorApi } = require('./util');

let pool = null;
let conectando = null;

function configuracion() {
  const { DB_SERVER, DB_NAME, DB_USER, DB_PASSWORD } = process.env;
  if (!DB_SERVER || !DB_NAME || !DB_USER || !DB_PASSWORD) {
    throw new Error('Faltan DB_SERVER, DB_NAME, DB_USER o DB_PASSWORD en el entorno.');
  }
  return {
    server: DB_SERVER,
    database: DB_NAME,
    user: DB_USER,
    password: DB_PASSWORD,
    options: { encrypt: true, trustServerCertificate: false },
    connectionTimeout: 30000,
    requestTimeout: 60000,
    pool: { max: 10, min: 0, idleTimeoutMillis: 30000 },
  };
}

const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

// la base gratuita se pausa sola: el primer intento puede tardar
async function conectar() {
  let ultimo;
  for (let intento = 1; intento <= 4; intento++) {
    try {
      const nuevo = new sql.ConnectionPool(configuracion());
      nuevo.on('error', () => { pool = null; });
      await nuevo.connect();
      return nuevo;
    } catch (e) {
      ultimo = e;
      console.error(`Conexión a la base fallida (intento ${intento}):`, e.message);
      await esperar(5000);
    }
  }
  throw ultimo;
}

async function obtener() {
  if (pool && pool.connected) return pool;
  if (!conectando) {
    conectando = conectar()
      .then((p) => { pool = p; return p; })
      .finally(() => { conectando = null; });
  }
  return conectando;
}

function preparar(request, params) {
  for (const [nombre, v] of Object.entries(params || {})) {
    if (Buffer.isBuffer(v)) request.input(nombre, sql.VarBinary(sql.MAX), v);
    else if (typeof v === 'number') request.input(nombre, Number.isInteger(v) ? sql.Int : sql.Float, v);
    else if (typeof v === 'boolean') request.input(nombre, sql.TinyInt, v ? 1 : 0);
    else request.input(nombre, sql.NVarChar(sql.MAX), v == null ? null : String(v));
  }
}

async function correr(request, texto, params) {
  preparar(request, params);
  const r = await request.query(texto);
  const filas = r.recordset || [];
  filas.afectadas = (r.rowsAffected || []).reduce((a, b) => a + b, 0);
  return filas;
}

// consulta sola
async function consulta(texto, params) {
  const p = await obtener();
  return correr(p.request(), texto, params);
}

// todo o nada: fn recibe q(texto, params)
async function transaccion(fn) {
  const p = await obtener();
  const tx = new sql.Transaction(p);
  await tx.begin();
  try {
    const q = (texto, params) => correr(new sql.Request(tx), texto, params);
    const resultado = await fn(q);
    await tx.commit();
    return resultado;
  } catch (e) {
    try { await tx.rollback(); } catch (_) { /* ya estaba cerrada */ }
    throw e;
  }
}

// errores de sql a mensajes claros
function mensajeSql(e) {
  if (e instanceof ErrorApi) return null;
  const t = (e && e.message) || '';
  const pares = [
    ['uq_usuarios_correo', 'Ese correo ya está registrado.'],
    ['uq_alumnos_matricula', 'Ese número de control ya está registrado.'],
    ['uq_actividades_nombre', 'Ya existe una actividad con ese nombre en el periodo.'],
    ['ux_inscripcion_activa', 'El alumno ya tiene una actividad inscrita.'],
    ['ux_periodo_activo', 'Solo puede haber un periodo activo.'],
    ['uq_horarios', 'Hay dos horarios iguales en la misma actividad.'],
    ['uq_lugares_nombre', 'Ese lugar ya existe.'],
    ['uq_especialidades_nombre', 'Esa especialidad ya existe.'],
  ];
  for (const [clave, mensaje] of pares) {
    if (t.includes(clave)) return mensaje;
  }
  if (e && (e.number === 2627 || e.number === 2601)) return 'Ya existe un registro con esos datos.';
  if (e && e.number === 547) {
    return t.includes('DELETE') ? 'El registro depende de otro que ya no existe.' : 'Los datos no cumplen el formato permitido.';
  }
  return null;
}

module.exports = { sql, consulta, transaccion, mensajeSql, obtener };
