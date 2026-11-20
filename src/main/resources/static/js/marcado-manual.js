/* RF-017, RNF-007: secuencia de marcado y guía; no mide ni puntúa. */
(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.AplomoMarcadoManual=api;})(typeof window==='object'?window:globalThis,()=>{
 const ORDEN=Object.freeze(['hombro','codo','muneca','coronilla','cadera']);
 const TEXTOS=Object.freeze(['Toca el hombro','Toca el codo','Toca la muñeca','Toca la cabeza','Toca la cintura']);
 const avanzar=paso=>Math.min(ORDEN.length,paso+1),anterior=paso=>Math.max(0,paso-1),reiniciar=()=>0;
 // RNF-007: instrucción completa y foco solo cuando la foto no está a la vista.
 const instruccion=paso=>'Paso '+(paso+1)+' de 5: '+TEXTOS[paso].toLowerCase();
 const fueraDeVista=(rect,alto,superior=0)=>rect.top<superior||rect.bottom<=superior||rect.top>=alto;
 return {ORDEN,TEXTOS,avanzar,anterior,reiniciar,instruccion,fueraDeVista};
});
