const test = require('node:test');
const assert = require('node:assert/strict');
const {procesar, montar} = require('../src/main/resources/static/js/notas.js');
test('la supresión es solo de esta página y usa el ID, no el texto ni sessionStorage', () => {
  const vistos = new Set(), avisos = [];
  procesar([{id:1,texto:'Igual'}], vistos, n => avisos.push(n));
  procesar([{id:1,texto:'Igual'},{id:2,texto:'Igual'}], vistos, n => avisos.push(n));
  assert.equal(avisos.length, 2);
  procesar([{id:1,texto:'Igual'}], new Set(), n => avisos.push(n));
  assert.equal(avisos.length, 3);
});
test('si no se muestra, no se suprime el reintento', () => {
  const vistos = new Set();
  assert.throws(() => procesar([{id:7}], vistos, () => {throw new Error('sin DOM');}));
  assert.equal(vistos.size, 0);
});
test('una pestaña oculta no consulta ni prolonga la sesión', async () => {
  let consultas = 0;
  const doc = {hidden:true, querySelector:()=>({}), addEventListener:()=>{}};
  await montar(doc, async () => {consultas++;})();
  assert.equal(consultas, 0);
});
test('sin sesión no se instala el sondeo', () => {
  assert.equal(montar({querySelector:()=>null}, null), null);
});
test('un fallo de red deja reintentar y un 401 se muestra sin acuse', async () => {
  const contador = {};
  const doc = {hidden:false, addEventListener:()=>{},
    querySelector: s => s.includes('contador') ? contador : {}};
  let llamadas = 0;
  const consultar = montar(doc, async () => {
    if (++llamadas === 1) throw new Error('sin red');
    return {status:401};
  });
  await consultar(); await consultar();
  assert.equal(llamadas, 2);
  assert.equal(contador.textContent, '!');
});
