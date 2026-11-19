// reportes para detectar problemas y tomar decisiones (solo administrador)
const express = require('express');
const { consulta } = require('../db');
const { solo } = require('../auth');
const { ErrorApi, ruta, NOMBRE_COMPLETO } = require('../util');
const V = require('../validar');

const router = express.Router();
const MINIMO_ASISTENCIA = 80.0;

// porcentaje con un decimal o vacio
function porcentaje(valor) {
  if (valor === null || valor === undefined) return '';
  const n = Number(valor);
  return n % 1 === 0 ? String(n) : n.toFixed(1);
}

const minuscula = (t) => String(t).toLowerCase();
const comparar = (a, b) => (a < b ? -1 : a > b ? 1 : 0);

// actividades con mas problemas primero
async function porActividad(p) {
  const filas = await consulta(
    `SELECT a.id, a.nombre AS actividad, ${NOMBRE_COMPLETO('d')} AS docente, a.cupo,
            (SELECT COUNT(*) FROM inscripciones i WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos,
            (SELECT COUNT(*) FROM sesiones s WHERE s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f) AS sesiones,
            (SELECT COUNT(*) FROM sesiones s
              WHERE s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f
                AND NOT EXISTS (SELECT 1 FROM evidencias e WHERE e.id_actividad = a.id AND e.fecha = s.fecha)) AS sin_evidencia,
            (SELECT CONVERT(VARCHAR(10), MAX(s.fecha), 23) FROM sesiones s
              WHERE s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f) AS ultima,
            (SELECT ROUND(100.0 * SUM(CAST(x.presente AS INT)) / COUNT(x.id), 1)
               FROM asistencias x JOIN sesiones s ON s.id = x.id_sesion
              WHERE s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f) AS asistencia_pct
       FROM actividades a
       LEFT JOIN usuarios d ON d.id = a.id_docente
      WHERE a.activa = 1`,
    p,
  );
  // alumnos de cada actividad que no llegan al minimo
  const alumnos = await consulta(
    `SELECT i.id_actividad AS actividad, COUNT(DISTINCT s.id) AS sesiones,
            COALESCE(SUM(CAST(x.presente AS INT)), 0) AS presentes
       FROM inscripciones i
       JOIN sesiones s ON s.id_actividad = i.id_actividad AND s.fecha BETWEEN @i AND @f
       LEFT JOIN asistencias x ON x.id_sesion = s.id AND x.id_alumno = i.id_alumno
      WHERE i.estado = 'activa'
      GROUP BY i.id_actividad, i.id_alumno`,
    p,
  );
  const riesgo = new Map();
  for (const a of alumnos) {
    if (a.sesiones > 0 && (a.presentes * 100.0) / a.sesiones < MINIMO_ASISTENCIA) {
      riesgo.set(a.actividad, (riesgo.get(a.actividad) || 0) + 1);
    }
  }

  const resultado = filas.map((f) => {
    const enRiesgo = riesgo.get(f.id) || 0;
    const avisos = [];
    if (f.inscritos === 0) avisos.push('Sin alumnos inscritos');
    if (!f.docente) avisos.push('Sin docente asignado');
    if (f.inscritos > 0 && f.sesiones === 0) avisos.push('Sin pases de lista en el periodo');
    if (enRiesgo > 0) avisos.push(`${enRiesgo} alumno(s) por debajo del 80 %`);
    if (f.sin_evidencia > 0) avisos.push(`${f.sin_evidencia} sesión(es) sin evidencia`);
    return {
      actividad: f.actividad,
      docente: f.docente || '',
      cupo: String(f.cupo),
      inscritos: String(f.inscritos),
      ocupacion: f.cupo > 0 ? String(Math.floor((f.inscritos * 100) / f.cupo)) : '0',
      sesiones: String(f.sesiones),
      ultima: f.ultima || '',
      asistencia_pct: porcentaje(f.asistencia_pct),
      en_riesgo: String(enRiesgo),
      sin_evidencia: String(f.sin_evidencia),
      atencion: avisos.length === 0 ? 'Sin pendientes' : avisos.join('\n'),
      pendientes: String(avisos.length),
    };
  });
  resultado.sort((a, b) =>
    comparar(Number(b.pendientes), Number(a.pendientes)) || comparar(minuscula(a.actividad), minuscula(b.actividad)));
  return resultado;
}

