/* RF-016: subidas y ejemplos comparten validación y reciben el mismo objeto File. */
(function(raiz){
 const api={
  validar(file,permiso,cantidad){
   if(!permiso)throw new Error('No hay permiso para fotos.');
   if(cantidad>=10)throw new Error('Puedes guardar hasta 10 fotos.');
   if(!file||!['image/jpeg','image/png'].includes(file.type)||!file.size||file.size>5*1024*1024)throw new Error('Elige una foto JPG o PNG de hasta 5 MB.');
   return file;
  },
  async ejemplo(url,recibir,{descargar=raiz.fetch?.bind(raiz),Archivo=raiz.File}={}){
   if(!/^\/static\/img\/galeria\/S[2-6]_[a-z0-9_]+_(perfil|frente|detalle)\.jpg$/.test(url))throw new Error('Esa foto de ejemplo no está disponible.');
   const r=await descargar(url);if(!r.ok)throw new Error('No pudimos abrir el ejemplo. Intenta de nuevo.');
   const blob=await r.blob(),file=new Archivo([blob],url.split('/').pop(),{type:blob.type});
   return recibir(file);
  }
 };
 if(typeof module!=='undefined'&&module.exports)module.exports=api;else raiz.AplomoFotosCampo=api;
})(typeof window==='undefined'?globalThis:window);
