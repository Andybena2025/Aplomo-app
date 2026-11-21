/* Paso 25, RNF-007: búsqueda progresiva, sin tildes ni distinción de mayúsculas. */
(function(raiz,fabrica){const ayuda=fabrica();if(typeof module==='object'&&module.exports)module.exports=ayuda;if(raiz?.document)ayuda.montar(raiz.document);})(typeof window==='undefined'?null:window,()=>{
 function normalizar(texto){return String(texto??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('es').trim();}
 function coincide(texto,busqueda){return normalizar(texto).includes(normalizar(busqueda));}
 function filtrar(textos,busqueda){const visibles=textos.map(t=>coincide(t,busqueda));return {visibles,sinResultados:!visibles.some(Boolean)};}
 function montar(doc){const input=doc.querySelector('[data-ayuda-buscar]');if(!input)return;const preguntas=[...doc.querySelectorAll('[data-ayuda-pregunta]')],grupos=[...doc.querySelectorAll('[data-ayuda-grupo]')],vacio=doc.querySelector('[data-ayuda-vacia]');input.addEventListener('input',()=>{const resultado=filtrar(preguntas.map(p=>p.textContent),input.value);preguntas.forEach((p,i)=>{p.hidden=!resultado.visibles[i];});grupos.forEach(g=>{g.hidden=![...g.querySelectorAll('[data-ayuda-pregunta]')].some(p=>!p.hidden);});vacio.hidden=!resultado.sinResultados;});}
 return {normalizar,coincide,filtrar,montar};
});
