// contrasenas y sesiones
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const { consulta } = require('./db');
const { ErrorApi } = require('./util');

const ITERACIONES = 120000;
const VIGENCIA = '30d';
const MENSAJE_SESION = 'La sesión terminó. Inicia sesión de nuevo.';

function secreto() {
  const s = process.env.JWT_SECRET;
  if (!s || s.length < 32) {
    throw new Error('JWT_SECRET debe existir y tener al menos 32 caracteres.');
  }
  return s;
}

// formato pbkdf2 iteraciones sal hash
function generarHash(clave) {
  const sal = crypto.randomBytes(16);
  const hash = crypto.pbkdf2Sync(clave, sal, ITERACIONES, 32, 'sha256');
  const salTexto = sal.toString('base64');
  const hashTexto = hash.toString('base64');
  return 'pbkdf2$' + ITERACIONES + '$' + salTexto + '$' + hashTexto;
}

function verificarHash(clave, guardado) {
  const partes = String(guardado).split('$');
  if (partes.length !== 4) {
    return false;
  }
  if (partes[0] !== 'pbkdf2') {
    return false;
  }
  const iteraciones = parseInt(partes[1], 10);
  if (!(iteraciones > 0)) {
    return false;
  }
  const sal = Buffer.from(partes[2], 'base64');
  const esperado = Buffer.from(partes[3], 'base64');
  const real = crypto.pbkdf2Sync(clave, sal, iteraciones, esperado.length, 'sha256');
  if (esperado.length !== real.length) {
    return false;
  }
  return crypto.timingSafeEqual(esperado, real);
}

// clave temporal de diez letras
function claveInicial() {
  const letras = 'abcdefghjkmnpqrstuvwxyz23456789';
  let clave = '';
  for (let i = 0; i < 10; i++) {
    const posicion = crypto.randomInt(letras.length);
    clave = clave + letras[posicion];
  }
  return clave;
}

function firmar(usuario) {
  const datos = { id: usuario.id, rol: usuario.rol };
  return jwt.sign(datos, secreto(), { expiresIn: VIGENCIA });
}

// revisa el token y la cuenta
async function exigirSesion(req, res, next) {
  try {
    let cabecera = req.headers.authorization;
    if (!cabecera) {
      cabecera = '';
    }
    if (!cabecera.startsWith('Bearer ')) {
      throw new ErrorApi(MENSAJE_SESION, 401);
    }
    const token = cabecera.slice(7);
    let datos;
    try {
      datos = jwt.verify(token, secreto());
    } catch (e) {
      throw new ErrorApi(MENSAJE_SESION, 401);
    }
    const filas = await consulta('SELECT id, rol, activo FROM usuarios WHERE id = @id', { id: datos.id });
    if (filas.length === 0 || !filas[0].activo) {
      throw new ErrorApi(MENSAJE_SESION, 401);
    }
    req.usuario = { id: filas[0].id, rol: filas[0].rol };
    next();
  } catch (e) {
    next(e);
  }
}

// deja pasar solo ciertos roles
function solo(...roles) {
  return function (req, res, next) {
    if (!roles.includes(req.usuario.rol)) {
      return next(new ErrorApi('No tienes permiso para esta acción.', 403));
    }
    next();
  };
}

module.exports = { generarHash, verificarHash, claveInicial, firmar, exigirSesion, solo };
