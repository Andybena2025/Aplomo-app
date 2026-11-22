'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const fuente = fs.readFileSync('src/main/resources/static/js/sistema-visual.js', 'utf8');
function nodo(textContent = '', dataset = {}) {
  return {textContent, dataset, hidden: false, eventos: {}, atributos: {},
    addEventListener(tipo, accion) { this.eventos[tipo] = accion; },
    setAttribute(nombre, valor) { this.atributos[nombre] = valor; }};
}
function entorno(seccion, datos, busqueda = '') {
  const filas = datos.map(dato => nodo(dato.texto || '', dato));
  const opciones = seccion === 'RESERVAS' ? ['PROXIMAS','EN_CURSO','FINALIZADA','CANCELADA','TODOS']
    : seccion === 'USUARIOS' ? ['ACTIVA','INACTIVA','TODOS'] : ['ABIERTOS','ABIERTO','ARREGLADO','TODOS'];
  const chips = opciones.map(valor => nodo(valor + ' · 0', {listaFiltro: valor}));
  const buscar = nodo(); buscar.value = '';
  const contador = nodo(), vacia = nodo(), mas = nodo();
  const lista = {dataset: {seccion}, addEventListener() {},
    querySelectorAll(selector) { return selector === '[data-registro]' ? filas :
      selector === '[data-lista-filtro]' ? chips : []; },
    querySelector(selector) { return {'[data-lista-buscar]': buscar, '[data-lista-contador]': contador,
      '[data-lista-vacia]': vacia, '[data-lista-mas]': mas}[selector] || null; }};
  class FechaFija extends Date { constructor(...args) { super(...(args.length ? args : ['2026-11-27T15:00:00Z'])); } }
  vm.runInNewContext(fuente, {document: {querySelectorAll: selector => selector === '[data-lista]' ? [lista] : []},
    location: {search: busqueda}, URLSearchParams, Intl, Date: FechaFija, setTimeout() {}});
  return {filas, chips, buscar, contador, vacia, mas,
    elegir(valor) { chips.find(chip => chip.dataset.listaFiltro === valor).eventos.click(); }};
}
test('Próximas excluye citas pasadas, finalizadas y canceladas; el contador cuenta lo visible', () => {
  const e = entorno('RESERVAS', [{estado:'EN_CURSO',fecha:'2026-11-27'},
    {estado:'EN_COLA',fecha:'2026-11-26'}, {estado:'FINALIZADA',fecha:'2026-11-27'},
    {estado:'CANCELADA',fecha:'2026-11-28'}]);
  assert.deepEqual(e.filas.map(fila => fila.hidden), [false,true,true,true]);
  assert.equal(e.contador.textContent, '1 resultado');
});
test('los chips cambian la lista sin quitar filas para búsquedas posteriores', () => {
  const e = entorno('RESERVAS', [{estado:'EN_CURSO',fecha:'2026-11-27'}, {estado:'FINALIZADA',fecha:'2026-11-27'}]);
  e.elegir('FINALIZADA'); assert.deepEqual(e.filas.map(f => f.hidden), [true,false]);
  e.elegir('TODOS'); assert.deepEqual(e.filas.map(f => f.hidden), [false,false]);
  assert.equal(e.contador.textContent, '2 resultados');
});
test('búsqueda en vivo ignora tildes y actualiza chip, contador y vacío', () => {
  const e = entorno('USUARIOS', [{estado:'ACTIVA',busqueda:'Ing. Andrés'}, {estado:'ACTIVA',busqueda:'Johan'}]);
  e.buscar.value = 'ANDRES'; e.buscar.eventos.input();
  assert.deepEqual(e.filas.map(f => f.hidden), [false,true]);
  assert.equal(e.chips[0].textContent, 'ACTIVA · 1');
  e.buscar.value = 'no existe'; e.buscar.eventos.input();
  assert.equal(e.contador.textContent, '0 resultados'); assert.equal(e.vacia.hidden, false);
});
test('cuentas bloqueadas permanecen en Activas y las inactivas se muestran al elegirlas', () => {
  const e = entorno('USUARIOS', [{estado:'BLOQUEADA'}, {estado:'INACTIVA'}]);
  assert.deepEqual(e.filas.map(f => f.hidden), [false,true]);
  e.elegir('INACTIVA'); assert.deepEqual(e.filas.map(f => f.hidden), [true,false]);
});
test('la dirección conserva el filtro inicial y la búsqueda', () => {
  const e = entorno('RESERVAS', [{estado:'EN_CURSO',texto:'Humboldt'}, {estado:'FINALIZADA',texto:'Humboldt'}],
    '?filtro_lista=FINALIZADA&q=humboldt');
  assert.deepEqual(e.filas.map(f => f.hidden), [true,false]); assert.equal(e.buscar.value, 'humboldt');
});
test('una fila que no cumple el filtro adicional no reaparece al cambiar chips', () => {
  const e = entorno('USUARIOS', [{estado:'ACTIVA',filtroExtra:'false'}, {estado:'ACTIVA',filtroExtra:'true'}]);
  e.elegir('TODOS'); assert.deepEqual(e.filas.map(f => f.hidden), [true,false]);
});

