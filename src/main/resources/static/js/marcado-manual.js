/* Paso 17: secuencia de marcado; no mide ni puntúa. */
(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.AplomoMarcadoManual=api;})(typeof window==='object'?window:globalThis,()=>{
 const ORDEN=Object.freeze(['hombro','codo','muneca','coronilla','cadera']);
 const TEXTOS=Object.freeze(['Toca el hombro','Toca el codo','Toca la muñeca','Toca la cabeza','Toca la cintura']);
 const avanzar=paso=>Math.min(ORDEN.length,paso+1),anterior=paso=>Math.max(0,paso-1),reiniciar=()=>0;
 return {ORDEN,TEXTOS,avanzar,anterior,reiniciar};
});
