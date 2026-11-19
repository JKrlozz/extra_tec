// actividades, inscripciones, horario y constancias
const express = require('express');
const { consulta, transaccion } = require('../db');
const { solo } = require('../auth');
const { exigirTitular } = require('../acceso');
const { ErrorApi, ruta, revisar, idDe, entero, hoy, NOMBRE_COMPLETO, AHORA_SQL } = require('../util');
const V = require('../validar');
const { exigirVigente } = require('../semestre');

const router = express.Router();
const MINIMO_ASISTENCIA = 80.0;

const MESES = [
  'enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
  'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre',
];

const CAMPOS_HORARIO = `h.id_dia AS idDia, d.nombre AS diaSemana, h.hora_inicio AS horaInicio,
       h.hora_fin AS horaFin, h.id_lugar AS idLugar, l.nombre AS lugar`;

function horarioJson(f) {
  return {
    idDia: f.idDia, diaSemana: f.diaSemana, horaInicio: f.horaInicio.slice(0, 5),
    horaFin: f.horaFin.slice(0, 5), idLugar: f.idLugar, lugar: f.lugar,
  };
}

async function horariosDe(q, ids) {
  const mapa = new Map();
  if (ids.length === 0) return mapa;
  // los ids vienen de la base, no del usuario
  const filas = await q(
    `SELECT h.id_actividad AS idActividad, ${CAMPOS_HORARIO}
       FROM horarios h
       JOIN dias_semana d ON d.id = h.id_dia
       JOIN lugares l ON l.id = h.id_lugar
      WHERE h.id_actividad IN (${ids.map(Number).join(',')})
      ORDER BY d.orden, h.hora_inicio`,
  );
  for (const f of filas) {
    if (!mapa.has(f.idActividad)) mapa.set(f.idActividad, []);
    mapa.get(f.idActividad).push(horarioJson(f));
  }
  return mapa;
}

// actividades con el texto de su horario
async function armarActividades(q, filas) {
  const horarios = await horariosDe(q, filas.map((f) => f.id));
  return filas.map((f) => {
    const lista = horarios.get(f.id) || [];
    let textoHorario = '';
    let textoLugar = '';
    for (const h of lista) {
      if (textoHorario) textoHorario += '\n';
      textoHorario += `${h.diaSemana.slice(0, 3)} ${h.horaInicio} a ${h.horaFin}`;
      if (!textoLugar.includes(h.lugar)) {
        if (textoLugar) textoLugar += '\n';
        textoLugar += h.lugar;
      }
    }
    return {
      id: f.id,
      nombre: f.nombre,
      descripcion: f.descripcion || '',
      cupo: f.cupo,
      inscritos: f.inscritos,
      horario: textoHorario,
      lugar: textoLugar,
      docente: f.docente || '',
      idDocente: f.id_docente === undefined ? null : f.id_docente,
      inscrito: !!f.inscrito,
      horarios: lista,
    };
  });
}

// actividades que ve el alumno
router.get('/actividades', solo('alumno'), ruta(async (req, res) => {
  const filas = await consulta(
    `SELECT v.*, CASE WHEN EXISTS (SELECT 1 FROM inscripciones i
                                    WHERE i.id_actividad = v.id AND i.id_alumno = @alumno AND i.estado = 'activa')
                      THEN 1 ELSE 0 END AS inscrito
       FROM v_actividades v WHERE v.activa = 1 ORDER BY v.nombre`,
    { alumno: req.usuario.id },
  );
  res.json(await armarActividades(consulta, filas));
}));

// actividades que ve el administrador
router.get('/actividades/todas', solo('admin'), ruta(async (req, res) => {
  const filas = await consulta('SELECT v.*, 0 AS inscrito FROM v_actividades v WHERE v.activa = 1 ORDER BY v.nombre');
  res.json(await armarActividades(consulta, filas));
}));

// actividades del docente
router.get('/actividades/mias', solo('docente'), ruta(async (req, res) => {
  const filas = await consulta(
    'SELECT v.*, 0 AS inscrito FROM v_actividades v WHERE v.id_docente = @docente AND v.activa = 1 ORDER BY v.nombre',
    { docente: req.usuario.id },
  );
  res.json(await armarActividades(consulta, filas));
}));

// dias y lugares para horarios
router.get('/catalogos/horario', solo('admin'), ruta(async (req, res) => {
  const dias = await consulta('SELECT id, nombre FROM dias_semana ORDER BY orden');
  const lugares = await consulta('SELECT id, nombre FROM lugares ORDER BY nombre');
  res.json({ dias, lugares });
}));

