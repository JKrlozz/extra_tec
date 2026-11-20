// errores y ayudas comunes

class ErrorApi extends Error {
  constructor(mensaje, estado = 400) {
    super(mensaje);
    this.estado = estado;
  }
}

// atrapa errores de funciones async
function ruta(fn) {
  return function (req, res, next) {
    return Promise.resolve(fn(req, res, next)).catch(next);
  };
}

// lanza error si hay texto
function revisar(error) {
  if (error) {
    throw new ErrorApi(error);
  }
}

// fecha de hoy en mexico
function hoy() {
  const formato = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Mexico_City' });
  return formato.format(new Date());
}

function entero(valor) {
  const n = Number(valor);
  if (Number.isInteger(n)) {
    return n;
  }
  return NaN;
}

// id de la url o error
function idDe(valor, texto = 'Identificador inválido.') {
  const n = entero(valor);
  if (!(n > 0)) {
    throw new ErrorApi(texto);
  }
  return n;
}

function NOMBRE_COMPLETO(u) {
  return "LTRIM(RTRIM(COALESCE(" + u + ".nombres, '') + ' ' + COALESCE(" + u +
    ".apellido_paterno, '') + ' ' + COALESCE(" + u + ".apellido_materno, '')))";
}

// hora de mexico dentro de sql
const AHORA_SQL = "CONVERT(DATETIME2(0), SYSDATETIMEOFFSET() AT TIME ZONE 'Central Standard Time (Mexico)')";

module.exports = { ErrorApi, ruta, revisar, hoy, entero, idDe, NOMBRE_COMPLETO, AHORA_SQL };
