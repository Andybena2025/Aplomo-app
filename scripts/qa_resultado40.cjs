const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const ui=require('../src/main/resources/static/js/resultado40.js');
const niveles=[{min:1,max:2,color:'VERDE',nivel:1,accion:'No necesita cambios'},{min:3,max:4,color:'AMARILLO',nivel:2,accion:'Hacer una revisión'},{min:5,max:6,color:'ROJO',nivel:3,accion:'Intervención inmediata'},{min:7,max:7,color:'ROJO',nivel:4,accion:'Intervención inmediata'}];
const base={puntaje:6,nivel:3,color:'ROJO',accion:'Intervención inmediata',lado:'DERECHO',actividad:0,carga:2,niveles,
 proyeccion:{puntaje:3,color:'AMARILLO'},segmentos:[
 {clave:'ANTEBRAZO',nombre:'Antebrazo',puntos:2,minimo:1,maximo:3,peso:2/3,color:'ROJO',medido:'Codo a 47°',meta:'60° a 100°',cambio:'Ajustar la altura',acciones:[]},
 {clave:'MUNECA',nombre:'Muñeca',puntos:1,minimo:1,maximo:4,peso:1/4,color:'VERDE',medido:'Recta',meta:'Hasta 5°',cambio:'Soporte',acciones:[]}],desglose:'<table><tr><td>Tabla A · B</td></tr></table>'};
const posturas=[{titulo:'La más incómoda',analisis:[base]},{titulo:'La que más dura',analisis:[{...base,puntaje:4,color:'AMARILLO',nivel:2}]}];
test('un solo anillo grande y anillos iguales para todas las posturas',()=>{
 const html=ui.render(posturas);assert.equal((html.match(/r40-anillo grande/g)||[]).length,1);
 assert.equal((html.match(/r40-anillo pequeno/g)||[]).length,2);
 assert.equal((html.match(/MÁS CRÍTICA/g)||[]).length,1);
 assert.ok(!html.includes('Qué hacer primero'));assert.ok(!html.includes('Sugerencias · aparte'));
});
test('desgloses cerrados por defecto, orden del servidor y solo partes por encima del mínimo',()=>{
 const html=ui.render(posturas);assert.equal((html.match(/<details class="r40-desglose">/g)||[]).length,2);
 assert.ok(!html.includes('r40-desglose" open'));assert.ok(html.includes('data-plan-parte="ANTEBRAZO"'));
 assert.ok(!html.includes('data-plan-parte="MUNECA"'));assert.ok(html.includes('PESA MÁS'));
 assert.ok(html.includes('Si se cumple el plan, el puesto bajaría a 3 · Amarillo'));
});
test('cuerpo con curvas C y resplandores recortados, sin figura de palitos',()=>{
 const path=ui.contorno();assert.ok(path.startsWith('M85,46 C'));assert.ok(path.endsWith(' Z'));
 const html=ui.render(posturas);assert.ok(html.includes('clipPath'));assert.ok(html.includes('radialGradient'));
 assert.ok(html.includes('viewBox="0 0 160 300"'));assert.ok(html.includes('cx="80" cy="28" rx="14" ry="17"'));
});
test('texto variable escapado; trabajador ausente no deja franja y no inventa historia',()=>{
 const html=ui.render([{titulo:'<script>alert(1)</script>',analisis:[base]}]);
 assert.ok(!html.includes('<script>alert'));assert.ok(!html.includes('r40-trabajador'));
 assert.ok(!html.includes('Evaluación anterior'));assert.equal(ui.trabajador({}),'');
 assert.ok(ui.trabajador({edad:25}).includes('25 años'));
});
test('estado persistido y formularios CSRF usan el endpoint actual sin fórmulas en el navegador',()=>{
 const d={...base,editable:true,guardado:true,segmentos:[{...base.segmentos[0],acciones:[{id:31,estado:'HECHA',texto:'Ajustar altura'}]}]};
 const html=ui.render([{titulo:'Postura',analisis:[d]}],'token-seguro');
 assert.ok(html.includes('/sugerencias/31/estado'));assert.ok(html.includes('value="token-seguro"'));
 assert.ok(html.includes('value="HECHA" selected'));assert.ok(html.includes('r40-avance hecha'));
 assert.ok(!ui.render(posturas).includes('Agendar reevaluación'));
});
test('contrato CSS de una columna, tarjetas móviles y texto sin truncar (la geometría se comprueba en navegador)',()=>{
 const css=fs.readFileSync('src/main/resources/static/css/aplomo.css','utf8').split('/* Paso 40 · RF-020/023')[1];
 assert.ok(css.includes('.resultado40{display:grid;grid-template-columns:minmax(0,1fr)'));
 assert.ok(css.includes('overflow-wrap:anywhere'));assert.ok(css.includes('@media(max-width:720px)'));
 assert.ok(css.includes('.r40-plan thead{display:none}'));assert.ok(css.includes('white-space:normal'));
 assert.ok(!css.includes('text-overflow:ellipsis'));assert.ok(!css.includes('line-clamp'));
 assert.ok(ui.render(posturas).includes('r40-plan-scroll'));
});

