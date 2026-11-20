// filtros opcionales de los reportes
// solo usa parametros nunca texto directo
const { ErrorApi, NOMBRE_COMPLETO } = require('./util');
const V = require('./validar');

const TEXTO_MAX = 100;

// texto para like con comodines escapados
function patron(filtro) {
  const t = V.texto(filtro).trim();
  let escapado = '';
  for (let i = 0; i < t.length; i++) {
    const c = t[i];
    if (c === '\\' || c === '%' || c === '_' || c === '[') {
      escapado = escapado + '\\' + c;
    } else {
      escapado = escapado + c;
    }
  }
  return '%' + escapado + '%';
}

// entero positivo opcional o null
function entero(valor, mensaje, maximo) {
  const t = V.texto(valor).trim();
  if (!t) {
    return null;
  }
  if (!/^[0-9]+$/.test(t)) {
    throw new ErrorApi(mensaje);
  }
  const n = Number(t);
  if (n < 1) {
    throw new ErrorApi(mensaje);
  }
  if (maximo && n > maximo) {
    throw new ErrorApi(mensaje);
  }
  return n;
}

// texto corto opcional
function corto(valor, campo) {
  const t = V.texto(valor).trim();
  if (t.length > TEXTO_MAX) {
    throw new ErrorApi(`El ${campo} no puede pasar de ${TEXTO_MAX} caracteres.`);
  }
  return t;
}

// revisa la consulta y arma condiciones
function leerFiltros(query) {
  let q = query;
  if (!q) {
    q = {};
  }
  const desde = V.texto(q.desde).trim();
  const hasta = V.texto(q.hasta).trim();
  if (desde && !V.esFechaReal(desde)) {
    throw new ErrorApi('La fecha debe tener el formato AAAA-MM-DD.');
  }
  if (hasta && !V.esFechaReal(hasta)) {
    throw new ErrorApi('La fecha debe tener el formato AAAA-MM-DD.');
  }
  let inicio = '1000-01-01';
  if (desde) {
    inicio = desde;
  }
  let fin = '9999-12-31';
  if (hasta) {
    fin = hasta;
  }
  if (inicio > fin) {
    throw new ErrorApi('La fecha inicial es posterior a la final.');
  }

  const matricula = corto(q.matricula, 'número de control').toUpperCase();
  const nombre = corto(q.nombre, 'nombre');
  const docente = corto(q.docente, 'docente');
  const carrera = entero(q.carrera, 'La carrera no es válida.');
  const semestre = entero(q.semestre, 'El semestre debe estar entre 1 y 8.', V.L.SEMESTRE_MAX);
  const actividad = entero(q.actividad, 'La actividad no es válida.');

  const p = { i: inicio, f: fin };

  // condiciones para alumnos
  const alumno = [];
  if (matricula) {
    alumno.push("al.matricula LIKE @matricula ESCAPE '\\'");
    p.matricula = patron(matricula);
  }
  if (nombre) {
    alumno.push(NOMBRE_COMPLETO('u') + " LIKE @nombre ESCAPE '\\'");
    p.nombre = patron(nombre);
  }
  if (carrera) {
    alumno.push('al.id_carrera = @carrera');
    p.carrera = carrera;
  }
  if (semestre) {
    alumno.push('al.semestre = @semestre');
    p.semestre = semestre;
  }
  if (actividad) {
    p.actividad = actividad;
  }

  // condiciones para docentes
  const maestro = [];
  if (docente) {
    maestro.push('(' + NOMBRE_COMPLETO('u') + " LIKE @docente ESCAPE '\\' OR u.correo LIKE @docente ESCAPE '\\')");
    p.docente = patron(docente);
  }
  if (actividad) {
    maestro.push('EXISTS (SELECT 1 FROM actividades fa WHERE fa.id_docente = u.id AND fa.id = @actividad)');
  }

  // busca a alguien en especifico
  let especifico = false;
  if (matricula || nombre) {
    especifico = true;
  }

  return {
    p: p,
    desde: desde,
    hasta: hasta,
    actividad: actividad,
    alumno: alumno,
    maestro: maestro,
    especifico: especifico,
  };
}

// une condiciones extra al where
function y(condiciones) {
  let texto = '';
  for (let i = 0; i < condiciones.length; i++) {
    texto = texto + ' AND ' + condiciones[i];
  }
  return texto;
}

module.exports = { leerFiltros, patron, y };