router.get('/actividades/:id(\\d+)', solo('admin'), ruta(async (req, res) => {
  const id = idDe(req.params.id);
  const filas = await consulta('SELECT v.*, 0 AS inscrito FROM v_actividades v WHERE v.id = @id', { id });
  if (filas.length === 0) throw new ErrorApi('Actividad no encontrada.', 404);
  const [actividad] = await armarActividades(consulta, filas);
  res.json({ actividad, horarios: actividad.horarios });
}));

function validarHorarios(horarios, dias) {
  if (!Array.isArray(horarios) || horarios.length === 0) throw new ErrorApi('Indica al menos un horario.');
  if (horarios.length > 7) throw new ErrorApi('Puedes registrar hasta 7 horarios.');
  horarios.forEach((h, i) => {
    const inicio = V.texto(h.horaInicio);
    const fin = V.texto(h.horaFin);
    if (!dias.has(Number(h.idDia)) || !V.horaRe.test(inicio) || !V.horaRe.test(fin)) {
      throw new ErrorApi('Horario inválido: revisa el día y las horas.');
    }
    if (fin <= inicio) throw new ErrorApi('La hora de fin debe ser posterior a la de inicio.');
    revisar(V.revisarLugar(h.lugar));
    for (let j = 0; j < i; j++) {
      const otro = horarios[j];
      if (Number(otro.idDia) === Number(h.idDia) && inicio < otro.horaFin && otro.horaInicio < fin) {
        throw new ErrorApi('Hay dos horarios traslapados el mismo día.');
      }
    }
  });
}

// busca el lugar o lo crea
async function buscarLugar(q, nombre) {
  const t = V.texto(nombre).trim();
  const filas = await q('SELECT id FROM lugares WHERE nombre = @n', { n: t });
  if (filas.length > 0) return filas[0].id;
  const nuevo = await q('INSERT INTO lugares (nombre) OUTPUT INSERTED.id VALUES (@n)', { n: t });
  return nuevo[0].id;
}

async function guardarHorarios(q, idActividad, horarios) {
  await q('DELETE FROM horarios WHERE id_actividad = @id', { id: idActividad });
  for (const h of horarios) {
    await q(
      `INSERT INTO horarios (id_actividad, id_dia, hora_inicio, hora_fin, id_lugar)
       VALUES (@actividad, @dia, @inicio, @fin, @lugar)`,
      {
        actividad: idActividad, dia: Number(h.idDia), inicio: h.horaInicio, fin: h.horaFin,
        lugar: await buscarLugar(q, h.lugar),
      },
    );
  }
}

async function guardarActividad(req, id) {
  const b = req.body;
  const cupo = Number(b.cupo);
  const nombre = V.texto(b.nombre).trim();
  const descripcion = V.texto(b.descripcion).trim();
  revisar(V.revisarActividad(nombre, descripcion, cupo));
  const idDocente = Number(b.idDocente);

  await transaccion(async (q) => {
    const dias = new Set((await q('SELECT id FROM dias_semana')).map((f) => f.id));
    validarHorarios(b.horarios, dias);
    const docente = await q(
      "SELECT 1 FROM docentes d JOIN usuarios u ON u.id = d.id_usuario WHERE d.id_usuario = @id AND u.activo = 1",
      { id: idDocente },
    );
    if (docente.length === 0) throw new ErrorApi('Selecciona un docente válido.');

    let idActividad = id;
    if (id === null) {
      const periodo = await q('SELECT id FROM periodos WHERE activo = 1');
      if (periodo.length === 0) throw new ErrorApi('No hay un periodo activo.');
      const nueva = await q(
        `INSERT INTO actividades (nombre, descripcion, cupo, id_docente, id_periodo)
         OUTPUT INSERTED.id VALUES (@nombre, @descripcion, @cupo, @docente, @periodo)`,
        { nombre, descripcion: descripcion || null, cupo, docente: idDocente, periodo: periodo[0].id },
      );
      idActividad = nueva[0].id;
    } else {
      // bloquea la fila para que el cupo no cambie mientras se revisa
      const actual = await q('SELECT id FROM actividades WITH (UPDLOCK, HOLDLOCK) WHERE id = @id AND activa = 1', { id });
      if (actual.length === 0) throw new ErrorApi('Actividad no encontrada.', 404);
      const inscritos = (await q(
        "SELECT COUNT(*) AS n FROM inscripciones WHERE id_actividad = @id AND estado = 'activa'", { id },
      ))[0].n;
      if (cupo < inscritos) throw new ErrorApi(`El cupo no puede ser menor a los ${inscritos} alumnos inscritos.`);
      await q(
        'UPDATE actividades SET nombre = @nombre, descripcion = @descripcion, cupo = @cupo, id_docente = @docente WHERE id = @id',
        { id, nombre, descripcion: descripcion || null, cupo, docente: idDocente },
      );
    }
    await guardarHorarios(q, idActividad, b.horarios);
  });
}

