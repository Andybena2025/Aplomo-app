'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {fechaISO, horaISO, salta} = require('../src/main/resources/static/js/tiempo-colombia.js');

test('fecha colombiana mantiene el contrato ISO y el orden día/mes', () => {
  assert.equal(fechaISO('27', '11', '2026'), '2026-11-27');
  assert.equal(fechaISO('4', '2', '2026'), '2026-02-04');
});
test('calendario rechaza fechas inexistentes e incompletas', () => {
  for (const partes of [['31','4','2026'], ['29','2','2026'], ['0','1','2026'],
    ['1','13','2026'], ['', '2', '2026'], ['1', '1', '26']]) {
    assert.equal(fechaISO(...partes), '');
  }
  assert.equal(fechaISO('29', '2', '2028'), '2028-02-29');
});
test('hora colombiana conserva minutos y añade cero inicial', () => {
  assert.equal(horaISO('9', '0'), '09:00');
  assert.equal(horaISO('23', '59'), '23:59');
  assert.equal(horaISO('0', '30'), '00:30');
});
test('hora incompleta o fuera del reloj no se envía', () => {
  for (const partes of [['24','0'], ['9','60'], ['-1','30'], ['abc','30'], ['9','']]) {
    assert.equal(horaISO(...partes), '');
  }
});
test('salto de día espera un segundo dígito cuando cabe', () => {
  for (const valor of ['0', '1', '2', '3']) assert.equal(salta(valor, 'dia'), false);
  for (const valor of ['4', '9', '27']) assert.equal(salta(valor, 'dia'), true);
});
test('salto de mes y hora admite escribir 9 como 09', () => {
  assert.equal(salta('1', 'mes'), false);
  assert.equal(salta('2', 'mes'), true);
  assert.equal(salta('9', 'hora'), true);
  assert.equal(salta('2', 'hora'), false);
  assert.equal(salta('2026', 'anio'), false);
});
