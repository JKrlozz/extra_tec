// reportes para el administrador
const express = require('express');
const { consulta } = require('../db');
const { solo } = require('../auth');
const { ErrorApi, ruta, NOMBRE_COMPLETO } = require('../util');
const { leerFiltros, y } = require('../reportes_filtros');

const router = express.Router();
const MINIMO_ASISTENCIA = 80.0;

// porcentaje con un decimal o vacio
function porcentaje(valor) {
  if (valor === null || valor === undefined) return '';
  const n = Number(valor);
  if (n % 1 === 0) {
    return String(n);
  }
  return n.toFixed(1);
}

function minuscula(t) {
  return String(t).toLowerCase();
}

function comparar(a, b) {
  if (a < b) {
    return -1;
  }
  if (a > b) {
    return 1;
  }
  return 0;
}

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
  // alumnos debajo del minimo
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
      let cuantos = riesgo.get(a.actividad);
      if (!cuantos) {
        cuantos = 0;
      }
      riesgo.set(a.actividad, cuantos + 1);
    }
  }

  const resultado = [];
  for (const f of filas) {
    const enRiesgo = riesgo.get(f.id) || 0;
    const avisos = [];
    if (f.inscritos === 0) avisos.push('Sin alumnos inscritos');
    if (!f.docente) avisos.push('Sin docente asignado');
    if (f.inscritos > 0 && f.sesiones === 0) avisos.push('Sin pases de lista en el periodo');
    if (enRiesgo > 0) avisos.push(`${enRiesgo} alumno(s) por debajo del 80 %`);
    if (f.sin_evidencia > 0) avisos.push(`${f.sin_evidencia} sesión(es) sin evidencia`);
    let ocupacion = '0';
    if (f.cupo > 0) {
      ocupacion = String(Math.floor((f.inscritos * 100) / f.cupo));
    }
    let atencion = 'Sin pendientes';
    if (avisos.length > 0) {
      atencion = avisos.join('\n');
    }
    resultado.push({
      actividad: f.actividad,
      docente: f.docente || '',
      cupo: String(f.cupo),
      inscritos: String(f.inscritos),
      ocupacion: ocupacion,
      sesiones: String(f.sesiones),
      ultima: f.ultima || '',
      asistencia_pct: porcentaje(f.asistencia_pct),
      en_riesgo: String(enRiesgo),
      sin_evidencia: String(f.sin_evidencia),
      atencion: atencion,
      pendientes: String(avisos.length),
    });
  }
  resultado.sort(function (a, b) {
    const porPendientes = comparar(Number(b.pendientes), Number(a.pendientes));
    if (porPendientes !== 0) {
      return porPendientes;
    }
    return comparar(minuscula(a.actividad), minuscula(b.actividad));
  });
  return resultado;
}

