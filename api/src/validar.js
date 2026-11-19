// mismas reglas que la app (Limites.kt y NumeroControl.kt)
// todas regresan el texto del error o null
const { hoy } = require('./util');

const L = {
  NOMBRE_MIN: 2, NOMBRES_MAX: 50, APELLIDO_MAX: 40, CORREO_MAX: 100,
  CLAVE_MIN: 8, CLAVE_MAX: 30, TELEFONO: 10, ESPECIALIDAD_MAX: 50,
  ACTIVIDAD_MIN: 3, ACTIVIDAD_MAX: 50, DESCRIPCION_ACTIVIDAD_MAX: 200,
  CUPO_MIN: 1, CUPO_MAX: 100, LUGAR_MAX: 60, DESCRIPCION_EVIDENCIA_MAX: 300,
  FOTOS_MAX: 10, FOTO_BYTES_MAX: 8 * 1024 * 1024, SEMESTRE_MAX: 8, DOMINIO: '@itsm.edu.mx',
};

const letras = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' .-]+$/;
const correoRe = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const digitos = /^[0-9]+$/;
const fechaRe = /^[0-9]{4}-[0-9]{2}-[0-9]{2}$/;
const horaRe = /^([01][0-9]|2[0-3]):[0-5][0-9]$/;
const matriculaRe = /^[0-9]{3}T[0-9]{4}$/;

const MENSAJE_MATRICULA = 'El número de control debe tener el formato 000T0000, por ejemplo 232T0182.';

const texto = (v) => (typeof v === 'string' ? v : v == null ? '' : String(v));

function revisarNombre(campo, valor, maximo, obligatorio) {
  const t = texto(valor).trim();
  if (!t) return obligatorio ? `Escribe ${campo}.` : null;
  if (t.length < L.NOMBRE_MIN) return `${campo} debe tener al menos ${L.NOMBRE_MIN} letras.`;
  if (t.length > maximo) return `${campo} puede tener hasta ${maximo} caracteres.`;
  if (!letras.test(t)) return `${campo} solo puede tener letras.`;
  return null;
}

function revisarCorreo(valor) {
  const t = texto(valor).trim();
  if (!t) return 'Escribe el correo.';
  if (t.length > L.CORREO_MAX) return `El correo puede tener hasta ${L.CORREO_MAX} caracteres.`;
  if (!correoRe.test(t)) return 'Escribe un correo válido, por ejemplo nombre@itsm.edu.mx.';
  if (!t.toLowerCase().endsWith(L.DOMINIO)) return `El correo debe terminar en ${L.DOMINIO}.`;
  return null;
}

function normalizarMatricula(valor) {
  return texto(valor).trim().toUpperCase();
}

function revisarCorreoAlumno(valor, matricula) {
  const error = revisarCorreo(valor);
  if (error) return error;
  if (texto(valor).trim().toLowerCase() !== texto(matricula).trim().toLowerCase() + L.DOMINIO) {
    return `El correo del alumno debe ser su número de control, por ejemplo 193t0183${L.DOMINIO}.`;
  }
  return null;
}

function revisarClave(valor, puedeIrVacia) {
  const v = texto(valor);
  if (!v && puedeIrVacia) return null;
  if (v.length < L.CLAVE_MIN || v.length > L.CLAVE_MAX) {
    return `La contraseña debe tener de ${L.CLAVE_MIN} a ${L.CLAVE_MAX} caracteres.`;
  }
  if (v.includes(' ')) return 'La contraseña no puede tener espacios.';
  return null;
}

function revisarPersona(nombres, paterno, materno, correo) {
  return (
    revisarNombre('Nombre(s)', nombres, L.NOMBRES_MAX, true) ||
    revisarNombre('Apellido paterno', paterno, L.APELLIDO_MAX, true) ||
    revisarNombre('Apellido materno', materno, L.APELLIDO_MAX, false) ||
    revisarCorreo(correo)
  );
}

function revisarTelefono(valor) {
  const t = texto(valor).trim();
  if (!t) return null;
  if (t.length !== L.TELEFONO || !digitos.test(t)) return `El teléfono debe tener ${L.TELEFONO} dígitos.`;
  return null;
}

function revisarEspecialidad(valor) {
  return revisarNombre('Especialidad', valor, L.ESPECIALIDAD_MAX, false);
}

function revisarActividad(nombre, descripcion, cupo) {
  const t = texto(nombre).trim();
  if (t.length < L.ACTIVIDAD_MIN || t.length > L.ACTIVIDAD_MAX) {
    return `El nombre debe tener de ${L.ACTIVIDAD_MIN} a ${L.ACTIVIDAD_MAX} caracteres.`;
  }
  if (texto(descripcion).trim().length > L.DESCRIPCION_ACTIVIDAD_MAX) {
    return `La descripción puede tener hasta ${L.DESCRIPCION_ACTIVIDAD_MAX} caracteres.`;
  }
  if (!Number.isInteger(cupo) || cupo < L.CUPO_MIN || cupo > L.CUPO_MAX) {
    return `El cupo debe ser de ${L.CUPO_MIN} a ${L.CUPO_MAX} alumnos.`;
  }
  return null;
}

function revisarLugar(valor) {
  const t = texto(valor).trim();
  if (t.length < L.NOMBRE_MIN || t.length > L.LUGAR_MAX) {
    return `El lugar debe tener de ${L.NOMBRE_MIN} a ${L.LUGAR_MAX} caracteres.`;
  }
  return null;
}

function revisarDescripcionEvidencia(valor) {
  if (texto(valor).trim().length > L.DESCRIPCION_EVIDENCIA_MAX) {
    return `La descripción puede tener hasta ${L.DESCRIPCION_EVIDENCIA_MAX} caracteres.`;
  }
  return null;
}

// existe en el calendario
function esFechaReal(v) {
  if (!fechaRe.test(v)) return false;
  const d = new Date(v + 'T00:00:00Z');
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === v;
}

// fecha real, sin futuras
function revisarFecha(valor) {
  const v = texto(valor);
  if (!esFechaReal(v)) return 'La fecha debe tener el formato AAAA-MM-DD.';
  if (v > hoy()) return 'La fecha no puede ser posterior a hoy.';
  return null;
}

// dias entre dos fechas AAAA-MM-DD
const diasEntre = (a, b) => Math.round((new Date(b + 'T00:00:00Z') - new Date(a + 'T00:00:00Z')) / 86400000);

// fechas del semestre: reales, fin despues de inicio, no mas de un ano y fin que no haya pasado
function revisarSemestre(inicio, fin, hoyTexto = hoy()) {
  const a = texto(inicio).trim();
  const b = texto(fin).trim();
  if (!esFechaReal(a)) return 'La fecha de inicio debe tener el formato AAAA-MM-DD.';
  if (!esFechaReal(b)) return 'La fecha de fin debe tener el formato AAAA-MM-DD.';
  if (b <= a) return 'La fecha de fin debe ser posterior a la de inicio.';
  if (diasEntre(a, b) > 366) return 'El semestre no puede durar más de un año.';
  if (b < hoyTexto) return 'La fecha de fin no puede ser anterior a hoy. Para terminar el semestre usa «Cerrar y archivar».';
  return null;
}

module.exports = {
  L, horaRe, matriculaRe, MENSAJE_MATRICULA, texto, esFechaReal,
  revisarNombre, revisarCorreo, normalizarMatricula, revisarCorreoAlumno, revisarClave,
  revisarPersona, revisarTelefono, revisarEspecialidad, revisarActividad, revisarLugar,
  revisarDescripcionEvidencia, revisarFecha, revisarSemestre,
};
