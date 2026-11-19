// evidencias con fotografias
const express = require('express');
const multer = require('multer');
const { consulta, transaccion } = require('../db');
const { solo } = require('../auth');
const { exigirTitular } = require('../acceso');
const { ErrorApi, ruta, revisar, idDe, AHORA_SQL } = require('../util');
const V = require('../validar');
const { exigirVigente } = require('../semestre');

const router = express.Router();
const permiso = solo('docente', 'admin');
const ESTADOS = ['realizada', 'suspendida', 'no_realizada'];

const subir = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: V.L.FOTO_BYTES_MAX, files: V.L.FOTOS_MAX, fields: 5, fieldSize: 64 * 1024 },
}).array('fotos', V.L.FOTOS_MAX);

// el tipo sale de los bytes, no de lo que diga el telefono
function tipoDe(b) {
  if (b.length > 3 && b[0] === 0xff && b[1] === 0xd8 && b[2] === 0xff) return 'image/jpeg';
  if (b.length > 8 && b[0] === 0x89 && b.toString('latin1', 1, 4) === 'PNG') return 'image/png';
  if (b.length > 12 && b.toString('latin1', 0, 4) === 'RIFF' && b.toString('latin1', 8, 12) === 'WEBP') return 'image/webp';
  return null;
}

const CAMPOS = `e.id, e.id_actividad AS idActividad, CONVERT(VARCHAR(10), e.fecha, 23) AS fecha,
       CONVERT(VARCHAR(19), e.creada_en, 120) AS creadaEn,
       CONVERT(VARCHAR(19), e.actualizada_en, 120) AS actualizadaEn,
       e.estado, COALESCE(e.descripcion, '') AS descripcion`;

async function conFotos(filas) {
  if (filas.length === 0) return [];
  // los ids vienen de la base
  const fotos = await consulta(
    `SELECT id, id_evidencia AS idEvidencia FROM evidencia_fotos
      WHERE id_evidencia IN (${filas.map((f) => Number(f.id)).join(',')}) ORDER BY orden, id`,
  );
  return filas.map((f) => ({
    id: f.id, idActividad: f.idActividad, fecha: f.fecha, creadaEn: f.creadaEn,
    actualizadaEn: f.actualizadaEn || null, estado: f.estado, descripcion: f.descripcion,
    fotos: fotos.filter((x) => x.idEvidencia === f.id).map((x) => ({ id: x.id })),
  }));
}

router.get('/actividades/:id(\\d+)/evidencias', permiso, ruta(async (req, res) => {
  const idActividad = idDe(req.params.id);
  await exigirTitular(req, idActividad);
  const filas = await consulta(
    `SELECT ${CAMPOS} FROM evidencias e WHERE e.id_actividad = @id ORDER BY e.fecha DESC, e.creada_en DESC, e.id DESC`,
    { id: idActividad },
  );
  res.json(await conFotos(filas));
}));

router.get('/evidencias/:id(\\d+)', permiso, ruta(async (req, res) => {
  const filas = await consulta(`SELECT ${CAMPOS} FROM evidencias e WHERE e.id = @id`, { id: idDe(req.params.id) });
  if (filas.length === 0) throw new ErrorApi('Evidencia no encontrada.', 404);
  await exigirTitular(req, filas[0].idActividad);
  res.json((await conFotos(filas))[0]);
}));