// alumnos en riesgo primero
async function porAlumno(filtro) {
  const p = filtro.p;
  let deActividad = '';
  if (filtro.actividad) {
    deActividad = ' AND a.id = @actividad';
  }
  const filas = await consulta(
    `SELECT u.id, ${NOMBRE_COMPLETO('u')} AS alumno, al.matricula, COALESCE(c.nombre, '') AS carrera, al.semestre,
            a.nombre AS actividad,
            COUNT(DISTINCT s.id) AS sesiones,
            COALESCE(SUM(CAST(x.presente AS INT)), 0) AS presentes
       FROM alumnos al
       JOIN usuarios u ON u.id = al.id_usuario
       LEFT JOIN carreras c ON c.id = al.id_carrera
       LEFT JOIN inscripciones i ON i.id_alumno = al.id_usuario AND i.estado = 'activa'
       LEFT JOIN actividades a ON a.id = i.id_actividad AND a.activa = 1
       LEFT JOIN sesiones s ON s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f
       LEFT JOIN asistencias x ON x.id_sesion = s.id AND x.id_alumno = al.id_usuario
      WHERE u.activo = 1${y(filtro.alumno)}${deActividad}
      GROUP BY u.id, u.nombres, u.apellido_paterno, u.apellido_materno, al.matricula, c.nombre, al.semestre,
               a.id, a.nombre`,
    p,
  );
  // detalle solo para alguien especifico
  const detalle = new Map();
  if (filtro.especifico && filas.length > 0) {
    const sesiones = await consulta(
      `SELECT al.id_usuario AS alumno, a.nombre AS actividad, CONVERT(VARCHAR(10), s.fecha, 23) AS fecha,
              COALESCE(CAST(x.presente AS INT), 0) AS presente
         FROM alumnos al
         JOIN usuarios u ON u.id = al.id_usuario
         JOIN inscripciones i ON i.id_alumno = al.id_usuario AND i.estado = 'activa'
         JOIN actividades a ON a.id = i.id_actividad AND a.activa = 1
         JOIN sesiones s ON s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f
         LEFT JOIN asistencias x ON x.id_sesion = s.id AND x.id_alumno = al.id_usuario
        WHERE u.activo = 1${y(filtro.alumno)}${deActividad}
        ORDER BY s.fecha, a.nombre`,
      p,
    );
    for (const s of sesiones) {
      const clave = `${s.alumno}|${s.actividad}`;
      let marca = 'Falta';
      if (s.presente) {
        marca = 'Presente';
      }
      const linea = `${s.fecha} · ${marca}`;
      if (detalle.has(clave)) {
        detalle.set(clave, detalle.get(clave) + '\n' + linea);
      } else {
        detalle.set(clave, linea);
      }
    }
  }
  const resultado = [];
  for (const f of filas) {
    const actividad = f.actividad || '';
    const faltas = f.sesiones - f.presentes;
    let pct = 0.0;
    if (f.sesiones > 0) {
      pct = (f.presentes * 100.0) / f.sesiones;
    }
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
    let semestre = '';
    if (f.semestre !== null && f.semestre !== undefined) {
      semestre = String(f.semestre);
    }
    let asistencia = '';
    if (f.sesiones > 0) {
      asistencia = porcentaje(pct);
    }
    resultado.push({
      alumno: f.alumno,
      matricula: f.matricula,
      carrera: f.carrera,
      semestre: semestre,
      actividad: actividad,
      sesiones: String(f.sesiones),
      presentes: String(f.presentes),
      faltas: String(faltas),
      asistencia_pct: asistencia,
      estado: estado,
      prioridad: String(prioridad),
      orden: pct.toFixed(2).padStart(6, '0'),
      detalle: detalle.get(`${f.id}|${actividad}`) || '',
    });
  }
  resultado.sort(function (a, b) {
    const porPrioridad = comparar(Number(a.prioridad), Number(b.prioridad));
    if (porPrioridad !== 0) {
      return porPrioridad;
    }
    const porOrden = comparar(a.orden, b.orden);
    if (porOrden !== 0) {
      return porOrden;
    }
    return comparar(minuscula(a.alumno), minuscula(b.alumno));
  });
  return resultado;
}

