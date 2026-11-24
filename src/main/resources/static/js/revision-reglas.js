/* Avisos de revisión, sin tablas ni puntuación RULA. */
(function(raiz){
 const bordes={BRAZO:[-20,20,45,90],ANTEBRAZO:[60,100],MUNECA:[-15,-5,5,15],CUELLO:[-5,10,20],TRONCO:[-5,5,20,60]};
 const reglas=Object.freeze({
  // RNF-007: solo presentación; el valor completo continúa hacia el servidor.
  anguloTexto(valor){if(valor===null||valor===undefined||valor==='')return 'Sin medir';const n=Number(valor);return Number.isFinite(n)?Math.round(n)+'°':'Sin medir';},
  cercaDelBorde(segmento,grados){return Number.isFinite(grados)&&!!bordes[segmento]?.some(b=>Math.abs(grados-b)<3);},
  requiereRevision({segmento,grados,encontrado,discrepa=false,resuelto=false}){
   return !resuelto&&(!encontrado||discrepa||this.cercaDelBorde(segmento,grados));
  }
 });
 if(typeof module!=='undefined'&&module.exports)module.exports=reglas;else raiz.AplomoRevisionReglas=reglas;
})(typeof window==='undefined'?{}:window);