test('el contador de un chip no se duplica al alternar y buscar', () => {
  const e = entorno('USUARIOS', [{estado:'ACTIVA',busqueda:'Andrés'}, {estado:'INACTIVA',busqueda:'Johan'}]);
  e.elegir('TODOS'); e.elegir('ACTIVA'); e.elegir('TODOS');
  assert.equal(e.chips[2].textContent, 'TODOS · 2');
  e.buscar.value = 'andres'; e.buscar.eventos.input();
  assert.equal(e.chips[2].textContent, 'TODOS · 1');
});

test('50 registros: abre 10, muestra 20 al pedir más y reinicia a 10 al filtrar', () => {
  const e = entorno('USUARIOS', Array.from({length:50}, (_,i)=>({estado:'ACTIVA',busqueda:'Persona '+i})));
  assert.equal(e.filas.filter(f=>!f.hidden).length,10);
  assert.equal(e.contador.textContent,'50 resultados');
  assert.equal(e.mas.textContent,'Ver 10 más · quedan 40');
  e.mas.eventos.click();
  assert.equal(e.filas.filter(f=>!f.hidden).length,20);
  e.elegir('TODOS');
  assert.equal(e.filas.filter(f=>!f.hidden).length,10);
  assert.equal(e.chips[2].textContent,'TODOS · 50');
});
test('Próximas excluye reservas a más de 14 días', () => {
  const e=entorno('RESERVAS',[{estado:'EN_COLA',fecha:'2026-12-11'},
    {estado:'EN_COLA',fecha:'2026-12-12'}]);
  assert.deepEqual(e.filas.map(f=>f.hidden),[false,true]);
});

test('ningún selector de chip de filtro declara más de 32 px de alto mínimo', () => {
  const css=fs.readFileSync('src/main/resources/static/css/aplomo.css','utf8');
  for(const regla of css.matchAll(/([^{}]+)\{([^{}]+)\}/g)) {
    if(!/(?:^|[^\w-])\.chip(?![\w-])/.test(regla[1])) continue;
    for(const altura of regla[2].matchAll(/min-height\s*:\s*(\d+)px/g))
      assert.ok(+altura[1]<=32,regla[1]);
  }
});

test('la cita destacada cuenta en los chips pero no se repite entre las filas', () => {
  const e=entorno('RESERVAS',[{estado:'EN_COLA',fecha:'2026-11-27',destacada:'true'},
    {estado:'EN_COLA',fecha:'2026-11-28'}]);
  assert.deepEqual(e.filas.map(f=>f.hidden),[true,false]);
  assert.equal(e.contador.textContent,'2 resultados');
  assert.equal(e.chips[0].textContent,'PROXIMAS · 2');
});

test('permite buscar el nombre corto visible y también el nombre completo', () => {
  const e=entorno('USUARIOS',[{estado:'ACTIVA',busqueda:'Ing. Laura Gabriella Romero',texto:'Laura Romero'}]);
  for(const nombre of ['Laura Romero','Laura Gabriella Romero']) {
    e.buscar.value=nombre;e.buscar.eventos.input();assert.equal(e.filas[0].hidden,false);
  }
});