// docentes con pendientes primero
async function porDocente(filtro) {
  const p = filtro.p;
  let deActividad = '';
  if (filtro.actividad) {
    deActividad = ' AND a.id = @actividad';
  }
  const filas = await consulta(
    `SELECT u.id, ${NOMBRE_COMPLETO('u')} AS docente, u.correo,
            (SELECT COUNT(*) FROM actividades a WHERE a.id_docente = u.id AND a.activa = 1${deActividad}) AS actividades,
            (SELECT COUNT(*) FROM sesiones s JOIN actividades a ON a.id = s.id_actividad
              WHERE a.id_docente = u.id AND a.activa = 1${deActividad} AND s.fecha BETWEEN @i AND @f) AS sesiones,
            (SELECT COUNT(*) FROM sesiones s JOIN actividades a ON a.id = s.id_actividad
              WHERE a.id_docente = u.id AND a.activa = 1${deActividad} AND s.fecha BETWEEN @i AND @f
                AND NOT EXISTS (SELECT 1 FROM evidencias e WHERE e.id_actividad = a.id AND e.fecha = s.fecha)) AS sin_evidencia,
            (SELECT COUNT(*) FROM evidencias e JOIN actividades a ON a.id = e.id_actividad
              WHERE a.id_docente = u.id AND a.activa = 1${deActividad} AND e.fecha BETWEEN @i AND @f) AS evidencias,
            (SELECT ROUND(100.0 * SUM(CAST(x.presente AS INT)) / COUNT(x.id), 1)
               FROM asistencias x
               JOIN sesiones s ON s.id = x.id_sesion
               JOIN actividades a ON a.id = s.id_actividad
              WHERE a.id_docente = u.id AND a.activa = 1${deActividad} AND s.fecha BETWEEN @i AND @f) AS asistencia_pct
       FROM docentes dc JOIN usuarios u ON u.id = dc.id_usuario
      WHERE u.activo = 1${y(filtro.maestro)}`,
    p,
  );
  // detalle de cada actividad del docente
  const porActividadDocente = await consulta(
    `SELECT a.id_docente AS docente, a.nombre AS actividad,
            (SELECT COUNT(*) FROM sesiones s WHERE s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f) AS sesiones,
            (SELECT COUNT(*) FROM inscripciones i WHERE i.id_actividad = a.id AND i.estado = 'activa') AS inscritos,
            (SELECT COUNT(*) FROM evidencias e WHERE e.id_actividad = a.id AND e.fecha BETWEEN @i AND @f) AS evidencias,
            (SELECT ROUND(100.0 * SUM(CAST(x.presente AS INT)) / COUNT(x.id), 1)
               FROM asistencias x JOIN sesiones s ON s.id = x.id_sesion
              WHERE s.id_actividad = a.id AND s.fecha BETWEEN @i AND @f) AS asistencia_pct
       FROM actividades a
      WHERE a.activa = 1 AND a.id_docente IS NOT NULL${deActividad}
      ORDER BY a.nombre`,
    p,
  );
  const detalle = new Map();
  for (const a of porActividadDocente) {
    const pct = porcentaje(a.asistencia_pct);
    let textoAsistencia = 'sin datos';
    if (pct) {
      textoAsistencia = pct + ' %';
    }
    const linea = `${a.actividad}: ${a.sesiones} sesión(es), ${a.inscritos} inscrito(s), ` +
      `asistencia ${textoAsistencia}, ${a.evidencias} evidencia(s)`;
    if (detalle.has(a.docente)) {
      detalle.set(a.docente, detalle.get(a.docente) + '\n' + linea);
    } else {
      detalle.set(a.docente, linea);
    }
  }
  const resultado = [];
  for (const f of filas) {
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
    resultado.push({
      docente: f.docente,
      correo: f.correo || '',
      actividades: String(f.actividades),
      sesiones: String(f.sesiones),
      sin_evidencia: String(f.sin_evidencia),
      evidencias: String(f.evidencias),
      asistencia_pct: porcentaje(f.asistencia_pct),
      estado: estado,
      prioridad: String(prioridad),
      pendientes: String(f.sin_evidencia),
      detalle: detalle.get(f.id) || '',
    });
  }
  resultado.sort(function (a, b) {
    const porPrioridad = comparar(Number(a.prioridad), Number(b.prioridad));
    if (porPrioridad !== 0) {
      return porPrioridad;
    }
    const porPendientes = comparar(Number(b.pendientes), Number(a.pendientes));
    if (porPendientes !== 0) {
      return porPendientes;
    }
    return comparar(minuscula(a.docente), minuscula(b.docente));
  });
  return resultado;
}

router.get('/reportes', solo('admin'), ruta(async (req, res) => {
  let tipo = '';
  if (typeof req.query.tipo === 'string') {
    tipo = req.query.tipo;
  }
  const filtro = leerFiltros(req.query);
  // el de actividades se queda general
  if (tipo === 'actividad') {
    const lista = await porActividad({ i: filtro.p.i, f: filtro.p.f });
    res.json(lista);
    return;
  }
  if (tipo === 'alumno') {
    const lista = await porAlumno(filtro);
    res.json(lista);
    return;
  }
  if (tipo === 'docente') {
    const lista = await porDocente(filtro);
    res.json(lista);
    return;
  }
  throw new ErrorApi('El reporte debe ser por actividad, alumno o docente.');
}));

module.exports = router;
