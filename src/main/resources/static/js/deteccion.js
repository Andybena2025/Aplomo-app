/* Paso 14: detección del mock, local al navegador. Solo se descargan código y modelo. */
(() => {
  const MP_URL='https://cdn.jsdelivr.net/npm/@mediapipe/pose@0.5.1675469404/';
  const CORONILLA={arriba:1.2,atras:0.9};
  const clamp=(n,a,b)=>Math.max(a,Math.min(b,n));
  const LIMITES=Object.freeze({script:20000,modelo:60000,inferencia:20000,total:60000}),MAX_LADO=1280,COMPLEJIDAD=0;
  let MP=null,cargando=null,scriptLista=null,cola=Promise.resolve(),generacion=0;
  const instancias=new Set(),diagnostico={etapa:'lista',historial:[]};
  function etapa(nombre){diagnostico.etapa=nombre;diagnostico.historial.push({etapa:nombre,ms:Math.round(performance.now())});if(diagnostico.historial.length>40)diagnostico.historial.shift();}
  function fallo(){
    if(diagnostico.etapa.startsWith('falló'))return;
    const textos={script:'falló al cargar el script',modelo:'falló al cargar el modelo',imagen:'falló al preparar la foto',inferencia:'falló al detectar los puntos'};
    etapa(textos[diagnostico.etapa]||'falló al completar la detección');
  }
  function liberar(){generacion++;for(const i of instancias)cerrar(i);cargando=null;cola=Promise.resolve();}
  function cancelar(){fallo();liberar();}
  function vigente(turno){if(turno!==generacion)throw new Error('La detección se canceló. Puedes intentarlo de nuevo.');}
  function conTiempo(promesa,ms,mensaje){
    let reloj;
    return Promise.race([promesa,new Promise((_,no)=>{reloj=setTimeout(()=>no(new Error(mensaje)),ms);})])
      .finally(()=>clearTimeout(reloj));
  }
  function cargarScript(){
    if(window.Pose)return Promise.resolve();
    if(scriptLista)return scriptLista;
    etapa('script');const script=document.createElement('script');
    script.src=MP_URL+'pose.js';script.crossOrigin='anonymous';
    const carga=new Promise((ok,no)=>{script.onload=ok;script.onerror=()=>no(new Error('No se pudo cargar la detección.'));});
    document.head.append(script);
    scriptLista=conTiempo(carga,LIMITES.script,'La detección no pudo cargarse a tiempo.').catch(error=>{script.remove();scriptLista=null;throw error;});
    return scriptLista;
  }
  function cerrar(instancia){
    if(MP===instancia)MP=null;instancias.delete(instancia);if(instancia.cerrada)return;instancia.cerrada=true;
    try{Promise.resolve(instancia.pose.close()).catch(()=>{});}catch(_){/* El marcado manual y el siguiente intento siguen disponibles. */}
  }
  function poseLista(){
    if(MP)return Promise.resolve(MP);
    if(cargando)return cargando.promesa;
    const trabajo={turno:generacion,promesa:null};
    trabajo.promesa=(async()=>{
      let instancia;
      try{
        await cargarScript();vigente(trabajo.turno);
        instancia={pose:new window.Pose({locateFile:archivo=>MP_URL+archivo}),resultado:null};instancias.add(instancia);
        instancia.pose.setOptions({modelComplexity:COMPLEJIDAD,smoothLandmarks:false,enableSegmentation:false,minDetectionConfidence:.5});
        instancia.pose.onResults(resultado=>{if(!instancia.cerrada)instancia.resultado=resultado;});
        etapa('modelo');await conTiempo(instancia.pose.initialize(),LIMITES.modelo,'El modelo no pudo cargarse a tiempo.');vigente(trabajo.turno);
        MP=instancia;etapa('modelo-listo');return instancia;
      }catch(error){if(instancia)cerrar(instancia);if(trabajo.turno===generacion)fallo();throw error;}
      finally{if(cargando===trabajo)cargando=null;}
    })();cargando=trabajo;return trabajo.promesa;
  }
  const PERFIL=Object.freeze({visibilidad:.5,frontal:.45,margen:.10});
  // Sugerencia visual, no puntuación RULA. Distancias relativas al alto de la imagen.
  const APOYO_PIES=Object.freeze({PUNTOS:Object.freeze([23,24,27,28]),DIFERENCIA_RELATIVA_MINIMA:.40,ALTURA_CADERA_TOBILLOS_MINIMA:.22,MARGEN_NUMERICO:1e-9,CONTEXTOS_EXCLUIDOS:Object.freeze(['OFICINA'])});
  function sugerirPiesSinApoyo(puntos,{contexto='',vista='PERFIL'}={}){
    if(vista!=='PERFIL'||APOYO_PIES.CONTEXTOS_EXCLUIDOS.includes(contexto.toUpperCase()))return null;
    if(!APOYO_PIES.PUNTOS.every(i=>{const p=puntos?.[i];return p&&Number.isFinite(p.x)&&Number.isFinite(p.y)&&p.x>=0&&p.x<=1&&p.y>=0&&p.y<=1&&(p.visibility??p.v??0)>=PERFIL.visibilidad;}))return null;
    const cadera=(puntos[23].y+puntos[24].y)/2,tobillos=(puntos[27].y+puntos[28].y)/2,altura=Math.abs(tobillos-cadera);
    if(altura<APOYO_PIES.ALTURA_CADERA_TOBILLOS_MINIMA)return null;
    return Math.abs(puntos[27].y-puntos[28].y)/altura-APOYO_PIES.DIFERENCIA_RELATIVA_MINIMA>APOYO_PIES.MARGEN_NUMERICO?true:null;
  }
  function revisarPerfil(puntos,ancho=1,alto=1){
    const aviso=(tipo,mensaje,bloquea=true)=>({tipo,mensaje,bloquea,permiteUsar:true});
    if(!puntos||puntos.length<33)return aviso('SIN_PERSONA','No se ve una persona. Toma otra de lado, con el cuerpo completo.');
    const dentro=i=>{const p=puntos[i];return p&&Number.isFinite(p.x)&&Number.isFinite(p.y)&&p.x>=0&&p.x<=1&&p.y>=0&&p.y<=1;};
    const visible=i=>dentro(i)&&(puntos[i].visibility??puntos[i].v??0)>=PERFIL.visibilidad;
    // Paso 17: primero orientación por coordenadas, incluso con el lado lejano oculto.
    if([11,12,23,24].every(i=>Number.isFinite(puntos[i]?.x)&&Number.isFinite(puntos[i]?.y))){
      const h={x:(puntos[11].x+puntos[12].x)/2,y:(puntos[11].y+puntos[12].y)/2},c={x:(puntos[23].x+puntos[24].x)/2,y:(puntos[23].y+puntos[24].y)/2};
      const torso=Math.hypot((h.x-c.x)*ancho,(h.y-c.y)*alto),razon=torso?Math.abs(puntos[11].x-puntos[12].x)*ancho/torso:Infinity;
      if(razon>=PERFIL.frontal-PERFIL.margen)return aviso(razon>PERFIL.frontal+PERFIL.margen?'FRENTE':'ANGULO_DUDOSO','Parece una foto de frente o de espaldas. Necesito una de lado (de perfil).',razon>PERFIL.frontal+PERFIL.margen);
    }
    const vis=i=>puntos[i]?.visibility??puntos[i]?.v??0;
    const izquierda=[11,23,25,27],derecha=[12,24,26,28],suma=lado=>lado.reduce((total,i)=>total+vis(i),0)/lado.length;
    const cercano=suma(derecha)>=suma(izquierda)?derecha:izquierda;
    if(![0,...cercano].every(visible))return aviso('CUERPO_INCOMPLETO','Se ve solo una parte del cuerpo. Necesito la foto de perfil con el cuerpo completo, de la cabeza a los pies.');
    return null;
  }
  async function detectar(src,turno,opciones){
    vigente(turno);const instancia=await poseLista();vigente(turno);etapa('imagen');
    const im=new Image();im.src=src;await im.decode();vigente(turno);
    const escala=Math.min(1,MAX_LADO/Math.max(im.naturalWidth,im.naturalHeight));
    const lienzo=document.createElement('canvas');lienzo.width=Math.max(1,Math.round(im.naturalWidth*escala));lienzo.height=Math.max(1,Math.round(im.naturalHeight*escala));
    const ctx=lienzo.getContext('2d');if(!ctx)throw new Error('Marca los puntos en la foto.');ctx.drawImage(im,0,0,lienzo.width,lienzo.height);
    instancia.resultado=null;
    try{etapa('inferencia');await conTiempo((async()=>{
      // Pose JS sigue video por defecto; cada foto necesita una localización nueva, no el encuadre anterior.
      await instancia.pose.reset();vigente(turno);instancia.resultado=null;await instancia.pose.send({image:lienzo});
    })(),LIMITES.inferencia,'La detección tardó demasiado.');vigente(turno);}finally{lienzo.width=1;lienzo.height=1;}
    const L=instancia.resultado?.poseLandmarks;if(!L){etapa('no se encontró una persona');return opciones.perfil?{L:null,revisionPerfil:revisarPerfil(null),raw:[]}:null;}
    if(L.length<33||L.some(q=>!Number.isFinite(q.x)||!Number.isFinite(q.y))){etapa('falló al encontrar los puntos');return opciones.perfil?{L:null,raw:[],revisionPerfil:revisarPerfil(null)}:null;}
    etapa('lista');
    const v=i=>L[i].visibility||0;
    const der=(v(14)+v(16)+v(20))>=(v(13)+v(15)+v(19));
    const I=der?{c:24,h:12,co:14,m:16,n:20,p:18,o:8}:{c:23,h:11,co:13,m:15,n:19,p:17,o:7};
    const w=im.width,h=im.height,P=i=>({x:clamp(L[i].x,0,1),y:clamp(L[i].y,0,1)});
    const oreja={x:L[I.o].x*w,y:L[I.o].y*h},nariz={x:L[0].x*w,y:L[0].y*h};
    let ux=oreja.x-nariz.x,uy=oreja.y-nariz.y;const n=Math.hypot(ux,uy)||1;ux/=n;uy/=n;
    let px=uy,py=-ux;if(py>0){px=-px;py=-py;}
    const cab={x:clamp((oreja.x+(CORONILLA.arriba*px+CORONILLA.atras*ux)*n)/w,0,1),y:clamp((oreja.y+(CORONILLA.arriba*py+CORONILLA.atras*uy)*n)/h,0,1)};
    const nud={x:clamp((L[I.n].x+L[I.p].x)/2,0,1),y:clamp((L[I.n].y+L[I.p].y)/2,0,1)};
    const raw=L.map(q=>({x:+q.x.toFixed(4),y:+q.y.toFixed(4),v:+(q.visibility||0).toFixed(2)}));
    return {L:{cadera:P(I.c),hombro:P(I.h),codo:P(I.co),muneca:P(I.m),nudillos:nud,cabeza:cab},mira:nariz.x>oreja.x?'Derecho':'Izquierdo',raw,piesSinApoyo:opciones.perfil?sugerirPiesSinApoyo(L,{contexto:opciones.contexto||''}):null,revisionPerfil:opciones.perfil?revisarPerfil(L,im.naturalWidth,im.naturalHeight):null};
  }
  function detectarPuntos(src,opciones={}){
    if(window.navigator?.onLine===false){etapa('falló al cargar el script');return Promise.reject(new Error('Marca los puntos en la foto.'));}
    const turno=generacion;
    const trabajo=cola.then(()=>conTiempo(detectar(src,turno,opciones),LIMITES.total,'La detección tardó demasiado.').catch(error=>{if(turno===generacion)cancelar();throw error;}));
    cola=trabajo.catch(()=>{});return trabajo;
  }
  // La preparación solo adelanta la descarga: un fallo no cancela ni bloquea futuros intentos.
  function preparar(){if(window.navigator?.onLine===false)return Promise.resolve(false);return poseLista().then(()=>true).catch(()=>false);}
  window.addEventListener?.('pagehide',()=>{liberar();etapa('pagina-cerrada');});
  window.AplomoDeteccion={sugerirPiesSinApoyo,APOYO_PIES,revisarPerfil,PERFIL,detectarPuntos,conTiempo,preparar,cancelar,get disponible(){return typeof WebAssembly!=='undefined';},diagnostico,LIMITES,LIMITE_MS:LIMITES.total,MAX_LADO};
})();
