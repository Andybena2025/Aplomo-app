/* Paso 18: filtra solo ejemplos públicos; no modifica los archivos ni la subida. */
(function(raiz,fabrica){const api=fabrica();if(typeof module==='object'&&module.exports)module.exports=api;else raiz.AplomoGaleria=api;})(typeof window==='undefined'?globalThis:window,()=>{
 function seleccionar(fotos,{vista,contexto='',lado='AUTO',turno=0}){
  const compatibles=fotos.filter(f=>f.vista===vista&&(!contexto||f.contexto===contexto));
  const mira=lado==='DERECHO'?'DERECHA':lado==='IZQUIERDO'?'IZQUIERDA':'';
  const buenas=compatibles.filter(f=>f.tipo==='BUENA'&&(vista!=='PERFIL'||!mira||f.mira===mira));
  const inicio=buenas.length?turno%buenas.length:0,lista=[...buenas.slice(inicio),...buenas.slice(0,inicio)].slice(0,6);
  if(vista==='PERFIL'){const errores=compatibles.filter(f=>f.tipo==='ERROR'&&f.rotacion);if(errores.length)lista.push(errores[turno%errores.length]);}
  return lista;
 }
 function iniciar(form,opciones){
  const galerias=[...form.querySelectorAll('[data-galeria-vista]')].map(el=>({el,turno:-1,clave:'',fotos:[...el.querySelectorAll('[data-usar-ejemplo]')].map(b=>({el:b,vista:el.dataset.galeriaVista,contexto:b.dataset.contexto,mira:b.dataset.mira,tipo:b.dataset.tipo,rotacion:b.dataset.rotacion==='true'}))}));
  function pintar(g,nueva=false){const o=opciones(),clave=JSON.stringify(o);if(!g.el.open||(!nueva&&g.clave===clave))return;if(nueva)g.turno++;g.clave=clave;
   const elegidas=seleccionar(g.fotos,{...o,vista:g.el.dataset.galeriaVista,turno:Math.max(0,g.turno)});
   for(const f of g.fotos){f.el.hidden=!elegidas.includes(f);const l=f.el.querySelector('[data-mira-label]');l.hidden=g.el.dataset.galeriaVista!=='PERFIL'||!f.mira||['DERECHO','IZQUIERDO'].includes(o.lado);l.textContent=f.mira==='DERECHA'?'Mira a la derecha':'Mira a la izquierda';}
   // El orden de los botones visibles también sigue el turno, sin crear otra ventana.
   for(const f of elegidas)g.el.querySelector('.galeria-demo-lista').append(f.el);
  }
  for(const g of galerias)g.el.addEventListener('toggle',()=>{if(g.el.open)pintar(g,true);});
  return {actualizar(){galerias.forEach(g=>pintar(g));}};
 }
 return {seleccionar,iniciar};
});
