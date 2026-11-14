/* Paso 14: detección del mock, local al navegador. Solo se descargan código y modelo. */
(() => {
  const MP_URL='https://cdn.jsdelivr.net/npm/@mediapipe/pose@0.5.1675469404/';
  const CORONILLA={arriba:1.2,atras:0.9};
  const clamp=(n,a,b)=>Math.max(a,Math.min(b,n));
  const LIMITE_MS=8000,MAX_LADO=1280,COMPLEJIDAD=0;
  let MP=null,cargando=null,scriptLista=null,cola=Promise.resolve(),generacion=0,fallida=false;
  const diagnostico={etapa:'lista',historial:[]};
  function etapa(nombre){diagnostico.etapa=nombre;diagnostico.historial.push({etapa:nombre,ms:Math.round(performance.now())});if(diagnostico.historial.length>40)diagnostico.historial.shift();}
  function cancelar(){fallida=true;generacion++;if(MP)cerrar(MP);cargando=null;etapa('manual');}

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
    scriptLista=conTiempo(carga,LIMITE_MS,'La detección no pudo cargarse a tiempo.').catch(error=>{script.remove();scriptLista=null;throw error;});
    return scriptLista;
  }
  function cerrar(instancia){
    if(MP===instancia)MP=null;
    try{Promise.resolve(instancia.pose.close()).catch(()=>{});}catch(_){/* El marcado manual sigue disponible. */}
  }
  async function poseLista(){
    if(fallida)throw new Error('Marca los puntos en la foto.');
    if(MP)return MP;
    if(cargando)return cargando;
    const turno=generacion;
    cargando=(async()=>{
      await cargarScript();
      const instancia={pose:new window.Pose({locateFile:archivo=>MP_URL+archivo}),resultado:null};
      instancia.pose.setOptions({modelComplexity:COMPLEJIDAD,staticImageMode:true,smoothLandmarks:false,enableSegmentation:false,minDetectionConfidence:.5});
      instancia.pose.onResults(resultado=>{instancia.resultado=resultado;});
      etapa('modelo');try{await conTiempo(instancia.pose.initialize(),LIMITE_MS,'El modelo no pudo cargarse a tiempo.');}
      catch(error){cerrar(instancia);throw error;}
      if(turno!==generacion||fallida){cerrar(instancia);throw new Error('Marca los puntos en la foto.');}MP=instancia;etapa('modelo-listo');return instancia;
    })().finally(()=>{cargando=null;});
    return cargando;
  }
  async function detectar(src){
    const instancia=await poseLista();
    const im=new Image();im.src=src;await im.decode();
    const escala=Math.min(1,MAX_LADO/Math.max(im.naturalWidth,im.naturalHeight));
    const lienzo=document.createElement('canvas');lienzo.width=Math.max(1,Math.round(im.naturalWidth*escala));lienzo.height=Math.max(1,Math.round(im.naturalHeight*escala));
    const ctx=lienzo.getContext('2d');if(!ctx)throw new Error('Marca los puntos en la foto.');ctx.drawImage(im,0,0,lienzo.width,lienzo.height);
    etapa('imagen-lista');instancia.resultado=null;
    try{etapa('inferencia');await instancia.pose.send({image:lienzo});etapa('inferencia-lista');}finally{lienzo.width=1;lienzo.height=1;}

    const L=instancia.resultado?.poseLandmarks;if(!L)return null;
    if(L.length<33||L.some(q=>!Number.isFinite(q.x)||!Number.isFinite(q.y)))return null;
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
    if(fallida||window.navigator?.onLine===false)return Promise.reject(new Error('Marca los puntos en la foto.'));
    const trabajo=cola.then(()=>conTiempo(detectar(src),LIMITE_MS,'Marca los puntos en la foto.').catch(error=>{cancelar();throw error;}));
    cola=trabajo.catch(()=>{});return trabajo;
  }
  function preparar(){if(fallida||window.navigator?.onLine===false)return Promise.resolve(false);return conTiempo(poseLista(),LIMITE_MS,'Marca los puntos en la foto.').then(()=>true).catch(()=>{cancelar();return false;});}
  window.AplomoDeteccion={detectarPuntos,conTiempo,preparar,cancelar,get disponible(){return !fallida&&typeof WebAssembly!=='undefined';},diagnostico,LIMITE_MS,MAX_LADO};
})();