// crea o cambia una evidencia, guarda y borra fotos
async function guardar(req, idActividad, idEvidencia) {
  let datos;
  try {
    datos = JSON.parse(req.body.datos || '{}');
  } catch (e) {
    throw new ErrorApi('Los datos de la evidencia no son válidos.');
  }
  const fecha = V.texto(datos.fecha);
  const estado = V.texto(datos.estado);
  const descripcion = V.texto(datos.descripcion).trim();
  const quitar = Array.isArray(datos.quitar) ? datos.quitar.map(Number) : [];
  const nuevas = req.files || [];

  await transaccion(async (q) => {
    const titular = await exigirTitular(req, idActividad, q);
    await exigirVigente(q, idActividad);
    revisar(V.revisarFecha(fecha));
    revisar(V.revisarDescripcionEvidencia(descripcion));
    if (!ESTADOS.includes(estado)) throw new ErrorApi('Selecciona un estado válido para la evidencia.');
    for (const foto of nuevas) {
      if (foto.size === 0) throw new ErrorApi('Falta una fotografía.');
      foto.tipo = tipoDe(foto.buffer);
      if (!foto.tipo) throw new ErrorApi('Solo se aceptan imágenes JPG, PNG o WebP.');
    }

    let existentes = [];
    if (idEvidencia !== null) {
      const pertenece = await q('SELECT 1 AS x FROM evidencias WHERE id = @id AND id_actividad = @actividad',
        { id: idEvidencia, actividad: idActividad });
      if (pertenece.length === 0) throw new ErrorApi('Evidencia no encontrada.', 404);
      existentes = (await q('SELECT id FROM evidencia_fotos WHERE id_evidencia = @id', { id: idEvidencia })).map((f) => f.id);
    }

    // cuantas fotos quedan al final
    const aQuitar = existentes.filter((id) => quitar.includes(id));
    const quedan = existentes.length - aQuitar.length + nuevas.length;
    if (estado === 'realizada' && quedan === 0) {
      throw new ErrorApi('Selecciona al menos una fotografía para una actividad realizada.');
    }
    if (quedan > V.L.FOTOS_MAX) throw new ErrorApi(`Puedes guardar hasta ${V.L.FOTOS_MAX} fotografías por evidencia.`);

    let id = idEvidencia;
    if (id === null) {
      const nueva = await q(
        `INSERT INTO evidencias (id_actividad, id_docente, fecha, estado, descripcion)
         OUTPUT INSERTED.id VALUES (@actividad, @docente, @fecha, @estado, @descripcion)`,
        { actividad: idActividad, docente: titular, fecha, estado, descripcion: descripcion || null },
      );
      id = nueva[0].id;
    } else {
      await q(
        `UPDATE evidencias SET fecha = @fecha, estado = @estado, descripcion = @descripcion,
                actualizada_en = ${AHORA_SQL} WHERE id = @id`,
        { id, fecha, estado, descripcion: descripcion || null },
      );
    }

    for (const idFoto of aQuitar) {
      await q('DELETE FROM evidencia_fotos WHERE id = @id', { id: idFoto });
    }

    // las fotos nuevas van al final
    let orden = (await q('SELECT COALESCE(MAX(orden), -1) + 1 AS n FROM evidencia_fotos WHERE id_evidencia = @id', { id }))[0].n;
    for (const foto of nuevas) {
      await q(
        'INSERT INTO evidencia_fotos (id_evidencia, tipo, contenido, orden) VALUES (@evidencia, @tipo, @contenido, @orden)',
        { evidencia: id, tipo: foto.tipo, contenido: foto.buffer, orden },
      );
      orden += 1;
    }
  });
}

router.post('/actividades/:id(\\d+)/evidencias', permiso, subir, ruta(async (req, res) => {
  await guardar(req, idDe(req.params.id), null);
  res.status(201).json({ ok: true });
}));

router.put('/evidencias/:id(\\d+)', permiso, subir, ruta(async (req, res) => {
  const id = idDe(req.params.id);
  const filas = await consulta('SELECT id_actividad FROM evidencias WHERE id = @id', { id });
  if (filas.length === 0) throw new ErrorApi('Evidencia no encontrada.', 404);
  await guardar(req, filas[0].id_actividad, id);
  res.json({ ok: true });
}));

router.delete('/evidencias/:id(\\d+)', permiso, ruta(async (req, res) => {
  const id = idDe(req.params.id);
  const filas = await consulta('SELECT id_actividad FROM evidencias WHERE id = @id', { id });
  if (filas.length === 0) throw new ErrorApi('Evidencia no encontrada.', 404);
  await exigirTitular(req, filas[0].id_actividad);
  await consulta('DELETE FROM evidencias WHERE id = @id', { id });
  res.json({ ok: true });
}));

// la imagen se pide aparte para no cargar todo junto
router.get('/fotos/:id(\\d+)', permiso, ruta(async (req, res) => {
  const filas = await consulta(
    `SELECT f.tipo, f.contenido, e.id_actividad AS idActividad
       FROM evidencia_fotos f JOIN evidencias e ON e.id = f.id_evidencia WHERE f.id = @id`,
    { id: idDe(req.params.id) },
  );
  if (filas.length === 0) throw new ErrorApi('Fotografía no encontrada.', 404);
  await exigirTitular(req, filas[0].idActividad);
  res.set('Cache-Control', 'private, max-age=86400');
  res.type(filas[0].tipo).send(filas[0].contenido);
}));

module.exports = router;
