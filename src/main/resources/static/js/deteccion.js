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
        instancia.pose.setOptions({modelComplexity:COMPLEJIDAD,staticImageMode:true,smoothLandmarks:false,enableSegmentation:false,minDetectionConfidence:.5});
        instancia.pose.onResults(resultado=>{if(!instancia.cerrada)instancia.resultado=resultado;});
        etapa('modelo');await conTiempo(instancia.pose.initialize(),LIMITES.modelo,'El modelo no pudo cargarse a tiempo.');vigente(trabajo.turno);
        MP=instancia;etapa('modelo-listo');return instancia;
      }catch(error){if(instancia)cerrar(instancia);if(trabajo.turno===generacion)fallo();throw error;}
      finally{if(cargando===trabajo)cargando=null;}
    })();cargando=trabajo;return trabajo.promesa;
  }
  async function detectar(src,turno){
    vigente(turno);const instancia=await poseLista();vigente(turno);etapa('imagen');
    const im=new Image();im.src=src;await im.decode();vigente(turno);
    const escala=Math.min(1,MAX_LADO/Math.max(im.naturalWidth,im.naturalHeight));
    const lienzo=document.createElement('canvas');lienzo.width=Math.max(1,Math.round(im.naturalWidth*escala));lienzo.height=Math.max(1,Math.round(im.naturalHeight*escala));
    const ctx=lienzo.getContext('2d');if(!ctx)throw new Error('Marca los puntos en la foto.');ctx.drawImage(im,0,0,lienzo.width,lienzo.height);
    instancia.resultado=null;
    try{etapa('inferencia');await conTiempo(instancia.pose.send({image:lienzo}),LIMITES.inferencia,'La detección tardó demasiado.');vigente(turno);}finally{lienzo.width=1;lienzo.height=1;}
    const L=instancia.resultado?.poseLandmarks;if(!L){etapa('no se encontró una persona');return null;}
    if(L.length<33||L.some(q=>!Number.isFinite(q.x)||!Number.isFinite(q.y))){etapa('falló al encontrar los puntos');return null;}
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
    return {L:{cadera:P(I.c),hombro:P(I.h),codo:P(I.co),muneca:P(I.m),nudillos:nud,cabeza:cab},mira:nariz.x>oreja.x?'Derecho':'Izquierdo',raw};
  }
  function detectarPuntos(src){
    if(window.navigator?.onLine===false){etapa('falló al cargar el script');return Promise.reject(new Error('Marca los puntos en la foto.'));}
    const turno=generacion;
    const trabajo=cola.then(()=>conTiempo(detectar(src,turno),LIMITES.total,'La detección tardó demasiado.').catch(error=>{if(turno===generacion)cancelar();throw error;}));
    cola=trabajo.catch(()=>{});return trabajo;
  }
  // La preparación solo adelanta la descarga: un fallo no cancela ni bloquea futuros intentos.
  function preparar(){if(window.navigator?.onLine===false)return Promise.resolve(false);return poseLista().then(()=>true).catch(()=>false);}
  window.addEventListener?.('pagehide',()=>{liberar();etapa('pagina-cerrada');});
  window.AplomoDeteccion={detectarPuntos,conTiempo,preparar,cancelar,get disponible(){return typeof WebAssembly!=='undefined';},diagnostico,LIMITES,LIMITE_MS:LIMITES.total,MAX_LADO};
})();
