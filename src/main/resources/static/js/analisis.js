/* RF-017 a RF-021: interacción del lienzo y formularios. Los ángulos y puntos RULA los devuelve Java. */
window.crearAnalizadorAplomo = (form, obtenerFotos, aviso, renovarFormulario) => {
  const marcas=[['cadera','Cadera'],['hombro','Hombro'],['codo','Codo'],['muneca','Muñeca'],['nudillos','Nudillos'],['coronilla','Coronilla']];
  const segmentos=['BRAZO','ANTEBRAZO','MUNECA','CUELLO','TRONCO'];
  const cv=form.querySelector('#lienzo'), ctx=cv.getContext('2d');
  let activa=0, imagen=null, arrastre=null, temporizador=null, numeroPeticion=0;
  const foto=()=>obtenerFotos()[activa];
  const estado=f=>f.analisis||(f.analisis={habilitada:f.vista==='PERFIL',lado:'DERECHO',puntos:{},manual:{},ajustes:{},giro:'MEDIO',piernas:true,actividad:false,carga:'MENOS_2KG_INTERMITENTE',html:null,revision:0});
  const detectarBoton=form.querySelector('[data-detectar-puntos]');
  const estadoDeteccion=form.querySelector('[data-estado-deteccion]');
  const falloDeteccion='No se encontró una persona en la foto. Marca los puntos a mano.';
  function mostrarDeteccion(){
    const f=foto();if(!f)return;const s=estado(f);
    detectarBoton.disabled=s.ia==='cargando'||f.vista==='OTRA';
    detectarBoton.textContent=s.ia==='cargando'?'Detectando puntos…':'Detectar puntos';
    estadoDeteccion.hidden=!s.mensajeIA;estadoDeteccion.textContent=s.mensajeIA||'';
  }
  async function autoDetectar(f,forzar=false){
    if(!f||f.vista==='OTRA')return;
    const s=estado(f);
    if(s.ia==='cargando'||(!forzar&&(f.vista!=='PERFIL'||!s.habilitada||s.ia||Object.keys(s.puntos).length||Object.keys(s.manual).length)))return;
    const revision=s.revision;s.ia='cargando';s.mensajeIA='Detectando puntos…';
    if(f===foto())mostrarDeteccion();
    try{
      const r=await window.AplomoDeteccion.conTiempo(window.AplomoDeteccion.detectarPuntos(f.src||f.preview),60000,'La detección tardó demasiado.');
      if(!obtenerFotos().includes(f))return;
      if(s.revision!==revision||(f===foto()&&arrastre)){s.ia='manual';s.mensajeIA='Se conservaron tus correcciones manuales.';return;}
      if(!r){s.ia='nada';s.mensajeIA=falloDeteccion;return;}
      s.raw=r.raw;s.ia='ok';s.mensajeIA='Puntos detectados automáticamente: revísalos';
      if(f.vista==='PERFIL'){
        s.puntos={...r.L,coronilla:r.L.cabeza};delete s.puntos.cabeza;
        const lado=r.mira==='Derecho'?'DERECHO':'IZQUIERDO';
        if(s.lado!==lado){s.lado=lado;s.mensajeIA+=' · Lado ajustado a '+(lado==='DERECHO'?'Derecho':'Izquierdo')+' según la foto.';}
        s.revision++;s.html=null;
        if(f===foto()){sincronizar();dibujar();await medir(f).catch(e=>aviso(e.message));}
      }
    }catch(_){s.ia='error';s.mensajeIA=falloDeteccion;}
    finally{if(f===foto())mostrarDeteccion();}
  }
  function grupos(f){
    const p=estado(f).puntos;
    const grupo=ks=>ks.every(k=>p[k])?ks.map(k=>p[k]):[];
    return {BRAZO:grupo(['cadera','hombro','codo']),ANTEBRAZO:grupo(['hombro','codo','muneca']),MUNECA:grupo(['codo','muneca','nudillos']),CUELLO:grupo(['cadera','hombro','coronilla']),TRONCO:p.cadera&&p.hombro?[p.cadera,p.hombro,{x:p.cadera.x,y:0}]:[]};
  }
  function completo(f){const s=estado(f),g=grupos(f);return segmentos.every(k=>s.manual[k]!==undefined?s.manual[k]!==''&&Number.isFinite(Number(s.manual[k])):g[k].length===3);}
  function parametros(f,indice){
    const s=estado(f),g=grupos(f),p=new URLSearchParams({foto:String(f.id||indice),lado:s.lado,ancho:String(f.ancho||1),alto:String(f.alto||1),giro:s.giro,piernasApoyadas:s.piernas?'si':'no',actividad:s.actividad?'si':'no',carga:s.carga});
    for(const k of segmentos){p.set(k+'_modo',s.manual[k]!==undefined?'MANUAL':'PUNTOS');p.set(k+'_puntos',g[k].map(q=>q.x.toFixed(6)+','+q.y.toFixed(6)).join(';'));if(s.manual[k]!==undefined)p.set(k+'_grados',s.manual[k]);}
    for(const el of form.querySelectorAll('[data-ajuste]'))p.set(el.dataset.ajuste,s.ajustes[el.dataset.ajuste]?'si':'no');
    return p;
  }
  function siguientePunto(){
    const f=foto();if(!f)return;const s=estado(f),sig=marcas.find(([k])=>!s.puntos[k]);
    form.querySelector('[data-siguiente-punto]').textContent=sig?'Toca: '+sig[1]+' ('+(marcas.indexOf(sig)+1)+' de 6)':'Listo: los 6 puntos están marcados.';
  }
  function dibujar(){
    if(!imagen||!foto())return;
    const f=foto(),s=estado(f),w=cv.clientWidth,h=w*imagen.naturalHeight/imagen.naturalWidth,dpr=window.devicePixelRatio||1;
    cv.width=Math.round(w*dpr);cv.height=Math.round(h*dpr);cv.style.aspectRatio=imagen.naturalWidth+'/'+imagen.naturalHeight;ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,w,h);ctx.drawImage(imagen,0,0,w,h);
    if(w<=0)return;
    const P={};for(const [k]of marcas)if(s.puntos[k])P[k]={x:s.puntos[k].x*w,y:s.puntos[k].y*h};
    const escala=w/600;ctx.lineWidth=Math.max(2,2.4*escala);ctx.strokeStyle='#EDE6D8';ctx.lineCap='round';
    for(const[a,b]of[['cadera','hombro'],['hombro','codo'],['codo','muneca'],['muneca','nudillos'],['hombro','coronilla']])if(P[a]&&P[b]){ctx.beginPath();ctx.moveTo(P[a].x,P[a].y);ctx.lineTo(P[b].x,P[b].y);ctx.stroke();}
    if(P.cadera){ctx.save();ctx.setLineDash([5,6]);ctx.strokeStyle='rgba(201,204,210,.75)';ctx.lineWidth=1.5;ctx.beginPath();ctx.moveTo(P.cadera.x,P.cadera.y);ctx.lineTo(P.cadera.x,P.cadera.y-160*escala);ctx.stroke();ctx.restore();}
    // Arcos del mock: solo dibujan el ángulo recibido de Java, sin medir ni puntuar en el navegador.
    const arco=(v,a,b,texto)=>{
      if(!P[v]||!P[a]||!P[b])return;
      const inicio=Math.atan2(P[a].y-P[v].y,P[a].x-P[v].x),fin=Math.atan2(P[b].y-P[v].y,P[b].x-P[v].x);let recorrido=fin-inicio;
      while(recorrido>Math.PI)recorrido-=2*Math.PI;while(recorrido<-Math.PI)recorrido+=2*Math.PI;
      const radio=Math.max(15,26*escala);ctx.save();ctx.strokeStyle='#C9CCD2';ctx.setLineDash([3,3]);ctx.lineWidth=1.6;ctx.beginPath();ctx.arc(P[v].x,P[v].y,radio,inicio,inicio+recorrido,recorrido<0);ctx.stroke();ctx.setLineDash([]);
      const medio=inicio+recorrido/2,x=P[v].x+Math.cos(medio)*(radio+16),y=P[v].y+Math.sin(medio)*(radio+16);ctx.font='600 12px Inter, sans-serif';ctx.textAlign='center';ctx.textBaseline='middle';const ancho=ctx.measureText(texto).width+10;ctx.fillStyle='rgba(11,11,12,.85)';ctx.fillRect(x-ancho/2,y-9,ancho,18);ctx.fillStyle='#EDE6D8';ctx.fillText(texto,x,y);ctx.restore();
    };
    const texto=k=>s.medidas&&s.medidas[k]?Number(s.medidas[k].grados).toFixed(1)+'°'+(s.medidas[k].manual?'*':''):null;
    if(texto('BRAZO'))arco('hombro','cadera','codo',texto('BRAZO'));
    if(P.hombro&&P.codo&&P.muneca){P._e1={x:P.codo.x+(P.codo.x-P.hombro.x)*.4,y:P.codo.y+(P.codo.y-P.hombro.y)*.4};if(texto('ANTEBRAZO'))arco('codo','_e1','muneca',texto('ANTEBRAZO'));}
    if(P.codo&&P.muneca&&P.nudillos){P._e2={x:P.muneca.x+(P.muneca.x-P.codo.x)*.4,y:P.muneca.y+(P.muneca.y-P.codo.y)*.4};if(texto('MUNECA'))arco('muneca','_e2','nudillos',texto('MUNECA'));}
    if(P.cadera&&P.hombro){P._e3={x:P.hombro.x+(P.hombro.x-P.cadera.x)*.4,y:P.hombro.y+(P.hombro.y-P.cadera.y)*.4};P._v={x:P.cadera.x,y:P.cadera.y-100};if(texto('CUELLO'))arco('hombro','_e3','coronilla',texto('CUELLO'));if(texto('TRONCO'))arco('cadera','_v','hombro',texto('TRONCO'));}
    for(const[k,t]of marcas)if(P[k]){const p=P[k];if(arrastre===k){ctx.beginPath();ctx.arc(p.x,p.y,16*escala,0,Math.PI*2);ctx.fillStyle='rgba(237,230,216,.18)';ctx.fill();}ctx.beginPath();ctx.arc(p.x,p.y,Math.max(5,6.5*escala),0,Math.PI*2);ctx.fillStyle='#0B0B0C';ctx.fill();ctx.strokeStyle='#EDE6D8';ctx.lineWidth=2.4;ctx.stroke();ctx.font='12px Inter, sans-serif';ctx.fillStyle='#EDE6D8';ctx.fillText(t,Math.min(p.x+10,w-65),Math.max(15,p.y-10));}
  }
  function pintarMedidas(f){
    if(f!==foto())return;const s=estado(f),pagina=s.html?new DOMParser().parseFromString(s.html,'text/html'):null;
    s.medidas={};
    for(const fila of form.querySelectorAll('[data-segmento]')){
      const k=fila.dataset.segmento,m=pagina&&pagina.querySelector('[data-medicion="'+k+'"]');
      if(m)s.medidas[k]={grados:m.dataset.grados,manual:m.textContent.includes('manual')};
      fila.querySelector('[data-grados]').textContent=m?Number(m.dataset.grados).toFixed(2)+'°':'—';fila.querySelector('[data-puntos]').textContent=m?m.dataset.puntos+' pts':'—';
    }
    const parcial=form.querySelector('[data-vista-parcial]');parcial.replaceChildren();
    if(pagina){const pill=pagina.querySelector('.pill');parcial.append('Vista previa: ',pill.cloneNode(true));}else parcial.textContent='Completa los ángulos para ver la vista previa.';
    dibujar();
  }
  function sincronizar(){
    const f=foto();if(!f)return;const s=estado(f);
    form.querySelector('[data-analizar-foto]').checked=s.habilitada;form.querySelector('[data-editor-foto]').hidden=!s.habilitada;
    form.querySelector('[data-lado]').value=s.lado;
    for(const fila of form.querySelectorAll('[data-segmento]')){const k=fila.dataset.segmento,manual=s.manual[k]!==undefined;fila.querySelector('[data-manual]').checked=manual;fila.querySelector('[data-angulo-manual]').hidden=!manual;fila.querySelector('[data-angulo-manual]').value=manual?s.manual[k]:'';}
    for(const el of form.querySelectorAll('[data-ajuste]')){el.checked=!!s.ajustes[el.dataset.ajuste];el.closest('.opt').classList.toggle('on',el.checked);}
    form.querySelector('[data-giro]').value=s.giro;form.querySelector('[data-piernas]').checked=s.piernas;form.querySelector('[data-actividad]').checked=s.actividad;form.querySelector('[data-carga]').value=s.carga;
    siguientePunto();pintarMedidas(f);mostrarDeteccion();
  }
  function cargarFoto(){
    const f=foto();if(!f)return;imagen=null;const img=new Image();
    img.onload=()=>{if(f!==foto())return;imagen=img;f.ancho=img.naturalWidth;f.alto=img.naturalHeight;dibujar();autoDetectar(f);};
    img.onerror=()=>aviso('No pudimos abrir la foto. Comprueba tu sesión y vuelve a seleccionarla sin cerrar este formulario.');img.src=f.src||f.preview;
  }
  function tabs(){
    for(const destino of ['[data-tabs-fotos]','[data-tabs-ajustes]']){
      const cont=form.querySelector(destino);cont.replaceChildren();
      obtenerFotos().forEach((f,i)=>{const b=document.createElement('button');b.type='button';b.textContent='Foto '+(i+1)+' · '+(f.vista==='PERFIL'?'Perfil':f.vista==='FRENTE'?'Frente':'Detalle');b.classList.toggle('on',i===activa);b.addEventListener('click',()=>{activa=i;tabs();sincronizar();cargarFoto();});cont.append(b);});
    }
  }
  async function medir(f,exigir=false){
    const s=estado(f);if(!s.habilitada||!completo(f)){if(exigir)throw new Error('Completa los ángulos de la foto '+(obtenerFotos().indexOf(f)+1)+': marca sus puntos o escribe los cinco ángulos.');return;}
    const revision=s.revision,peticion=++numeroPeticion,p=parametros(f,obtenerFotos().indexOf(f));p.set('csrf',form.elements.csrf.value);p.set('visita',form.elements.visita.value);for(const k of ['escritorio','pantalla','pies','apoyapies','apoyabrazos','regulable']){const e=form.elements[k];p.set(k,e?.type==='checkbox'?(e.checked?'si':'no'):(e?.value||'no'));}
    const terminarProgreso=window.AplomoProgreso?.iniciar('Calculando el resultado…');
    try{
    let r=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin',redirect:'manual'});
    if(r.status===403 && await renovarFormulario()){p.set('csrf',form.elements.csrf.value);r=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin',redirect:'manual'});}
    if(!r.ok){const texto=(r.headers.get('Content-Type')||'').startsWith('text/plain')?await r.text():'No se pudo calcular. Comprueba tu sesión en otra pestaña y vuelve aquí; tus datos se conservan.';throw new Error(texto);}
    const html=await r.text();if(revision!==s.revision)return;if(!html.includes('data-calculo'))throw new Error('No pudimos recibir el resultado. Vuelve a intentarlo.');s.html=html;pintarMedidas(f);
    }catch(error){
      if(error instanceof TypeError)throw new Error('No se pudo conectar. Tus puntos y ángulos se conservan; revisa la conexión e inténtalo de nuevo.');
      throw error;
    }finally{terminarProgreso?.();}
  }
  function cambiado(){
    const f=foto();if(!f)return;const s=estado(f);s.revision++;s.html=null;pintarMedidas(f);siguientePunto();dibujar();clearTimeout(temporizador);
    temporizador=setTimeout(()=>medir(f).catch(e=>aviso(e.message)),180);
  }
  const posicion=e=>{const r=cv.getBoundingClientRect();return{x:Math.max(0,Math.min(1,(e.clientX-r.left)/r.width)),y:Math.max(0,Math.min(1,(e.clientY-r.top)/r.height))};};
  cv.addEventListener('pointerdown',e=>{
    if(!foto()||!imagen)return;e.preventDefault();const s=estado(foto()),p=posicion(e),r=cv.getBoundingClientRect(),dedo=e.pointerType!=='mouse';let cercano=null,distancia=Infinity;
    for(const[k]of marcas)if(s.puntos[k]){const q=s.puntos[k],d=Math.hypot((q.x-p.x)*r.width,(q.y-p.y)*r.height);if(d<distancia){distancia=d;cercano=k;}}
    const sig=marcas.find(([k])=>!s.puntos[k]);
    if(cercano&&(distancia<(dedo?44:22)||(!sig&&distancia<(dedo?90:40)))){arrastre=cercano;try{cv.setPointerCapture(e.pointerId);}catch(_){}dibujar();return;}
    if(!sig)return;s.puntos[sig[0]]=p;cambiado();
  });
  cv.addEventListener('pointermove',e=>{if(!arrastre)return;e.preventDefault();estado(foto()).puntos[arrastre]=posicion(e);dibujar();});
  function terminar(){if(!arrastre)return;arrastre=null;cambiado();}
  cv.addEventListener('pointerup',terminar);cv.addEventListener('pointercancel',terminar);cv.addEventListener('lostpointercapture',terminar);cv.addEventListener('contextmenu',e=>e.preventDefault());
  cv.addEventListener('touchmove',e=>{if(arrastre)e.preventDefault();},{passive:false});new ResizeObserver(()=>dibujar()).observe(cv.parentElement);
  form.querySelector('[data-puntos-deshacer]').addEventListener('click',()=>{const s=estado(foto()),k=[...marcas].reverse().find(([k])=>s.puntos[k]);if(k){delete s.puntos[k[0]];cambiado();}});
  form.querySelector('[data-puntos-borrar]').addEventListener('click',()=>{estado(foto()).puntos={};cambiado();});
  detectarBoton.addEventListener('click',()=>autoDetectar(foto(),true));
  form.querySelector('[data-analizar-foto]').addEventListener('change',e=>{estado(foto()).habilitada=e.target.checked;sincronizar();dibujar();});
  form.querySelector('[data-lado]').addEventListener('change',e=>{estado(foto()).lado=e.target.value;cambiado();});
  for(const fila of form.querySelectorAll('[data-segmento]')){
    const k=fila.dataset.segmento;fila.querySelector('[data-manual]').addEventListener('change',e=>{const s=estado(foto());if(e.target.checked)s.manual[k]='';else delete s.manual[k];sincronizar();cambiado();});
    fila.querySelector('[data-angulo-manual]').addEventListener('input',e=>{estado(foto()).manual[k]=e.target.value;cambiado();});
  }
  for(const el of form.querySelectorAll('[data-ajuste]'))el.addEventListener('change',()=>{estado(foto()).ajustes[el.dataset.ajuste]=el.checked;el.closest('.opt').classList.toggle('on',el.checked);cambiado();});
  for(const[selector,k]of[['[data-giro]','giro'],['[data-piernas]','piernas'],['[data-actividad]','actividad'],['[data-carga]','carga']])form.querySelector(selector).addEventListener('change',e=>{estado(foto())[k]=e.target.type==='checkbox'?e.target.checked:e.target.value;cambiado();});
  return {
    iniciar(){const fs=obtenerFotos();fs.forEach(estado);if(fs.length&&!fs.some(f=>estado(f).habilitada))estado(fs[0]).habilitada=true;activa=Math.min(activa,Math.max(0,fs.length-1));tabs();sincronizar();cargarFoto();},
    async paso5(){const fs=obtenerFotos().filter(f=>estado(f).habilitada);if(!fs.length)throw new Error('Selecciona al menos una foto para analizar.');for(const f of fs)await medir(f,true);if(!estado(foto()).habilitada)activa=obtenerFotos().indexOf(fs[0]);tabs();sincronizar();},
    async paso6(){await this.paso5();const destino=form.querySelector('[data-resultados]');destino.replaceChildren();for(const[f,i]of obtenerFotos().map((f,i)=>[f,i]).filter(([f])=>estado(f).habilitada)){const bloque=document.createElement('div');bloque.className='stack resultado-por-foto';const titulo=document.createElement('h3');titulo.textContent='Foto '+(i+1)+' · '+(estado(f).lado==='DERECHO'?'Lado derecho':'Lado izquierdo');bloque.append(titulo);const contenido=new DOMParser().parseFromString(estado(f).html,'text/html').querySelector('[data-calculo]');bloque.append(contenido);destino.append(bloque);}},
    solicitudes(){return obtenerFotos().flatMap((f,i)=>estado(f).habilitada?[parametros(f,i).toString()]:[]);}
  };
};