router.post('/actividades', solo('admin'), ruta(async (req, res) => {
  await guardarActividad(req, null);
  res.status(201).json({ ok: true });
}));

router.put('/actividades/:id(\\d+)', solo('admin'), ruta(async (req, res) => {
  await guardarActividad(req, idDe(req.params.id));
  res.json({ ok: true });
}));

// baja logica para conservar historial
router.delete('/actividades/:id(\\d+)', solo('admin'), ruta(async (req, res) => {
  const filas = await consulta('UPDATE actividades SET activa = 0 WHERE id = @id', { id: idDe(req.params.id) });
  if (filas.afectadas === 0) throw new ErrorApi('Actividad no encontrada.', 404);
  res.json({ ok: true });
}));

router.post('/actividades/:id(\\d+)/inscripcion', solo('alumno'), ruta(async (req, res) => {
  const idActividad = idDe(req.params.id);
  const idAlumno = req.usuario.id;
  await transaccion(async (q) => {
    await exigirVigente(q, idActividad);
    // bloquea la actividad para respetar el cupo
    const act = await q(
      'SELECT cupo FROM actividades WITH (UPDLOCK, HOLDLOCK) WHERE id = @id AND activa = 1', { id: idActividad },
    );
    if (act.length === 0) throw new ErrorApi('Actividad no disponible.');

    const otra = await q(
      `SELECT a.nombre FROM inscripciones i JOIN actividades a ON a.id = i.id_actividad
        WHERE i.id_alumno = @alumno AND i.estado = 'activa' AND i.id_actividad <> @actividad`,
      { alumno: idAlumno, actividad: idActividad },
    );
    if (otra.length > 0) {
      throw new ErrorApi(`Ya estás inscrito en ${otra[0].nombre}. Da de baja esa actividad antes de inscribirte en otra.`);
    }

    const previa = await q(
      'SELECT id, estado FROM inscripciones WHERE id_alumno = @alumno AND id_actividad = @actividad',
      { alumno: idAlumno, actividad: idActividad },
    );
    if (previa.length > 0 && previa[0].estado === 'activa') throw new ErrorApi('Ya estás inscrito en esta actividad.');

    const inscritos = (await q(
      "SELECT COUNT(*) AS n FROM inscripciones WHERE id_actividad = @id AND estado = 'activa'", { id: idActividad },
    ))[0].n;
    if (inscritos >= act[0].cupo) throw new ErrorApi('Cupo lleno para esta actividad.');

    if (previa.length === 0) {
      await q('INSERT INTO inscripciones (id_alumno, id_actividad) VALUES (@alumno, @actividad)',
        { alumno: idAlumno, actividad: idActividad });
    } else {
      // si ya estuvo se reactiva
      await q(`UPDATE inscripciones SET estado = 'activa', fecha = ${AHORA_SQL} WHERE id = @id`, { id: previa[0].id });
    }
  });
  res.status(201).json({ ok: true });
}));

router.delete('/actividades/:id(\\d+)/inscripcion', solo('alumno'), ruta(async (req, res) => {
  const filas = await consulta(
    "UPDATE inscripciones SET estado = 'baja' WHERE id_alumno = @alumno AND id_actividad = @actividad AND estado = 'activa'",
    { alumno: req.usuario.id, actividad: idDe(req.params.id) },
  );
  if (filas.afectadas === 0) throw new ErrorApi('No estás inscrito en esa actividad.');
  res.json({ ok: true });
}));

router.get('/horario', solo('alumno'), ruta(async (req, res) => {
  const filas = await consulta(
    `SELECT a.id AS idActividad, a.nombre, ${CAMPOS_HORARIO}
       FROM inscripciones i
       JOIN actividades a ON a.id = i.id_actividad
       JOIN horarios h ON h.id_actividad = a.id
       JOIN dias_semana d ON d.id = h.id_dia
       JOIN lugares l ON l.id = h.id_lugar
      WHERE i.id_alumno = @alumno AND i.estado = 'activa' AND a.activa = 1
      ORDER BY d.orden, h.hora_inicio`,
    { alumno: req.usuario.id },
  );
  res.json(filas.map((f) => ({ idActividad: f.idActividad, nombre: f.nombre, dia: horarioJson(f) })));
}));

router.get('/constancias', solo('alumno'), ruta(async (req, res) => {
  const filas = await consulta(
    `SELECT a.id AS idActividad, a.nombre, p.nombre AS periodo,
            v.sesiones, v.asistencias, COALESCE(v.porcentaje, 0) AS porcentaje
       FROM v_asistencia_alumno v
       JOIN actividades a ON a.id = v.id_actividad
       JOIN v_periodos p ON p.id = a.id_periodo
      WHERE v.id_alumno = @alumno ORDER BY a.nombre`,
    { alumno: req.usuario.id },
  );
  res.json(filas.map((f) => ({
    idActividad: f.idActividad, nombre: f.nombre, periodo: f.periodo,
    sesiones: f.sesiones, asistencias: f.asistencias, porcentaje: Number(f.porcentaje),
    disponible: Number(f.porcentaje) >= MINIMO_ASISTENCIA,
  })));
}));

