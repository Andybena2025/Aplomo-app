'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const fuente = fs.readFileSync('src/main/resources/static/js/inicio-usuario.js', 'utf8');
function entorno() {
 const fichas = [{hidden: true}, {hidden: true}];
 const botones = fichas.map((f, i) => ({
  estado: 'false',
  getAttribute(n) { return n === 'aria-controls' ? String(i) : this.estado; },
  setAttribute(n, v) { this.estado = v; },
  addEventListener(_, f) { this.clic = f; }
 }));
 vm.runInNewContext(fuente, {document: {
  querySelector: () => ({querySelectorAll: () => botones}),
  getElementById: id => fichas[Number(id)]
 }});
 return {fichas, botones};
}
test('un nombre abre su ficha en línea y un segundo clic la cierra', () => {
 const e = entorno();
 e.botones[0].clic();
 assert.equal(e.fichas[0].hidden, false);
 assert.equal(e.botones[0].estado, 'true');
 e.botones[0].clic();
 assert.equal(e.fichas[0].hidden, true);
 assert.equal(e.botones[0].estado, 'false');
});
test('solo una ficha puede estar abierta, sin ocultar la tarjeta ni usar sessionStorage', () => {
 const e = entorno();
 e.botones[0].clic();
 e.botones[1].clic();
 assert.equal(e.fichas[0].hidden, true);
 assert.equal(e.fichas[1].hidden, false);
 assert.equal(e.botones[0].estado, 'false');
 assert.equal(e.botones[1].estado, 'true');
});
