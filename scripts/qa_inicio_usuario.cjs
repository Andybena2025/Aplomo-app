'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const fuente=fs.readFileSync('src/main/resources/static/js/inicio-usuario.js','utf8');
function ejecutar(valor,fallar=false){let clic,guardado;const tarjeta={hidden:false,querySelector:()=>({addEventListener:(_,f)=>clic=f})};
vm.runInNewContext(fuente,{document:{querySelector:()=>tarjeta},sessionStorage:{getItem(){if(fallar)throw Error();return valor;},setItem(k,v){if(fallar)throw Error();guardado=[k,v];}}});
return {tarjeta,clic,guardado:()=>guardado};}
test('ayuda se oculta solo durante la sesión y permite cerrar',()=>{const e=ejecutar(null);assert.equal(e.tarjeta.hidden,false);e.clic();assert.equal(e.tarjeta.hidden,true);assert.deepEqual(e.guardado(),['alineo-ayuda-inicio-oculta','1']);assert.equal(ejecutar('1').tarjeta.hidden,true);});
test('sin acceso a sessionStorage sigue siendo usable',()=>{const e=ejecutar(null,true);assert.equal(e.tarjeta.hidden,false);assert.doesNotThrow(e.clic);assert.equal(e.tarjeta.hidden,true);});