// alumnos en riesgo primero
async function porAlumno(p) {
  const filas = await consulta(
    `SELECT ${NOMBRE_COMPLETO('u')} AS alumno, al.matricula, a.nombre AS actividad,
            COUNT(DISTINCT s.id) AS sesiones,
            COALESCE(SUM(CAST(x.presente AS INT)), 0) AS presentes
       FROM alumnos al
       JOIN usuarios u ON u.id = al.id_usuario
       LEFT JOIN inscripciones i ON i.id_alumno = al.id_usuario AND i.estado = 'activa'
       LEFT JOIN actividades a ON a.id = i.id_actividad AND a.activa = 1
       LEFT JOIN sesiones s ON s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f
       LEFT JOIN asistencias x ON x.id_sesion = s.id AND x.id_alumno = al.id_usuario
      WHERE u.activo = 1
      GROUP BY u.id, u.nombres, u.apellido_paterno, u.apellido_materno, al.matricula, a.id, a.nombre`,
    p,
  );
  const resultado = filas.map((f) => {
    const actividad = f.actividad || '';
    const faltas = f.sesiones - f.presentes;
    const pct = f.sesiones > 0 ? (f.presentes * 100.0) / f.sesiones : 0.0;
    let estado;
    let prioridad;
    if (!actividad) {
      estado = 'Sin actividad inscrita';
      prioridad = 1;
    } else if (f.sesiones === 0) {
      estado = 'Aún sin pases de lista';
      prioridad = 2;
    } else if (pct < MINIMO_ASISTENCIA) {
      estado = `En riesgo: ${faltas} falta(s) en ${f.sesiones} sesión(es)`;
      prioridad = 0;
    } else {
      estado = 'Cumple el 80 % para su constancia';
      prioridad = 3;
    }
    return {
      alumno: f.alumno,
      matricula: f.matricula,
      actividad,
      sesiones: String(f.sesiones),
      presentes: String(f.presentes),
      faltas: String(faltas),
      asistencia_pct: f.sesiones > 0 ? porcentaje(pct) : '',
      estado,
      prioridad: String(prioridad),
      orden: pct.toFixed(2).padStart(6, '0'),
    };
  });
  resultado.sort((a, b) =>
    comparar(Number(a.prioridad), Number(b.prioridad)) || comparar(a.orden, b.orden) ||
    comparar(minuscula(a.alumno), minuscula(b.alumno)));
  return resultado;
}

// docentes con pendientes primero
async function porDocente(p) {
  const filas = await consulta(
    `SELECT ${NOMBRE_COMPLETO('u')} AS docente,
            (SELECT COUNT(*) FROM actividades a WHERE a.id_docente = u.id AND a.activa = 1) AS actividades,
            (SELECT COUNT(*) FROM sesiones s JOIN actividades a ON a.id = s.id_actividad
              WHERE a.id_docente = u.id AND a.activa = 1 AND s.fecha BETWEEN @i AND @f) AS sesiones,
            (SELECT COUNT(*) FROM sesiones s JOIN actividades a ON a.id = s.id_actividad
              WHERE a.id_docente = u.id AND a.activa = 1 AND s.fecha BETWEEN @i AND @f
                AND NOT EXISTS (SELECT 1 FROM evidencias e WHERE e.id_actividad = a.id AND e.fecha = s.fecha)) AS sin_evidencia,
            (SELECT COUNT(*) FROM evidencias e JOIN actividades a ON a.id = e.id_actividad
              WHERE a.id_docente = u.id AND a.activa = 1 AND e.fecha BETWEEN @i AND @f) AS evidencias,
            (SELECT ROUND(100.0 * SUM(CAST(x.presente AS INT)) / COUNT(x.id), 1)
               FROM asistencias x
               JOIN sesiones s ON s.id = x.id_sesion
               JOIN actividades a ON a.id = s.id_actividad
              WHERE a.id_docente = u.id AND a.activa = 1 AND s.fecha BETWEEN @i AND @f) AS asistencia_pct
       FROM docentes dc JOIN usuarios u ON u.id = dc.id_usuario
      WHERE u.activo = 1`,
    p,
  );
  const resultado = filas.map((f) => {
    let estado;
    let prioridad;
    if (f.actividades === 0) {
      estado = 'Sin actividades asignadas';
      prioridad = 1;
    } else if (f.sin_evidencia > 0) {
      estado = `${f.sin_evidencia} sesión(es) sin evidencia`;
      prioridad = 0;
    } else if (f.sesiones === 0) {
      estado = 'Sin pases de lista en el periodo';
      prioridad = 2;
    } else {
      estado = 'Al corriente';
      prioridad = 3;
    }
    return {
      docente: f.docente,
      actividades: String(f.actividades),
      sesiones: String(f.sesiones),
      sin_evidencia: String(f.sin_evidencia),
      evidencias: String(f.evidencias),
      asistencia_pct: porcentaje(f.asistencia_pct),
      estado,
      prioridad: String(prioridad),
      pendientes: String(f.sin_evidencia),
    };
  });
  resultado.sort((a, b) =>
    comparar(Number(a.prioridad), Number(b.prioridad)) || comparar(Number(b.pendientes), Number(a.pendientes)) ||
    comparar(minuscula(a.docente), minuscula(b.docente)));
  return resultado;
}

router.get('/reportes', solo('admin'), ruta(async (req, res) => {
  const tipo = V.texto(req.query.tipo);
  const desde = V.texto(req.query.desde);
  const hasta = V.texto(req.query.hasta);
  if ((desde && !V.esFechaReal(desde)) || (hasta && !V.esFechaReal(hasta))) {
    throw new ErrorApi('La fecha debe tener el formato AAAA-MM-DD.');
  }
  const inicio = desde || '1000-01-01';
  const fin = hasta || '9999-12-31';
  if (inicio > fin) throw new ErrorApi('La fecha inicial es posterior a la final.');
  const p = { i: inicio, f: fin };
  if (tipo === 'actividad') return res.json(await porActividad(p));
  if (tipo === 'alumno') return res.json(await porAlumno(p));
  if (tipo === 'docente') return res.json(await porDocente(p));
  throw new ErrorApi('El reporte debe ser por actividad, alumno o docente.');
}));

module.exports = router;
