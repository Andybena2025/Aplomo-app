/* RNF-003: exclusión de doble toque y liberación garantizada incluso ante fallos. */
(function(raiz){
 function crearAccionEvaluacion({alCambiar=()=>{},alError=()=>{}}={}){
  let ocupada=false;
  return {
   get ocupada(){return ocupada;},
   async ejecutar(fn){
    if(ocupada)return false;
    ocupada=true;
    try{alCambiar(true);await fn();return true;}
    catch(error){alError(error);return false;}
    finally{ocupada=false;alCambiar(false);}
   }
  };
 }
 if(typeof module!=='undefined'&&module.exports)module.exports=crearAccionEvaluacion;
 else raiz.crearAccionEvaluacion=crearAccionEvaluacion;
})(typeof window==='undefined'?{}:window);
