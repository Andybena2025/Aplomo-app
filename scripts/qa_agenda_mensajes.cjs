'use strict';
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const fuente=fs.readFileSync('src/main/resources/static/js/agenda-mensajes.js','utf8');
const api=require('../src/main/resources/static/js/agenda-mensajes.js');
const html='<section data-agenda-url="/agenda/citas/7">mensajes</section>';
function entorno(obtener){
 const eventos={},fases={},panelEventos={},envios=[];
 const campo={value:'',focus(){this.enfocado=true;}},salida={textContent:''},contador={textContent:''};
 const lista={innerHTML:'anterior'},raiz={dataset:{agendaUrl:'/agenda/citas/7',agendaCsrf:'token QA'}};
 const contenido={innerHTML:'',textContent:'',querySelector(s){return {'[data-agenda-texto]':campo,
  '[data-agenda-error]':salida,'[data-agenda-contador]':contador,'[data-agenda-url]':raiz,
  '[data-cita-mensajes]':lista}[s]||null;}};
 const cerrar={focus(){}},panel={open:false,showModal(){this.open=true;},close(){this.open=false;},
  addEventListener(n,f){panelEventos[n]=f;},querySelector:s=>s==='[data-agenda-contenido]'?contenido:cerrar};
 const doc={hidden:false,querySelector:()=>panel,addEventListener(n,f,captura){eventos[n]=f;fases[n]=captura;}};
 const modulo={exports:{}};
 class Parser{parseFromString(texto){return{querySelector:s=>!texto.includes('data-agenda-url')?null:
  s==='[data-cita-mensajes]'?{innerHTML:texto}:raiz};}}
 class Datos{constructor(form){return form.valores;}}
 vm.runInNewContext(fuente,{module:modulo,DOMParser:Parser,URLSearchParams,FormData:Datos});
 const control=modulo.exports.montar(doc,async(url,opts)=>{envios.push({url,opts});return obtener?obtener(url,opts):
  {ok:true,text:async()=>html};});
 const boton={disabled:false,textContent:'Enviar'};
 const form={valores:[['csrf','token QA'],['texto','Texto con <script>']],matches:s=>s==='[data-agenda-enviar]',
  getAttribute:()=>'/agenda/citas/7/responder',querySelector:()=>boton};
 return{control,eventos,fases,panelEventos,envios,campo,salida,contador,contenido,panel,lista,raiz,boton,form,
  enviar(){let prevenido=false;const promesa=eventos.submit({target:form,preventDefault(){prevenido=true;}});
   assert.equal(prevenido,true);return promesa;}};
}
test('Ctrl, Cmd, Mayúsculas, Alt y botón central conservan el enlace de reserva',()=>{
 assert.equal(api.debeAbrir({button:0}),true);
 for(const modificador of ['ctrlKey','metaKey','shiftKey','altKey'])assert.equal(api.debeAbrir({[modificador]:true}),false);
 assert.equal(api.debeAbrir({button:1}),false);
 assert.equal(api.debeAbrir({defaultPrevented:true}),false);
});
test('abrir usa GET sin datos y después acusa con POST y CSRF, solo tras mostrar la conversación',async()=>{
 const e=entorno();await e.control.abrir('/agenda/citas/7');
 assert.equal(e.panel.open,true);assert.equal(e.contenido.innerHTML,html);
 assert.equal(e.envios.length,2);assert.equal(e.envios[0].opts.headers['X-Alineo-Agenda'],'1');
 assert.equal(e.envios[1].url,'/agenda/citas/7/leidos');assert.equal(e.envios[1].opts.method,'POST');
 assert.equal(new URLSearchParams(e.envios[1].opts.body).get('csrf'),'token QA');
});
test('un fallo al abrir no acusa ni deja mensajes de otra cita',async()=>{
 const e=entorno(async()=>({ok:false}));await e.control.abrir('/agenda/citas/7');
 assert.equal(e.envios.length,1);assert.match(e.salida.textContent,/No pudimos abrir/);
});
test('al fallar el envío conserva el borrador, restaura el botón y permite reintentar',async()=>{
 const f=entorno(async(url)=>({ok:!url.includes('responder'),text:async()=>html}));
 await f.control.abrir('/agenda/citas/7');f.campo.value='Borrador importante';await f.enviar();
 assert.equal(f.campo.value,'Borrador importante');assert.equal(f.boton.disabled,false);
 assert.equal(f.boton.textContent,'Enviar');assert.match(f.salida.textContent,/El texto sigue aquí/);
 const envio=f.envios.find(x=>x.url.endsWith('responder'));
 assert.equal(new URLSearchParams(envio.opts.body).get('csrf'),'token QA');
 assert.equal(new URLSearchParams(envio.opts.body).get('texto'),'Texto con <script>');
 await f.enviar();assert.equal(f.envios.filter(x=>x.url.endsWith('responder')).length,2);
});
test('dos pulsaciones mientras envía generan un solo POST',async()=>{
 let terminar;const e=entorno(async(url)=>url.endsWith('responder')?new Promise(r=>terminar=r):
  {ok:true,text:async()=>html});
 await e.control.abrir('/agenda/citas/7');const pendiente=e.enviar();await e.enviar();
 assert.equal(e.boton.disabled,true);assert.equal(e.envios.filter(x=>x.url.endsWith('responder')).length,1);
 terminar({ok:true,text:async()=>html});await pendiente;assert.equal(e.boton.disabled,false);
});
test('chips editables y contador reflejan el texto sin enviarlo por sí solos',async()=>{
 const e=entorno();await e.control.abrir('/agenda/citas/7');const antes=e.envios.length;
 const chip={dataset:{agendaPlantilla:'Llevar EPP: '}};
 e.eventos.click({target:{closest:s=>s==='[data-agenda-plantilla]'?chip:null}});
 assert.equal(e.campo.value,'Llevar EPP: ');assert.equal(e.contador.textContent,'12 / 500');
 e.campo.value+='casco';e.eventos.input({target:{matches:()=>true}});
 assert.equal(e.contador.textContent,'17 / 500');assert.equal(e.envios.length,antes);
});
test('el sondeo actualiza solo las burbujas y conserva lo que se está escribiendo',async()=>{
 const e=entorno();await e.control.abrir('/agenda/citas/7');e.campo.value='Estoy respondiendo';
 await e.control.actualizar();assert.equal(e.envios[2].opts.headers['X-Alineo-Sondeo'],'1');
 assert.equal(e.envios[3].opts.headers['X-Alineo-Sondeo'],'1');assert.equal(e.lista.innerHTML,html);assert.equal(e.campo.value,'Estoy respondiendo');
 const total=e.envios.length;e.panel.open=false;await e.control.actualizar();assert.equal(e.envios.length,total);
});
test('cerrar antes de terminar la carga impide que una respuesta tardía reabra el panel o acuse',async()=>{
 let terminar;const e=entorno(()=>new Promise(r=>terminar=r));const carga=e.control.abrir('/agenda/citas/7');
 e.panel.open=false;e.panelEventos.close();terminar({ok:true,text:async()=>html});await carga;
 assert.equal(e.envios.length,1);assert.equal(e.contenido.innerHTML,'');assert.equal(e.panel.open,false);
});
test('un fallo de acuse posterior no presenta un envío guardado como fallido ni invita a duplicarlo',async()=>{
 let fallo=false;
 const e=entorno(async(url)=>{if(fallo&&url.endsWith('leidos'))throw new Error('Sin conexión');
  return{ok:true,text:async()=>html};});
 await e.control.abrir('/agenda/citas/7');fallo=true;await e.enviar();
 assert.match(e.salida.textContent,/no pudimos confirmar su lectura/);
 assert.doesNotMatch(e.salida.textContent,/No pudimos enviar/);
 assert.equal(e.envios.filter(x=>x.url.endsWith('responder')).length,1);
});
test('la agenda consume clic y envío antes del indicador global de navegación',async()=>{
 const e=entorno();const enlace={textContent:'Escribir',getAttribute:()=>'/agenda/evaluadores/4',focus(){}};
 let navegando=false;
 const evento={button:0,defaultPrevented:false,target:{closest:s=>s.includes('.cal-cita')?enlace:null},
  preventDefault(){this.defaultPrevented=true;}};
 if(e.fases.click)e.eventos.click(evento);
 if(!evento.defaultPrevented){navegando=true;enlace.textContent='Cargando…';}
 if(!e.fases.click)e.eventos.click(evento);
 await new Promise(setImmediate);
 assert.equal(navegando,false);assert.equal(enlace.textContent,'Escribir');
 assert.equal(e.panel.open,true);assert.equal(e.fases.submit,true);
});
