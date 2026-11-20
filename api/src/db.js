// conexion a azure sql
const sql = require('mssql');
const { ErrorApi } = require('./util');

let pool = null;
let conectando = null;

function configuracion() {
  const servidor = process.env.DB_SERVER;
  const base = process.env.DB_NAME;
  const usuario = process.env.DB_USER;
  const clave = process.env.DB_PASSWORD;
  if (!servidor || !base || !usuario || !clave) {
    throw new Error('Faltan DB_SERVER, DB_NAME, DB_USER o DB_PASSWORD en el entorno.');
  }
  return {
    server: servidor,
    database: base,
    user: usuario,
    password: clave,
    options: { encrypt: true, trustServerCertificate: false },
    connectionTimeout: 30000,
    requestTimeout: 60000,
    pool: { max: 10, min: 0, idleTimeoutMillis: 30000 },
  };
}

function esperar(ms) {
  return new Promise(function (resolver) {
    setTimeout(resolver, ms);
  });
}

// la base gratuita tarda en despertar
async function conectar() {
  let ultimoError;
  for (let intento = 1; intento <= 4; intento++) {
    try {
      const nuevo = new sql.ConnectionPool(configuracion());
      nuevo.on('error', function () {
        pool = null;
      });
      await nuevo.connect();
      return nuevo;
    } catch (e) {
      ultimoError = e;
      console.error(`Conexión a la base fallida (intento ${intento}):`, e.message);
      await esperar(5000);
    }
  }
  throw ultimoError;
}

async function obtener() {
  if (pool && pool.connected) {
    return pool;
  }
  if (!conectando) {
    conectando = conectar()
      .then(function (p) {
        pool = p;
        return p;
      })
      .finally(function () {
        conectando = null;
      });
  }
  return conectando;
}

function preparar(request, params) {
  if (!params) {
    return;
  }
  const nombres = Object.keys(params);
  for (let i = 0; i < nombres.length; i++) {
    const nombre = nombres[i];
    const valor = params[nombre];
    if (Buffer.isBuffer(valor)) {
      request.input(nombre, sql.VarBinary(sql.MAX), valor);
    } else if (typeof valor === 'number') {
      if (Number.isInteger(valor)) {
        request.input(nombre, sql.Int, valor);
      } else {
        request.input(nombre, sql.Float, valor);
      }
    } else if (typeof valor === 'boolean') {
      if (valor) {
        request.input(nombre, sql.TinyInt, 1);
      } else {
        request.input(nombre, sql.TinyInt, 0);
      }
    } else if (valor === null || valor === undefined) {
      request.input(nombre, sql.NVarChar(sql.MAX), null);
    } else {
      request.input(nombre, sql.NVarChar(sql.MAX), String(valor));
    }
  }
}

async function correr(request, texto, params) {
  preparar(request, params);
  const resultado = await request.query(texto);
  let filas = resultado.recordset;
  if (!filas) {
    filas = [];
  }
  let total = 0;
  const cambios = resultado.rowsAffected || [];
  for (let i = 0; i < cambios.length; i++) {
    total = total + cambios[i];
  }
  filas.afectadas = total;
  return filas;
}

// una consulta sola
async function consulta(texto, params) {
  const p = await obtener();
  return correr(p.request(), texto, params);
}

// todo o nada con q
async function transaccion(fn) {
  const p = await obtener();
  const tx = new sql.Transaction(p);
  await tx.begin();
  try {
    const q = function (texto, params) {
      return correr(new sql.Request(tx), texto, params);
    };
    const resultado = await fn(q);
    await tx.commit();
    return resultado;
  } catch (e) {
    try {
      await tx.rollback();
    } catch (otro) {
      // ya estaba cerrada
    }
    throw e;
  }
}

// errores de sql a mensajes claros
function mensajeSql(e) {
  if (e instanceof ErrorApi) {
    return null;
  }
  let t = '';
  if (e && e.message) {
    t = e.message;
  }
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
  for (let i = 0; i < pares.length; i++) {
    if (t.includes(pares[i][0])) {
      return pares[i][1];
    }
  }
  if (e && (e.number === 2627 || e.number === 2601)) {
    return 'Ya existe un registro con esos datos.';
  }
  if (e && e.number === 547) {
    if (t.includes('DELETE')) {
      return 'El registro depende de otro que ya no existe.';
    }
    return 'Los datos no cumplen el formato permitido.';
  }
  return null;
}

module.exports = { sql, consulta, transaccion, mensajeSql, obtener };
