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

test('el aviso se abre localmente y Entendido actualiza servidor, burbuja y contador sin navegar', async () => {
  const elementos = new Map(), eventos = {}, peticiones = [];
  const nodo = nombre => ({nombre, dataset:{}, hidden:false, textContent:'', disabled:false, open:false,
    querySelector:s => elementos.get(s), focus(){this.enfocado=true;},
    showModal(){this.open=true;}, close(){this.open=false;}});
  for(const s of ['[data-notas-csrf]','[data-panel-avisos]','[data-aviso-dialog]','[data-contador-avisos]',
    '[data-avisos-saludo]','[data-ver-aviso-saludo]','[data-aviso-titulo]','[data-aviso-texto]',
    '[data-aviso-error]','[data-aviso-entendido]','[data-abrir-avisos]']) elementos.set(s,nodo(s));
  elementos.get('[data-notas-csrf]').dataset.notasCsrf='token-privado';
  const doc={hidden:false,querySelector:s=>elementos.get(s)||null,addEventListener:(n,f)=>eventos[n]=f};
  const api=montar(doc,async(url,opciones)=>{
    peticiones.push({url,opciones});
    if(opciones.method==='POST')return {ok:true,json:async()=>({ok:true,nuevos:0})};
    return {ok:true,json:async()=>[{id:9,tipo:'Aviso',texto:'Texto privado <seguro>'}]};
  });
  await api();
  assert.equal(elementos.get('[data-contador-avisos]').textContent,'1');
  assert.equal(elementos.get('[data-ver-aviso-saludo]').textContent,'Tienes 1 aviso · Ver ›');
  eventos.click({target:{closest:s=>s==='[data-ver-aviso-saludo]'?elementos.get(s):null},preventDefault(){}});
  assert.equal(elementos.get('[data-aviso-dialog]').open,true);
  assert.equal(elementos.get('[data-aviso-texto]').textContent,'Texto privado <seguro>');
  assert.equal(peticiones.length,1,'abrir no marca leído');
  await api.entender();
  assert.equal(peticiones[1].url,'/avisos/9/entendido');
  assert.equal(peticiones[1].opciones.method,'POST');
  assert.equal(peticiones[1].opciones.body,'csrf=token-privado');
  assert.equal(elementos.get('[data-aviso-dialog]').open,false);
  assert.equal(elementos.get('[data-contador-avisos]').hidden,true);
  assert.equal(elementos.get('[data-avisos-saludo]').hidden,true);
});
