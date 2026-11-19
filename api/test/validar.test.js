const test = require('node:test');
const assert = require('node:assert');
const V = require('../src/validar');

test('correo del alumno debe ser su numero de control', () => {
  assert.strictEqual(V.revisarCorreoAlumno('232t0182@itsm.edu.mx', '232T0182'), null);
  assert.match(V.revisarCorreoAlumno('otro@itsm.edu.mx', '232T0182'), /número de control/);
  assert.match(V.revisarCorreo('a@gmail.com'), /@itsm.edu.mx/);
});

test('contraseña de 8 a 30 caracteres sin espacios', () => {
  assert.strictEqual(V.revisarClave('itsm1234', false), null);
  assert.match(V.revisarClave('corta', false), /8 a 30/);
  assert.match(V.revisarClave('con espacio 123', false), /espacios/);
  assert.strictEqual(V.revisarClave('', true), null);
});

test('fechas reales y no futuras', () => {
  assert.strictEqual(V.revisarFecha('2026-02-30') !== null, true);
  assert.match(V.revisarFecha('2999-01-01'), /posterior a hoy/);
  assert.strictEqual(V.revisarFecha('2026-09-07'), null);
});

test('actividad: nombre, descripcion y cupo', () => {
  assert.strictEqual(V.revisarActividad('Fútbol', '', 25), null);
  assert.match(V.revisarActividad('ab', '', 25), /3 a 50/);
  assert.match(V.revisarActividad('Fútbol', '', 0), /cupo/);
  assert.match(V.revisarActividad('Fútbol', '', 1.5), /cupo/);
});

test('hash de contraseña se verifica y no se repite', () => {
  process.env.JWT_SECRET = 'x'.repeat(40);
  const { generarHash, verificarHash } = require('../src/auth');
  const a = generarHash('itsm1234');
  assert.ok(verificarHash('itsm1234', a));
  assert.ok(!verificarHash('otra-clave', a));
  assert.notStrictEqual(a, generarHash('itsm1234'));
  assert.ok(!verificarHash('itsm1234', 'basura'));
});

test('semestre: fechas reales, fin despues de inicio y fin no vencido', () => {
  const hoy = '2026-11-19';
  assert.strictEqual(V.revisarSemestre('2027-01-12', '2027-06-04', hoy), null);
  assert.match(V.revisarSemestre('2027-06-04', '2027-01-12', hoy), /posterior a la de inicio/);
  assert.match(V.revisarSemestre('2027-01-12', '2027-01-12', hoy), /posterior a la de inicio/);
  assert.match(V.revisarSemestre('2026-08-17', '2026-11-18', hoy), /anterior a hoy/);
  assert.strictEqual(V.revisarSemestre('2026-08-17', '2026-11-19', hoy), null);
  assert.match(V.revisarSemestre('2026-13-01', '2027-06-04', hoy), /inicio/);
  assert.match(V.revisarSemestre('2027-01-12', 'mañana', hoy), /fin/);
  assert.match(V.revisarSemestre('2026-01-01', '2027-06-01', '2026-01-01'), /más de un año/);
});

test('fecha legible del semestre', () => {
  const { legible } = require('../src/semestre');
  assert.strictEqual(legible('2026-08-17'), '17/08/2026');
});
