const router = require('express').Router();
const PDFDocument = require('pdfkit');
const db = require('../db');
const { wrap, HttpError, auth } = require('../util');

const minimo = () => Number(process.env.ASISTENCIA_MINIMA || 80);

router.get('/constancias', auth('alumno'), wrap(async (req, res) => {
  const [rows] = await db.query(
    `SELECT v.id_inscripcion, a.id AS id_actividad, a.nombre, p.nombre AS periodo,
            v.sesiones, v.asistencias, COALESCE(v.porcentaje, 0) AS porcentaje
       FROM v_asistencia_alumno v
       JOIN actividades a ON a.id = v.id_actividad
       JOIN v_periodos p ON p.id = a.id_periodo
      WHERE v.id_alumno = ? ORDER BY a.nombre`, [req.user.id]);
  res.json(rows.map((r) => ({
    ...r,
    sesiones: Number(r.sesiones),
    asistencias: Number(r.asistencias),
    porcentaje: Number(r.porcentaje),
    disponible: Number(r.porcentaje) >= minimo(),
  })));
}));

router.get('/constancias/:idActividad/pdf', auth('alumno'), wrap(async (req, res) => {
  const [[d]] = await db.query(
    `SELECT v.id_inscripcion, COALESCE(v.porcentaje, 0) AS porcentaje, a.nombre AS actividad, p.nombre AS periodo,
            u.nombre_completo AS alumno, al.matricula, c.nombre AS carrera
       FROM v_asistencia_alumno v
       JOIN actividades a ON a.id = v.id_actividad
       JOIN v_periodos p ON p.id = a.id_periodo
       JOIN usuarios u ON u.id = v.id_alumno
       JOIN alumnos al ON al.id_usuario = v.id_alumno
       JOIN carreras c ON c.id = al.id_carrera
      WHERE v.id_alumno = ? AND v.id_actividad = ?`, [req.user.id, req.params.idActividad]);
  if (!d) throw new HttpError(404, 'No estás inscrito en esa actividad');
  if (Number(d.porcentaje) < minimo()) {
    throw new HttpError(403, `Se requiere ${minimo()}% de asistencia (llevas ${d.porcentaje}%)`);
  }

  let [[c]] = await db.query('SELECT folio, emitida_en FROM constancias WHERE id_inscripcion = ?', [d.id_inscripcion]);
  if (!c) {
    const folio = `ITSM-${new Date().getFullYear()}-${String(d.id_inscripcion).padStart(5, '0')}`;
    await db.query('INSERT INTO constancias (id_inscripcion, folio) VALUES (?,?)',
      [d.id_inscripcion, folio]);
    [[c]] = await db.query('SELECT folio, emitida_en FROM constancias WHERE id_inscripcion = ?', [d.id_inscripcion]);
  }

  res.setHeader('Content-Type', 'application/pdf');
  res.setHeader('Content-Disposition', `attachment; filename="constancia-${c.folio}.pdf"`);
  const doc = new PDFDocument({ size: 'LETTER', margin: 72 });
  doc.pipe(res);
  doc.fillColor('#1B396A').fontSize(20).text('Instituto Tecnológico Superior de Misantla', { align: 'center' });
  doc.moveDown(0.5).fontSize(14).fillColor('#C9A227').text('Actividades Extraescolares', { align: 'center' });
  doc.moveDown(2).fontSize(26).fillColor('#12284B').text('CONSTANCIA', { align: 'center' });
  doc.moveDown(2).fontSize(13).fillColor('#000').text(
    `Se hace constar que ${d.alumno}, con matrícula ${d.matricula}, de la carrera de ${d.carrera}, ` +
    `cumplió satisfactoriamente la actividad extraescolar "${d.actividad}" durante el periodo ${d.periodo}, ` +
    `con un porcentaje de asistencia de ${Number(d.porcentaje).toFixed(0)}%.`, { align: 'justify' });
  doc.moveDown(3).fontSize(10).fillColor('#555').text(`Folio: ${c.folio}`, { align: 'right' });
  doc.text(`Fecha de emisión: ${String(c.emitida_en).slice(0, 10)}`, { align: 'right' });
  doc.end();
}));

module.exports = router;