test('riesgo que viene solo de carga no promete bajar ni inventa filas; el aviso va dentro del plan',()=>{
 const d={...base,puntaje:4,color:'AMARILLO',segmentos:[base.segmentos[1]],
  proyeccion:{puntaje:4,color:'AMARILLO'},sinSugerencias:true,guardado:true,editable:true,evaluacion:8};
 const html=ui.render([{titulo:'Solo carga',analisis:[d]}]);
 assert.ok(html.includes('el puntaje se mantiene en 4'));
 assert.ok(html.includes('Revisa la actividad muscular y la carga'));
 assert.ok(html.includes('/sugerencias/nueva?evaluacion=8'));
 assert.ok(!ui.render([{titulo:'Solo carga',analisis:[{...d,editable:false}]}]).includes('Escribir sugerencia'));
});

// Paso 41 · RNF-007/008: semántica, navegación de teclado y conservación de la pestaña.
test('resultado con tres pestañas enlazadas; sin activar JS los tres paneles quedan completos',()=>{
 const html=ui.render(posturas);
 assert.equal((html.match(/role="tablist"/g)||[]).length,1);
 assert.equal((html.match(/role="tab"/g)||[]).length,3);
 assert.equal((html.match(/role="tabpanel"/g)||[]).length,3);
 const tabs=[...html.matchAll(/role="tab" id="([^"]+)" aria-controls="([^"]+)" aria-selected="(true|false)"/g)];
 assert.equal(tabs[0][3],'true');assert.equal(tabs[1][3],'false');
 for(const [,tab,panel] of tabs)assert.ok(html.includes(`id="${panel}" aria-labelledby="${tab}"`));
 assert.ok(html.includes('aria-label="Entender el resultado" hidden'));
 assert.ok(!html.match(/role="tabpanel"[^>]*\bhidden\b/));
 const css=fs.readFileSync('src/main/resources/static/css/densidad.css','utf8');
 assert.ok(css.includes('@media print'));assert.ok(css.includes('[role=tabpanel][hidden]{display:block!important'));
 assert.ok(!/\bzoom\s*:/.test(css));assert.ok(css.includes('min-width:900px'));
});
function escenarioPestanas(memoria=new Map(),search=''){
 const botones=Array.from({length:3},()=>({eventos:{},atributos:{},addEventListener(n,f){this.eventos[n]=f;},setAttribute(n,v){this.atributos[n]=v;},focus(){this.enfocado=true;}}));
 const paneles=Array.from({length:3},()=>({hidden:false}));
 const barra={dataset:{},hidden:true,querySelectorAll(){return botones;}};
 const resultado={clases:new Set(),eventos:{},querySelector(){return barra;},querySelectorAll(){return paneles;},addEventListener(n,f){this.eventos[n]=f;}};
 resultado.classList={add:n=>resultado.clases.add(n)};
 const entorno={location:{pathname:'/evaluaciones/15',search},sessionStorage:{getItem:k=>memoria.get(k)??null,setItem:(k,v)=>memoria.set(k,v)}};
 ui.iniciar({querySelectorAll:()=>[resultado]},entorno);
 return {botones,paneles,barra,resultado,entorno};
}
test('pestañas responden al clic y flechas con foco y selección única',()=>{
 const s=escenarioPestanas();assert.equal(s.barra.hidden,false);
 assert.deepEqual(s.paneles.map(p=>p.hidden),[false,true,true]);
 s.botones[2].eventos.click();assert.deepEqual(s.paneles.map(p=>p.hidden),[true,true,false]);
 let prevenido=false;s.botones[2].eventos.keydown({key:'ArrowRight',preventDefault(){prevenido=true;}});
 assert.ok(prevenido);assert.equal(s.botones[0].enfocado,true);
 assert.deepEqual(s.botones.map(b=>b.atributos['aria-selected']),['true','false','false']);
 s.botones[0].eventos.keydown({key:'ArrowLeft',preventDefault(){}});
 assert.deepEqual(s.paneles.map(p=>p.hidden),[true,true,false]);
});
test('recarga y guardar estado conservan Plan; parámetro plan tiene prioridad',()=>{
 const memoria=new Map(),s=escenarioPestanas(memoria);
 s.botones[2].eventos.click();assert.equal(escenarioPestanas(memoria).paneles[2].hidden,false);
 s.botones[0].eventos.click();s.resultado.eventos.submit({target:{matches:()=>true}});
 assert.equal(escenarioPestanas(memoria).paneles[2].hidden,false);
 assert.equal(escenarioPestanas(new Map(),'?pestana=plan').paneles[2].hidden,false);
});
test('almacenamiento bloqueado no impide usar las pestañas',()=>{
 const s=escenarioPestanas();s.barra.dataset={};
 s.entorno.sessionStorage={getItem(){throw Error('bloqueado');},setItem(){throw Error('bloqueado');}};
 assert.doesNotThrow(()=>ui.iniciar({querySelectorAll:()=>[s.resultado]},s.entorno));
 s.botones[1].eventos.click();assert.equal(s.paneles[1].hidden,false);
});

test('guardar el Estado desde Resultado vuelve a Plan sin alterar los envíos de otras páginas',()=>{
 const js=fs.readFileSync('src/main/resources/static/js/sugerencias.js','utf8');
 assert.ok(js.includes("form.closest('.resultado40')?new URL(location.href):null"));
 assert.ok(js.includes("regreso.searchParams.set('pestana','plan')"));
 assert.ok(js.includes("regreso?.href||r.headers.get('Location')"));
});