const nombreMes = (numero) => {
  let mes = parseInt(numero, 10);
  if (!(mes >= 1 && mes <= 12)) mes = 1;
  return MESES[mes - 1];
};

// por ejemplo AGOSTO 2026
const periodoOficial = (fecha) => {
  const partes = String(fecha).slice(0, 10).split('-');
  if (partes.length !== 3) return String(fecha);
  return `${nombreMes(partes[1])} ${partes[0]}`.toUpperCase();
};

// datos para armar el pdf en el telefono
router.post('/actividades/:id(\\d+)/constancia', solo('alumno'), ruta(async (req, res) => {
  const idActividad = idDe(req.params.id);
  const datos = await transaccion(async (q) => {
    const filas = await q(
      `SELECT v.id_inscripcion AS idInscripcion, COALESCE(v.porcentaje, 0) AS porcentaje,
              a.nombre AS actividad,
              CONVERT(VARCHAR(10), p.inicio, 23) AS inicio, CONVERT(VARCHAR(10), p.fin, 23) AS fin,
              ${NOMBRE_COMPLETO('u')} AS alumno, al.matricula, c.nombre AS carrera
         FROM v_asistencia_alumno v
         JOIN actividades a ON a.id = v.id_actividad
         JOIN periodos p ON p.id = a.id_periodo
         JOIN usuarios u ON u.id = v.id_alumno
         JOIN alumnos al ON al.id_usuario = v.id_alumno
         JOIN carreras c ON c.id = al.id_carrera
        WHERE v.id_alumno = @alumno AND v.id_actividad = @actividad`,
      { alumno: req.usuario.id, actividad: idActividad },
    );
    if (filas.length === 0) throw new ErrorApi('No estás inscrito en esa actividad.');
    const f = filas[0];
    const porcentaje = Number(f.porcentaje);
    if (porcentaje < MINIMO_ASISTENCIA) {
      throw new ErrorApi(`Se requiere 80% de asistencia (llevas ${porcentaje.toFixed(0)}%).`);
    }

    // el folio se guarda una vez
    const leerFolio = () => q(
      'SELECT folio, CONVERT(VARCHAR(19), emitida_en, 120) AS emitida FROM constancias WHERE id_inscripcion = @id',
      { id: f.idInscripcion },
    );
    let folio = await leerFolio();
    if (folio.length === 0) {
      const anio = hoy().slice(0, 4);
      await q('INSERT INTO constancias (id_inscripcion, folio) VALUES (@id, @folio)', {
        id: f.idInscripcion, folio: `ITSM-${anio}-${String(f.idInscripcion).padStart(5, '0')}`,
      });
      folio = await leerFolio();
    }
    if (folio.length === 0) throw new ErrorApi('No se pudo registrar la constancia.');

    const emision = folio[0].emitida.slice(0, 10).split('-');
    const folioLargo = folio[0].folio;
    return {
      alumno: f.alumno,
      matricula: f.matricula,
      carrera: f.carrera,
      actividad: f.actividad,
      periodo: `${periodoOficial(f.inicio)} - ${periodoOficial(f.fin)}`,
      nivel: porcentaje >= 95 ? 'SOBRESALIENTE' : 'NOTABLE',
      valor: porcentaje >= 95 ? '4' : '3',
      creditos: '1',
      folio: folioLargo.slice(folioLargo.lastIndexOf('-') + 1),
      dia: String(parseInt(emision[2], 10) || 1),
      mes: nombreMes(emision[1]),
      anio: emision[0],
      nombreArchivo: `constancia-${folioLargo}.pdf`,
    };
  });
  res.json(datos);
}));

router.get('/actividades/:id(\\d+)/alumnos', solo('docente', 'admin'), ruta(async (req, res) => {
  const idActividad = idDe(req.params.id);
  await exigirTitular(req, idActividad);
  const filas = await consulta(
    `SELECT u.id, ${NOMBRE_COMPLETO('u')} AS nombre, a.matricula
       FROM inscripciones i
       JOIN alumnos a ON a.id_usuario = i.id_alumno
       JOIN usuarios u ON u.id = a.id_usuario
      WHERE i.id_actividad = @actividad AND i.estado = 'activa'
      ORDER BY u.apellido_paterno, u.apellido_materno, u.nombres`,
    { actividad: idActividad },
  );
  res.json(filas);
}));

module.exports = router;
