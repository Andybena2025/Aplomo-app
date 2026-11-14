/* Paso 13E-B: recorrido corporal. Las medidas y los puntos siempre vienen del servidor. */
window.crearRecorridoCorporal=(form,analizador,aviso,renovar,alCambiar,alTerminar)=>{
  const orden=['BRAZO','ANTEBRAZO','MUNECA','GIRO','CUELLO','TRONCO','PIERNAS'];
  const nombres={BRAZO:'Brazo',ANTEBRAZO:'Antebrazo',MUNECA:'Muñeca',GIRO:'Giro de muñeca',CUELLO:'Cuello',TRONCO:'Tronco',PIERNAS:'Piernas'};
  const angulares=['BRAZO','ANTEBRAZO','MUNECA','CUELLO','TRONCO'];
  const ajustes={BRAZO:['hombroElevado','brazoSeparado','brazoApoyado'],ANTEBRAZO:['antebrazoCruza'],MUNECA:['munecaDesviada'],CUELLO:['cuelloGirado','cuelloInclinado'],TRONCO:['troncoGirado','troncoInclinado']};
  const panel=form.querySelector('[data-paso="8"]'),titulo=panel.querySelector('h2'),editor=panel.querySelector('[data-editor-foto]'),medida=panel.querySelector('.measure');
  const cabecera=form.querySelector('[data-cuerpo-cabecera]'),rangos=form.querySelector('[data-cuerpo-rangos]');
  const fotoPanel=medida.firstElementChild,manual=rangos.querySelector('[data-manual-cuerpo]'),angulo=manual.querySelector('input');
  const hoja=form.querySelector('dialog.hoja-cuerpo'),listo=hoja.querySelector('[data-listo-hoja]'),avisoHoja=hoja.querySelector('[data-aviso-hoja]');
  const reducir=()=>matchMedia('(prefers-reduced-motion: reduce)').matches;
  let tareas=[],indice=0,segmento=0,temporizador,autoavance,peticion=0,ocupado=false,opener=null,hojaSegmento=null;
  cabecera.hidden=false;panel.insertBefore(cabecera,titulo);
  panel.querySelector(':scope > p').textContent='Revisa un segmento a la vez. Confirma lo que muestra la foto o elige un mosaico por observación directa.';
  panel.querySelector('[data-analizar-foto]').closest('label').hidden=true;
  editor.querySelector('[data-lado]').closest('.field').hidden=true;
  medida.lastElementChild.hidden=true;rangos.hidden=false;medida.append(rangos);
  const actual=()=>{const d=analizador.datos();if(!d)return null;d.estado.recorrido||={confirmados:{},ajustes:{},elecciones:{},noFoto:{},manualEscrito:{},sugerencias:{}};return {...d,r:d.estado.recorrido,k:orden[segmento]};};
  const radios=k=>[...rangos.querySelectorAll(`[data-rangos="${k}"] input[type="radio"]`)];
  const igual=(k,a,b)=>angulares.includes(k)?Number(a)===Number(b):a===b;
  const elegido=d=>d&&d.r.elecciones[d.k]!==undefined;
  function refrescar(){
    const d=actual();if(!d)return;
    titulo.textContent=d.k==='GIRO'?'¿Cómo está el giro de la muñeca?':d.k==='PIERNAS'?'¿Cómo están las piernas y los pies?':`¿Cómo está ${d.k==='MUNECA'?'la':'el'} ${nombres[d.k].toLowerCase()}?`;
    // Las sugerencias se muestran para confirmar; nunca cuentan como una respuesta confirmada.
    if(d.k==='PIERNAS'&&d.r.elecciones.PIERNAS===undefined&&(form.elements.pies?.checked||form.elements.pies?.value==='si')){d.r.elecciones.PIERNAS='no';d.r.piernasSugeridas=true;}
    form.querySelectorAll('[data-pildora]').forEach(p=>{const k=p.dataset.pildora;p.textContent=(d.r.confirmados[k]?'✓ ':'')+nombres[k];if(k===d.k)p.setAttribute('aria-current','step');else p.removeAttribute('aria-current');});
    form.querySelector('[data-tarea-cuerpo]').textContent=`Foto ${(d.foto.indiceFoto??indice)+1} · Lado ${d.estado.lado==='DERECHO'?'derecho':'izquierdo'} · ${indice+1} de ${tareas.length}`;
    form.querySelector('[data-puntaje-parcial]').textContent=d.r.ultimo?`Puntaje parcial · ${d.r.ultimo}`:'Puntaje parcial · Pendiente';
    form.querySelector('[data-final-pendiente]').textContent='Puntaje final: Pendiente';
    rangos.querySelectorAll('[data-rangos]').forEach(g=>g.hidden=g.dataset.rangos!==d.k);
    for(const radio of radios(d.k))radio.checked=elegido(d)&&igual(d.k,radio.value,d.r.elecciones[d.k]);
    fotoPanel.hidden=!!d.r.noFoto[d.k];
    rangos.querySelector('[data-corregir-cuerpo]').hidden=!angulares.includes(d.k);
    manual.hidden=!d.r.manualAbierto||!angulares.includes(d.k);
    angulo.min=d.k==='ANTEBRAZO'?'0':'-180';angulo.value=d.estado.manual[d.k]??'';
    rangos.querySelector('[data-no-foto]').textContent=d.r.noFoto[d.k]?'Volver a ver la foto':'No se ve en la foto';
    const sug=d.r.sugerencias[d.k];let texto='';
    if(d.k==='PIERNAS'&&d.r.piernasSugeridas&&!d.r.confirmados.PIERNAS)texto='Según lo que marcaste en el puesto: confirma el apoyo de las piernas.';
    else if(sug)texto=(sug.manual?'Corrección manual: ':'Sugerido por la foto: ')+Number(sug.grados).toLocaleString('es-CO',{maximumFractionDigits:2})+'° · revísalo y confirma.';
    else if(angulares.includes(d.k))texto=d.r.noFoto[d.k]?'Elige el rango que observas directamente en el puesto.':'Marca los puntos, detecta la postura o elige un rango por observación directa.';
    form.querySelector('[data-sugerencia-foto]').textContent=texto;
    analizador.destacar(d.k);alCambiar();
  }
  function entrar(){
    clearTimeout(autoavance);const d=actual();if(!d)return;d.r.manualAbierto=false;refrescar();programar();
    titulo.tabIndex=-1;titulo.focus({preventScroll:true});window.scrollTo({top:0,behavior:reducir()?'auto':'smooth'});
  }
  function programar(){clearTimeout(temporizador);temporizador=setTimeout(()=>sondear().catch(e=>aviso(e.message)),140);}
  async function sondear(){
    const d=actual();if(!d)return null;const k=d.k,p=analizador.parametrosActuales();if(!p)return null;
    if(angulares.includes(k)){
      const manual=d.estado.manual[k];if(manual!==undefined&&(manual===''||!Number.isFinite(Number(manual))))return null;
      if(manual===undefined&&d.grupos[k].length!==3)return null;
    }else{const valor=d.r.elecciones[k];if(valor===undefined||!d.r.confirmados[k])return null;p.set('respuesta',valor);}
    p.set('segmento',k);p.set('csrf',form.elements.csrf.value);p.set('visita',form.elements.visita.value);p.set('ajustes_confirmados',d.r.ajustes[k]?'si':'no');
    const num=++peticion,revision=d.estado.revision;
    let respuesta=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin',redirect:'manual'});
    if(respuesta.status===403&&await renovar()){p.set('csrf',form.elements.csrf.value);respuesta=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin',redirect:'manual'});}
    if(!respuesta.ok)throw new Error((respuesta.headers.get('Content-Type')||'').startsWith('text/plain')?await respuesta.text():'No pudimos revisar la medición. Tus respuestas se conservan; inténtalo de nuevo.');
    const html=await respuesta.text(),nodo=new DOMParser().parseFromString(html,'text/html').querySelector('[data-parcial]');
    if(!nodo)throw new Error('No pudimos recibir la medición. Inténtalo de nuevo.');
    if(num!==peticion||revision!==d.estado.revision||actual()?.estado!==d.estado||actual()?.k!==k)return null;
    if(angulares.includes(k)){
      const sug={grados:Number(nodo.dataset.grados),rango:Number(nodo.dataset.rango),manual:nodo.dataset.manual==='true'};
      d.r.sugerencias[k]=sug;
      d.estado.medidas||={};d.estado.medidas[k]={grados:sug.grados,manual:sug.manual};
      if(d.r.elecciones[k]===undefined||!d.r.confirmados[k]&&!d.r.eleccionExplicita?.[k]&&!d.r.manualEscrito[k]&&d.estado.manual[k]===undefined)d.r.elecciones[k]=String(sug.rango);
    }
    if(nodo.dataset.puntos!==''&&d.r.confirmados[k])d.r.ultimo=`${nombres[k]}: ${nodo.dataset.puntos} ${nodo.dataset.puntos==='1'?'punto':'puntos'}`;
    refrescar();analizador.repintar();return nodo;
  }
  function prepararEleccion(d){
    if(!elegido(d))return false;
    if(angulares.includes(d.k)){
      const valor=Number(d.r.elecciones[d.k]),sug=d.r.sugerencias[d.k];
      const conserva=sug&&igual(d.k,sug.rango,valor)&&(d.estado.manual[d.k]!==undefined&&d.r.manualEscrito[d.k]||!d.r.noFoto[d.k]&&d.estado.manual[d.k]===undefined&&d.grupos[d.k].length===3);
      if(!conserva){d.estado.manual[d.k]=String(valor);d.r.manualEscrito[d.k]=false;}
      d.r.confirmados[d.k]=false;analizador.actualizar(d.k);
    }
    return true;
  }
  function avanzar(){
    clearTimeout(autoavance);clearTimeout(temporizador);++peticion;
    if(segmento<orden.length-1)segmento++;
    else if(indice<tareas.length-1){indice++;segmento=0;analizador.seleccionar(indice);}
    else{alTerminar();return;}
    entrar();
  }
  function abrirHoja(){
    const d=actual();if(!d||!prepararEleccion(d))return false;
    hojaSegmento=d.k;opener=document.activeElement;
    hoja.querySelector('#titulo-hoja-cuerpo').textContent=`¿Algo más en ${d.k==='MUNECA'?'la':'el'} ${nombres[d.k].toLowerCase()}?`;
    hoja.querySelectorAll('[data-hoja-segmento]').forEach(g=>{
      const es=g.dataset.hojaSegmento===d.k;g.hidden=!es;
      if(es){for(const input of g.querySelectorAll('[data-cuerpo-ajuste]'))input.checked=!!d.estado.ajustes[input.dataset.cuerpoAjuste];g.querySelector('[data-cuerpo-ninguno]').checked=!!d.r.ajustes[d.k]&&ajustes[d.k].every(k=>!d.estado.ajustes[k]);}
    });
    avisoHoja.hidden=true;actualizarHoja();hoja.showModal();hoja.querySelector('[data-cerrar-hoja]').focus();return false;
  }
  function actualizarHoja(){
    const grupo=hoja.querySelector(`[data-hoja-segmento="${hojaSegmento}"]`);if(!grupo)return false;
    const completa=!!grupo.querySelector('input:checked');listo.setAttribute('aria-disabled',String(!completa||ocupado));
    if(completa)avisoHoja.hidden=true;return completa;
  }
  function cerrarHoja(){hoja.close();avisoHoja.hidden=true;if(opener?.isConnected)opener.focus();}
  hoja.addEventListener('cancel',e=>{e.preventDefault();cerrarHoja();});
  hoja.querySelector('[data-cerrar-hoja]').addEventListener('click',cerrarHoja);
  hoja.addEventListener('keydown',e=>{
    if(e.key!=='Tab')return;const controles=[...hoja.querySelectorAll('button,input')].filter(el=>!el.disabled&&el.getClientRects().length);
    const primero=controles[0],ultimo=controles[controles.length-1];if(e.shiftKey&&document.activeElement===primero){e.preventDefault();ultimo.focus();}else if(!e.shiftKey&&document.activeElement===ultimo){e.preventDefault();primero.focus();}
  });
  hoja.addEventListener('change',e=>{
    const grupo=e.target.closest('[data-hoja-segmento]');if(!grupo)return;
    if(e.target.hasAttribute('data-cuerpo-ninguno')&&e.target.checked)grupo.querySelectorAll('[data-cuerpo-ajuste]').forEach(i=>i.checked=false);
    else if(e.target.checked)grupo.querySelector('[data-cuerpo-ninguno]').checked=false;
    actualizarHoja();
  });
  listo.addEventListener('click',async()=>{
    if(ocupado)return;if(!actualizarHoja()){avisoHoja.hidden=false;return;}
    const d=actual(),grupo=hoja.querySelector(`[data-hoja-segmento="${d.k}"]`);ocupado=true;listo.disabled=true;listo.textContent='Cargando…';
    try{
      for(const input of grupo.querySelectorAll('[data-cuerpo-ajuste]'))d.estado.ajustes[input.dataset.cuerpoAjuste]=input.checked;
      d.r.ajustes[d.k]=true;d.r.confirmados[d.k]=true;d.estado.ajustesConfirmados=angulares.every(k=>d.r.ajustes[k]);
      analizador.actualizar('CONFIRMAR');clearTimeout(temporizador);await sondear();
      if(hoja.open){cerrarHoja();avanzar();}
    }catch(e){avisoHoja.textContent=e instanceof TypeError?'No se pudo conectar. Tus respuestas se conservan; vuelve a intentarlo.':e.message;avisoHoja.hidden=false;}
    finally{ocupado=false;listo.disabled=false;listo.textContent='Listo';actualizarHoja();alCambiar();}
  });
  function seleccionarRango(e){
    if(!e.target.matches('[data-rango-segmento]'))return;clearTimeout(autoavance);const d=actual();d.r.elecciones[d.k]=e.target.value;d.r.eleccionExplicita||={};d.r.eleccionExplicita[d.k]=true;d.r.confirmados[d.k]=false;
    if(d.k==='PIERNAS')d.r.piernasSugeridas=false;
    refrescar();if(!reducir())autoavance=setTimeout(()=>continuar().catch(e=>aviso(e.message)),250);
  }
  rangos.addEventListener('click',seleccionarRango);rangos.addEventListener('change',seleccionarRango);
  async function continuar(){
    const d=actual();if(!d||!elegido(d)||ocupado)return false;
    if(angulares.includes(d.k))return abrirHoja();
    ocupado=true;alCambiar();
    try{if(d.k==='GIRO')d.estado.giro=d.r.elecciones.GIRO;else d.estado.piernas=d.r.elecciones.PIERNAS==='si';d.r.confirmados[d.k]=true;analizador.actualizar('CONFIRMAR');clearTimeout(temporizador);await sondear();avanzar();}
    finally{ocupado=false;alCambiar();}return false;
  }
  rangos.querySelector('[data-corregir-cuerpo]').addEventListener('click',()=>{const d=actual();d.r.manualAbierto=!d.r.manualAbierto;refrescar();if(d.r.manualAbierto)angulo.focus();});
  rangos.querySelector('[data-restaurar-foto]').addEventListener('click',()=>{const d=actual();delete d.estado.manual[d.k];delete d.r.elecciones[d.k];if(d.r.eleccionExplicita)delete d.r.eleccionExplicita[d.k];d.r.manualEscrito[d.k]=false;d.r.noFoto[d.k]=false;analizador.actualizar(d.k);programar();});
  angulo.addEventListener('input',()=>{const d=actual();d.estado.manual[d.k]=angulo.value;d.r.manualEscrito[d.k]=true;delete d.r.elecciones[d.k];analizador.actualizar(d.k);programar();});
  rangos.querySelector('[data-no-foto]').addEventListener('click',()=>{const d=actual();d.r.noFoto[d.k]=!d.r.noFoto[d.k];d.r.confirmados[d.k]=false;analizador.actualizar('OBSERVACION');refrescar();});
  form.addEventListener('aplomo:analisis',()=>{if(!panel.hidden){refrescar();programar();}});
  form.addEventListener('aplomo:foto',()=>{if(!panel.hidden){indice=analizador.datos().indice;segmento=0;entrar();}});
  return {
    preparar(fotos,lado){
      const lados=lado==='AMBOS'?['DERECHO','IZQUIERDO']:[lado];
      tareas=lados.flatMap(l=>fotos.flatMap((f,i)=>{
        if(f.uso!=='MEDIR')return [];f.tareas||={};
        const tarea=f.tareas[l]||={ladoFijo:l};Object.assign(tarea,{id:f.id,indiceFoto:i,vista:f.vista,descripcion:f.descripcion,preview:f.preview,src:f.src,uso:'MEDIR'});return [tarea];
      }));indice=0;segmento=0;return tareas;
    },
    mostrar(){if(tareas.length)entrar();},
    ultimo(){indice=tareas.length-1;segmento=orden.length-1;analizador.seleccionar(indice);},
    atras(){clearTimeout(autoavance);if(segmento>0)segmento--;else if(indice>0){indice--;segmento=orden.length-1;analizador.seleccionar(indice);}else return false;entrar();return true;},
    progreso(){return {actual:indice*7+segmento,total:Math.max(1,tareas.length)*7};},
    respondido(){return !ocupado&&elegido(actual());},
    ocupado(){return ocupado;},
    continuar,
    cancelar(){clearTimeout(autoavance);clearTimeout(temporizador);++peticion;},
    completo(){return tareas.length>0&&tareas.every(t=>orden.every(k=>t.analisis?.recorrido?.confirmados[k]));}
  };
};
