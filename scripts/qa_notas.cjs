const test = require('node:test');
const assert = require('node:assert/strict');
const {procesar, montar} = require('../src/main/resources/static/js/notas.js');
test('nota aparece una sola vez por sesión y otro texto vuelve a avisar', () => {
  const memoria = new Map();
  const almacenamiento = {getItem: clave => memoria.get(clave), setItem: (clave, valor) => memoria.set(clave, valor)};
  const avisos = [];
  const nota = {reserva: 12, texto: 'Revisar los apoyos'};
  procesar([nota], almacenamiento, valor => avisos.push(valor));
  procesar([nota], almacenamiento, valor => avisos.push(valor));
  procesar([{...nota, texto: 'Nota nueva'}], almacenamiento, valor => avisos.push(valor));
  assert.equal(avisos.length, 2);
});
test('una pestaña oculta no hace consultas', async () => {
  let consultas = 0;
  const consultar = montar({hidden: true, querySelector: () => ({dataset: {notasCsrf: 'csrf'}})},
    async () => {consultas++;}, null);
  await consultar();
  assert.equal(consultas, 0);
});
test('sin sesión de Evaluador no se instala el sondeo', () => {
  assert.equal(montar({querySelector: () => null}, null, null), null);
});
