/* RF-016: subidas y ejemplos comparten validación y reciben el mismo objeto File. */
(function(raiz){
 const api={
  validar(file,permiso,cantidad){
   if(!permiso)throw new Error('No hay permiso para fotos.');
   if(cantidad>=10)throw new Error('Puedes guardar hasta 10 fotos.');
   if(!file||!['image/jpeg','image/png'].includes(file.type))throw new Error('El archivo no es JPG ni PNG.');
   if(!file.size)throw new Error('El archivo está vacío. Sube una foto JPG o PNG.');
   if(file.size>5*1024*1024)throw new Error('La foto pesa más de 5 MB. Sube una más liviana.');
   return file;
  },
  MIN_LADO:240,
  async verificarPerfil(file,{medir}={}){
   const b=new Uint8Array(await file.slice(0,8).arrayBuffer()),jpg=b[0]===255&&b[1]===216&&b[2]===255,png=[137,80,78,71,13,10,26,10].every((v,i)=>b[i]===v);
   if(!jpg&&!png)throw new Error('El archivo no es JPG ni PNG.');
   const dimensiones=medir||async function(f){const url=raiz.URL.createObjectURL(f);try{const im=new raiz.Image();im.src=url;await im.decode();return [im.naturalWidth,im.naturalHeight];}finally{raiz.URL.revokeObjectURL(url);}};
   let w,h;try{[w,h]=await dimensiones(file);}catch(_){throw new Error('No pudimos abrir la foto. Sube otra JPG o PNG.');}
   if(w<api.MIN_LADO||h<api.MIN_LADO)throw new Error('La foto es muy pequeña. Sube una de mejor calidad.');return {ancho:w,alto:h};
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
