// contrasenas y sesiones
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const { consulta } = require('./db');
const { ErrorApi } = require('./util');

const ITERACIONES = 120000;
const VIGENCIA = '30d';

function secreto() {
  const s = process.env.JWT_SECRET;
  if (!s || s.length < 32) {
    throw new Error('JWT_SECRET debe existir y tener al menos 32 caracteres.');
  }
  return s;
}

// queda como pbkdf2$iteraciones$sal$hash
function generarHash(clave) {
  const sal = crypto.randomBytes(16);
  const hash = crypto.pbkdf2Sync(clave, sal, ITERACIONES, 32, 'sha256');
  return `pbkdf2$${ITERACIONES}$${sal.toString('base64')}$${hash.toString('base64')}`;
}

function verificarHash(clave, guardado) {
  const partes = String(guardado).split('$');
  if (partes.length !== 4 || partes[0] !== 'pbkdf2') return false;
  const iteraciones = parseInt(partes[1], 10);
  if (!(iteraciones > 0)) return false;
  const esperado = Buffer.from(partes[3], 'base64');
  const real = crypto.pbkdf2Sync(clave, Buffer.from(partes[2], 'base64'), iteraciones, esperado.length, 'sha256');
  return esperado.length === real.length && crypto.timingSafeEqual(esperado, real);
}

// clave temporal de diez letras
function claveInicial() {
  const letras = 'abcdefghjkmnpqrstuvwxyz23456789';
  let clave = '';
  for (let i = 0; i < 10; i++) clave += letras[crypto.randomInt(letras.length)];
  return clave;
}

function firmar(usuario) {
  return jwt.sign({ id: usuario.id, rol: usuario.rol }, secreto(), { expiresIn: VIGENCIA });
}

// pone req.usuario si el token sirve y la cuenta sigue activa
async function exigirSesion(req, res, next) {
  try {
    const cabecera = req.headers.authorization || '';
    if (!cabecera.startsWith('Bearer ')) throw new ErrorApi('La sesión terminó. Inicia sesión de nuevo.', 401);
    let datos;
    try {
      datos = jwt.verify(cabecera.slice(7), secreto());
    } catch (e) {
      throw new ErrorApi('La sesión terminó. Inicia sesión de nuevo.', 401);
    }
    const filas = await consulta('SELECT id, rol, activo FROM usuarios WHERE id = @id', { id: datos.id });
    if (filas.length === 0 || !filas[0].activo) {
      throw new ErrorApi('La sesión terminó. Inicia sesión de nuevo.', 401);
    }
    req.usuario = { id: filas[0].id, rol: filas[0].rol };
    next();
  } catch (e) {
    next(e);
  }
}

// limita una ruta a ciertos roles
const solo = (...roles) => (req, res, next) => {
  if (!roles.includes(req.usuario.rol)) {
    return next(new ErrorApi('No tienes permiso para esta acción.', 403));
  }
  next();
};

module.exports = { generarHash, verificarHash, claveInicial, firmar, exigirSesion, solo };
