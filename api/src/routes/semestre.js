// fechas del semestre y semestres archivados
const express = require('express');
const { consulta } = require('../db');
const { solo } = require('../auth');
const { ruta, revisar, hoy } = require('../util');
const V = require('../validar');
const semestre = require('../semestre');

const router = express.Router();

async function estado() {
  const filasActivo = await consulta(
    `SELECT id, nombre, CONVERT(VARCHAR(10), inicio, 23) AS inicio, CONVERT(VARCHAR(10), fin, 23) AS fin
       FROM v_periodos WHERE activo = 1`,
  );
  let activo = null;
  if (filasActivo.length > 0) {
    const f = filasActivo[0];
    activo = { id: f.id, nombre: f.nombre, inicio: f.inicio, fin: f.fin };
  }
  const archivados = await consulta(
    `SELECT p.id, p.nombre, CONVERT(VARCHAR(10), p.inicio, 23) AS inicio, CONVERT(VARCHAR(10), p.fin, 23) AS fin,
            CONVERT(VARCHAR(10), pe.cerrado_en, 23) AS cerrado,
            (SELECT COUNT(*) FROM actividades a WHERE a.id_periodo = p.id AND a.archivada_en IS NOT NULL) AS actividades,
            (SELECT COUNT(*) FROM inscripciones i JOIN actividades a ON a.id = i.id_actividad
              WHERE a.id_periodo = p.id AND i.estado = 'archivada') AS inscripciones,
            (SELECT COUNT(*) FROM sesiones s JOIN actividades a ON a.id = s.id_actividad
              WHERE a.id_periodo = p.id) AS sesiones,
            (SELECT COUNT(*) FROM evidencias e JOIN actividades a ON a.id = e.id_actividad
              WHERE a.id_periodo = p.id) AS evidencias
       FROM periodos pe JOIN v_periodos p ON p.id = pe.id
      WHERE pe.activo = 0 ORDER BY pe.id DESC`,
  );
  const lista = [];
  for (const f of archivados) {
    lista.push({
      id: f.id, nombre: f.nombre, inicio: f.inicio, fin: f.fin, cerrado: f.cerrado || '',
      actividades: f.actividades, inscripciones: f.inscripciones, sesiones: f.sesiones, evidencias: f.evidencias,
    });
  }
  return {
    hoy: hoy(),
    activo: activo,
    archivados: lista,
  };
}

router.get('/semestre', solo('admin'), ruta(async (req, res) => {
  await semestre.archivarVencido(true);
  res.json(await estado());
}));

// cambia fechas o inicia un semestre
router.put('/semestre', solo('admin'), ruta(async (req, res) => {
  const inicio = V.texto(req.body.inicio).trim();
  const fin = V.texto(req.body.fin).trim();
  revisar(V.revisarSemestre(inicio, fin));
  await semestre.archivarVencido(true);
  const activo = await semestre.periodoActivo();
  let copiadas = 0;
  if (activo) {
    await consulta('UPDATE periodos SET inicio = @inicio, fin = @fin WHERE id = @id', { inicio, fin, id: activo.id });
  } else {
    copiadas = await semestre.iniciar(inicio, fin);
  }
  const respuesta = await estado();
  respuesta.copiadas = copiadas;
  res.json(respuesta);
}));

// termina el semestre antes de tiempo
router.post('/semestre/cerrar', solo('admin'), ruta(async (req, res) => {
  await semestre.cerrarActivo();
  res.json(await estado());
}));

module.exports = router;
