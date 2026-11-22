'use strict';
const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const fuente=fs.readFileSync('src/main/resources/static/js/centro-ayuda.js','utf8');
test('un enlace anterior por ancla abre solo un tema que existe en la página autorizada',()=>{
  let destino;
  vm.runInNewContext(fuente,{location:{search:'',hash:'#durante',replace:x=>destino=x},
    document:{querySelector:()=>({href:'/ayuda?tema=durante'}),querySelectorAll:()=>[]}});
  assert.equal(destino,'/ayuda?tema=durante');
});
test('un ancla desconocida no cambia la página ni construye un enlace externo',()=>{
  let destino;
  vm.runInNewContext(fuente,{location:{search:'',hash:'#tema-ajeno',replace:x=>destino=x},
    document:{querySelector:()=>null,querySelectorAll:()=>[]}});
  assert.equal(destino,undefined);
});
test('la respuesta de utilidad solo cambia el estado local de sus botones',()=>{
  const botones=[{},{}];
  botones.forEach(b=>{b.addEventListener=(t,f)=>b.pulsar=f;b.setAttribute=(k,v)=>b[k]=v;
    b.parentElement={querySelectorAll:()=>botones};});
  vm.runInNewContext(fuente,{location:{search:'?tema=cuenta',hash:''},document:{querySelectorAll:()=>botones}});
  botones[1].pulsar();assert.equal(botones[0]['aria-pressed'],'false');assert.equal(botones[1]['aria-pressed'],'true');
});
