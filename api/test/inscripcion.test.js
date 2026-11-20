const test = require('node:test');
const assert = require('node:assert');
const { mensajesInscripcion } = require('../src/routes/actividades');

test('mensajes de inscripcion para el alumno', () => {
  const m = mensajesInscripcion(false);
  assert.strictEqual(m.yaInscrito, 'Ya estás inscrito en esta actividad.');
  assert.match(m.otra('Ajedrez'), /^Ya estás inscrito en Ajedrez\. Da de baja/);
});

test('mensajes de inscripcion para el administrador', () => {
  const m = mensajesInscripcion(true);
  assert.strictEqual(m.yaInscrito, 'El alumno ya está inscrito en esta actividad.');
  assert.strictEqual(m.otra('Ajedrez'), 'El alumno ya está inscrito en Ajedrez.');
});
