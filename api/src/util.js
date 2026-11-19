// errores con mensaje para el usuario y ayudas comunes

class ErrorApi extends Error {
  constructor(mensaje, estado = 400) {
    super(mensaje);
    this.estado = estado;
  }
}

// atrapa errores de funciones async
const ruta = (fn) => (req, res, next) => Promise.resolve(fn(req, res, next)).catch(next);

// lanza el error si la validacion regreso texto
function revisar(error) {
  if (error) throw new ErrorApi(error);
}

// fecha de hoy en Mexico, AAAA-MM-DD
function hoy() {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Mexico_City' }).format(new Date());
}

function entero(valor) {
  const n = Number(valor);
  return Number.isInteger(n) ? n : NaN;
}

// id de la url, o error
function idDe(valor, texto = 'Identificador inválido.') {
  const n = entero(valor);
  if (!(n > 0)) throw new ErrorApi(texto);
  return n;
}

const NOMBRE_COMPLETO = (u) =>
  `LTRIM(RTRIM(COALESCE(${u}.nombres, '') + ' ' + COALESCE(${u}.apellido_paterno, '') + ' ' + COALESCE(${u}.apellido_materno, '')))`;

// hora de Mexico dentro de SQL
const AHORA_SQL = "CONVERT(DATETIME2(0), SYSDATETIMEOFFSET() AT TIME ZONE 'Central Standard Time (Mexico)')";

module.exports = { ErrorApi, ruta, revisar, hoy, entero, idDe, NOMBRE_COMPLETO, AHORA_SQL };
