'use strict';
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const fuente=fs.readFileSync('src/main/resources/static/js/notas.js','utf8');
function entorno(obtener){
 const eventos={},fases={},peticiones=[];
 const boton={disabled:false,textContent:'Enviar mensaje',focus(){}},error={textContent:''},contador={textContent:''};
 const contenido={innerHTML:'borrador'},texto={value:'Mensaje de prueba',closest:()=>form};
 const form={matches:s=>s==='[data-nuevo-aviso]',valores:[['csrf','token privado'],['para','12'],['texto','Mensaje <seguro>']],
  querySelector:s=>s==='button[type="submit"]'?boton:s==='[data-nuevo-aviso-error]'?error:contador,
  getAttribute:()=>'/avisos/nuevo'};
 const panel={open:true,contains:f=>f===form,querySelector:s=>s==='[data-contenido-avisos]'?contenido:boton,
  showModal(){this.open=true;},close(){this.open=false;}};
 const doc={querySelector:s=>s==='[data-notas-csrf]'?{dataset:{notasCsrf:'token privado'}}:
  s==='[data-panel-avisos]'?panel:null,addEventListener(n,f,captura){eventos[n]=f;fases[n]=captura;}};
 class Datos{constructor(f){return f.valores;}}
 class Parser{parseFromString(s){return{querySelector:()=>s.includes('data-bandeja-avisos')||s.includes('data-nuevo-aviso')?{}:null};}}
 const modulo={exports:{}};
 vm.runInNewContext(fuente,{module:modulo,FormData:Datos,DOMParser:Parser,URLSearchParams});
 modulo.exports.montar(doc,async(url,opts)=>{peticiones.push({url,opts});return obtener?obtener(url,opts):
  {ok:true,text:async()=>'<div data-bandeja-avisos>Mensaje enviado</div>'};});
 return{eventos,fases,peticiones,form,panel,contenido,boton,error,texto,contador,
  enviar(){return eventos.submit({target:form,preventDefault(){}});}};
}
test('el contador del nuevo mensaje funciona también en contenido cargado dentro del panel',()=>{
 const e=entorno();e.texto.value='Diez letras';
 e.eventos.input({target:{closest:()=>e.texto}});assert.equal(e.contador.textContent,'11 / 500');
});
test('envía con CSRF, destinatario y cabecera de panel, y vuelve a la bandeja sin navegar',async()=>{
 const e=entorno();await e.enviar();
 assert.equal(e.peticiones.length,1);const p=e.peticiones[0];
 assert.equal(p.url,'/avisos/nuevo');assert.equal(p.opts.method,'POST');
 assert.equal(p.opts.headers['X-Alineo-Panel'],'1');
 assert.equal(new URLSearchParams(p.opts.body).get('csrf'),'token privado');
 assert.equal(new URLSearchParams(p.opts.body).get('para'),'12');
 assert.match(e.contenido.innerHTML,/data-bandeja-avisos/);assert.equal(e.boton.disabled,false);
 assert.equal(e.fases.submit,true,'el envío evita el indicador global de navegación');
});
test('un fallo conserva texto y destinatario y deja volver a intentar',async()=>{
 const e=entorno(async()=>{throw new Error('sin red');});await e.enviar();
 assert.equal(e.contenido.innerHTML,'borrador');assert.equal(e.texto.value,'Mensaje de prueba');
 assert.equal(e.form.valores[1][1],'12');assert.equal(e.boton.disabled,false);
 assert.match(e.error.textContent,/Conservamos el texto/);await e.enviar();assert.equal(e.peticiones.length,2);
});
test('un segundo clic mientras envía no duplica el mensaje',async()=>{
 let terminar;const e=entorno(()=>new Promise(r=>terminar=r));const p=e.enviar();await e.enviar();
 assert.equal(e.peticiones.length,1);assert.equal(e.boton.disabled,true);
 terminar({ok:true,text:async()=>'<div data-bandeja-avisos>Enviado</div>'});await p;
 assert.equal(e.boton.disabled,false);
});
test('Nuevo mensaje abre el fragmento con X-Alineo-Panel antes del indicador global y respeta Ctrl',async()=>{
 const e=entorno();const enlace={getAttribute:()=>'/avisos/nuevo'};let prevenidos=0;
 const target={closest:s=>s==='a[href]'||s==='[data-lista-avisos]'?enlace:null};
 e.eventos.click({target,button:0,preventDefault(){prevenidos++;}});await new Promise(setImmediate);
 assert.equal(prevenidos,1);assert.equal(e.peticiones[0].url,'/avisos/nuevo');
 assert.equal(e.peticiones[0].opts.headers['X-Alineo-Panel'],'1');assert.equal(e.fases.click,true);
 e.eventos.click({target,button:0,ctrlKey:true,preventDefault(){prevenidos++;}});
 assert.equal(prevenidos,1);assert.equal(e.peticiones.length,1);
});
