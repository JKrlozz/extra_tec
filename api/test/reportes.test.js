const test = require('node:test');
const assert = require('node:assert');
const { leerFiltros, patron, y } = require('../src/reportes_filtros');

test('sin filtros se usa todo el historial', () => {
  const f = leerFiltros({ tipo: 'alumno' });
  assert.deepStrictEqual(f.p, { i: '1000-01-01', f: '9999-12-31' });
  assert.deepStrictEqual(f.alumno, []);
  assert.deepStrictEqual(f.maestro, []);
  assert.strictEqual(f.especifico, false);
});

test('una sola fecha especifica funciona', () => {
  const f = leerFiltros({ desde: '2026-03-10', hasta: '2026-03-10' });
  assert.strictEqual(f.p.i, '2026-03-10');
  assert.strictEqual(f.p.f, '2026-03-10');
});

test('fechas invalidas o al reves regresan error', () => {
  assert.throws(() => leerFiltros({ desde: '2026-13-01' }), /AAAA-MM-DD/);
  assert.throws(() => leerFiltros({ desde: '2026-03-02', hasta: '2026-03-01' }), /posterior/);
});

test('filtros de alumno usan parametros', () => {
  const f = leerFiltros({ matricula: '232t01', nombre: 'Ana', carrera: '2', semestre: '3', actividad: '7' });
  assert.strictEqual(f.p.matricula, '%232T01%');
  assert.strictEqual(f.p.nombre, '%Ana%');
  assert.strictEqual(f.p.carrera, 2);
  assert.strictEqual(f.p.semestre, 3);
  assert.strictEqual(f.p.actividad, 7);
  assert.strictEqual(f.alumno.length, 4);
  assert.strictEqual(f.especifico, true);
  assert.ok(!y(f.alumno).includes('Ana'));
});

test('enteros y textos fuera de rango regresan error', () => {
  assert.throws(() => leerFiltros({ semestre: '9' }), /entre 1 y 8/);
  assert.throws(() => leerFiltros({ semestre: 'dos' }), /entre 1 y 8/);
  assert.throws(() => leerFiltros({ carrera: '-1' }), /carrera/);
  assert.throws(() => leerFiltros({ actividad: '1.5' }), /actividad/);
  assert.throws(() => leerFiltros({ nombre: 'a'.repeat(101) }), /100 caracteres/);
});

test('filtros de docente por nombre o correo y actividad', () => {
  const f = leerFiltros({ docente: 'perez@', actividad: '4' });
  assert.strictEqual(f.p.docente, '%perez@%');
  assert.strictEqual(f.maestro.length, 2);
  assert.match(f.maestro[0], /u\.correo LIKE @docente/);
});

test('los comodines del LIKE se escapan', () => {
  assert.strictEqual(patron('50%_[x'), '%50\\%\\_\\[x%');
});
