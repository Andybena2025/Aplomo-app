/* Paso 14: revisión por excepción. El servidor sigue midiendo y puntuando. */
(() => {
 const form=document.querySelector('form[data-revision]');if(!form||form.dataset.conversacion)return;
 const $=s=>form.querySelector(s),$$=s=>[...form.querySelectorAll(s)];
 const orden=['BRAZO','ANTEBRAZO','MUNECA','GIRO','CUELLO','TRONCO','PIERNAS'],angulares=orden.filter(k=>!['GIRO','PIERNAS'].includes(k));
 let paso=1,maximo=1,ocupado=false,fotos=[],tareas=[],lado=null,otros=false,extras=0,medicionPendiente=null,temporizador,archivoTipo='FRENTE',fotoVisible=null;
 const esfuerzoPorLado=new Map(),confirmados=new Set(),fuentes=new Map();
 const continuar=Number(form.dataset.continuar||0);
 let evidenciaActiva=null,lecturas=0;
 function anotarFuente(f,k){f.referencias||=new Set();f.referencias.add(fila(k).querySelector('strong').textContent);f.descripcionBase||=f.descripcion;f.descripcion=f.descripcionBase+' · referencia para '+[...f.referencias].join(', ');}
 if(continuar)fotos=$$('[data-foto-id]').map((el,i)=>({id:Number(el.dataset.fotoId),vista:el.dataset.vista,descripcion:el.dataset.descripcion,src:el.dataset.src,preview:el.dataset.src,uso:i===0?'MEDIR':'APOYO'}));
 const memoria=window.crearMemoriaEvaluacion(form);
 function aviso(t){$('[data-error-evaluacion]').hidden=!t;$('[data-error-evaluacion] span').textContent=t;}
 async function renovar(){const r=await fetch(location.href,{credentials:'same-origin',cache:'no-store'});if(!r.ok)return false;const p=new DOMParser().parseFromString(await r.text(),'text/html'),token=p.querySelector('[name="csrf"]')?.value;if(!token||token===form.elements.csrf.value)return false;form.elements.csrf.value=token;return true;}
 const analizador=window.crearAnalizadorAplomo(form,()=>tareas,aviso,renovar);
 // Reutiliza los controles y el lienzo existentes, conservando escala/coordenadas.
 $('[data-foto-revision]').append($('[data-editor-foto] .measure').firstElementChild);
 const fotoCont=$('[data-foto-revision]');$('.wizfoot').append($('[data-confirmar-mediciones]'));
 function actual(){const d=analizador.datos();return d?.foto.ladoFijo===lado?d:null;}
 function delLado(){return tareas.filter(f=>f.ladoFijo===lado);}
 function conFotos(){return !!continuar||$('[name="registro"]:checked')?.value==='CON_FOTOS';}
 function estado(f){return f.analisis;}
 function nombreLado(){return lado==='DERECHO'?'Lado derecho':'Lado izquierdo';}
 function limpiarConfirmacion(){confirmados.delete(lado);actual()?.estado&&(actual().estado.ajustesConfirmados=false);}
 function contextoListo(){return !!form.elements.postura.value.trim()&&form.elements.postura.value.length<=150&&!!$('[name="criterio"]:checked')&&!!$('[name="minutos"]:checked');}
 function completa(){
  if(paso===1)return !!form.elements.puesto.value&&form.elements.acepta.checked&&!!$('[name="registro"]:checked');
  if(paso===2)return contextoListo();
  if(paso===3)return !!form.elements.condiciones_respondidas.value&&form.elements.observaciones.value.length<=255;
  if(paso===4)return !!$('[name="lado_recorrido"]:checked');
  if(paso===5)return confirmados.has(lado);
  if(paso===6)return !!$('[name="esfuerzo_opcion"]:checked')&&!!$('[name="carga_opcion"]:checked');
  return true;
 }
 function botones(){
  const b=$('[data-siguiente]');b.disabled=ocupado||!completa();b.setAttribute('aria-disabled',String(b.disabled));b.hidden=paso===7||paso===5;
  b.textContent=ocupado?'Cargando…':'Continuar →';
  $('[data-atras-evaluacion]').disabled=ocupado;
  for(const g of $$('[data-guardar-evaluacion],[data-guardar-volver]')){g.hidden=paso!==7;g.disabled=ocupado;}
  $('[data-confirmar-mediciones]').disabled=ocupado||lecturas>0||!delLado().length;$('[data-confirmar-mediciones]').hidden=paso!==5;
  $('[data-evaluar-otro]').hidden=!conFotos()||otros||!!continuar;
 }
 function mostrar(n){
  paso=n;maximo=Math.max(maximo,n);$$('[data-paso]').forEach(p=>p.hidden=Number(p.dataset.paso)!==n);
  $('#pasoActual').textContent='Paso '+n+' de 7';$('progress').value=maximo;
  if(n===5){$('[data-lado-titulo]').textContent=nombreLado()+' · Foto de perfil';refrescar();}
  if(n===6){
   $('[data-lado-esfuerzo]').textContent=nombreLado();
   const elegida=esfuerzoPorLado.get(lado),recordada=memoria.esfuerzo(0,lado),previo=elegida||recordada;
   for(const [nombre,valor] of [['esfuerzo_opcion',previo?.actividad],['carga_opcion',previo?.carga]]){
    for(const r of $$('[name="'+nombre+'"]'))r.checked=!!valor&&r.value===valor;
    memoria.marcarRecordada($$('[name="'+nombre+'"]'),elegida?null:valor);
   }
   $('[data-recuerdo-esfuerzo]').hidden=!!elegida||!recordada;
  }
  memoria.aviso(n);botones();aviso('');
  const h=$('[data-paso="'+n+'"] h2');h.tabIndex=-1;h.focus({preventScroll:true});window.scrollTo({top:0,behavior:'instant'});
  form.dispatchEvent(new CustomEvent('aplomo:pantalla',{detail:{paso:n,lado}}));
 }
 function motivo(){form.elements.motivo.value='Momento: '+($('[name="criterio"]:checked')?.value==='MAS_INADECUADA'?'La postura más exigente':'La que más tiempo dura')+'. Postura: '+form.elements.postura.value.trim()+'.';$('[data-momento-revision]').textContent=(continuar?form.dataset.criterio:$('[name="criterio"]:checked')?.value)==='MAS_INADECUADA'?'Mide el momento más exigente.':'Mide la postura habitual.';}
 const puerta=window.crearAccionEvaluacion({
  alCambiar(valor){ocupado=valor;form.dataset.ocupado=String(valor);botones();},
  alError(e){aviso(e instanceof TypeError?'No se pudo conectar. Tus respuestas y fotos se conservan; inténtalo de nuevo.':e.message);}
 });
 const accion=fn=>puerta.ejecutar(fn);
 $('[data-siguiente]').addEventListener('click',()=>accion(async()=>{
  if(!completa())return;
  if(paso===3&&!conFotos()){mostrar(7);$('[data-resultados]').textContent='Sin registro fotográfico. Se conservarán los datos sin fotos ni puntaje RULA.';return;}
  if(paso===4){const elegido=$('[name="lado_recorrido"]:checked').value;lado=elegido==='AMBOS'?'DERECHO':elegido;otros=false;if(continuar)fotos.filter(f=>f.uso==='MEDIR').forEach(f=>f.ladoOrigen=lado);prepararTareas();mostrar(5);return;}
  if(paso===6){
   const actividad=$('[name="esfuerzo_opcion"]:checked').value,carga=$('[name="carga_opcion"]:checked').value;
   esfuerzoPorLado.set(lado,{actividad,carga});
   for(const f of delLado()){const s=estado(f);s.actividadEleccion=actividad;s.actividad=actividad!=='NINGUNA';s.carga=carga;s.actividadConfirmada=true;s.cargaConfirmada=true;s.html=null;s.revision++;}
   await analizador.paso6();mostrar(7);return;
  }
  motivo();mostrar(paso+1);
 }));
 $('[data-atras-evaluacion]').addEventListener('click',()=>{
  if(ocupado)return;
  if(paso===1){
   let p=$('[data-salir-inline]');if(!p){p=document.createElement('div');p.dataset.salirInline='';p.className='aviso';const texto=document.createElement('span');texto.textContent='Los cambios aún no están guardados. ';const a=document.createElement('a');a.className='btn';a.href='/visitas/'+form.elements.visita.value;a.textContent='Salir sin guardar';const b=document.createElement('button');b.type='button';b.className='btn';b.textContent='Seguir evaluando';b.onclick=()=>p.remove();p.append(texto,a,b);form.prepend(p);}return;
  }
  mostrar(paso===7&&!conFotos()?3:paso-1);
 });
 $('[data-cerrar-error]').onclick=()=>aviso('');
 $('[data-cambiar-tarea]').onclick=()=>{$('[data-editar-tarea]').hidden=false;form.elements.postura.focus();};
 $$('[data-tarea]').forEach(b=>b.onclick=()=>{
  const otra=b.dataset.tarea==='Otra';form.elements.postura.value=otra?'':b.dataset.tarea;
  $('[data-tarea-visible]').textContent=otra?'Escribe la tarea':b.dataset.tarea;$('[data-editar-tarea]').hidden=!otra;if(otra)form.elements.postura.focus();botones();
 });
 form.addEventListener('input',e=>{if(e.target.name==='postura')$('[data-tarea-visible]').textContent=e.target.value;motivo();botones();});
 form.addEventListener('change',e=>{
  const el=e.target,condiciones=['escritorio','pantalla','pies','apoyapies','apoyabrazos','regulable'];
  if(el.name==='ninguna'&&el.checked)condiciones.forEach(k=>form.elements[k].checked=false);
  if(condiciones.includes(el.name)&&el.checked)form.elements.ninguna.checked=false;
  form.elements.condiciones_respondidas.value=form.elements.ninguna.checked?'NINGUNA':condiciones.some(k=>form.elements[k].checked)?'SELECCION':'';
  if(el.name==='puesto'){const op=el.selectedOptions[0];form.elements.postura.value=op.dataset.tarea||'';$('[data-tarea-visible]').textContent=form.elements.postura.value;document.querySelector('[data-modo]').textContent=op.dataset.tipo==='ESCRITORIO'?'Oficina':'Planta';}
  if(el.name==='registro'&&!conFotos()){fotos.forEach(f=>URL.revokeObjectURL(f.preview));fotos=[];tareas=[];confirmados.clear();}
  if(el.matches('[data-rango]')){
   const d=actual(),k=el.dataset.rango;if(!d)return;
   if(angulares.includes(k)){d.estado.manual[k]=el.value;d.estado.medidas||={};d.estado.medidas[k]={grados:Number(el.value),manual:true};}
   else if(k==='GIRO')d.estado.giro=el.value;else d.estado.piernas=el.value==='si';
   d.estado.resueltos||={};d.estado.resueltos[k]=!['error','nada','otroPerfil'].includes(d.estado.ia);limpiarConfirmacion();if(evidenciaActiva){fuentes.set(d.foto.indiceFoto+':'+lado+':'+k,evidenciaActiva.vista==='FRENTE'?'Frente':'Detalle');anotarFuente(evidenciaActiva,k);}analizador.actualizar(k);refrescar();
  }
  if(el.matches('[data-ajuste-revision]')){const d=actual();if(d){if(evidenciaActiva){const k=el.closest('[data-revision-segmento]').dataset.revisionSegmento;fuentes.set(d.foto.indiceFoto+':'+lado+':'+k,evidenciaActiva.vista==='FRENTE'?'Frente':'Detalle');anotarFuente(evidenciaActiva,k);}d.estado.ajustes[el.dataset.ajusteRevision]=el.checked;limpiarConfirmacion();analizador.actualizar('AJUSTES');}}
  motivo();botones();
 });
 function prepararTareas(){
  for(const [i,f] of fotos.entries())if(f.uso==='MEDIR'&&f.ladoOrigen===lado&&!tareas.some(t=>t.indiceFoto===i&&t.ladoFijo===lado))tareas.push({...f,indiceFoto:i,ladoFijo:lado});
  const i=tareas.findIndex(t=>t.ladoFijo===lado);if(i>=0){analizador.iniciar();analizador.seleccionar(i);fotoVisible=tareas[i];}
  evidencias();
 }
 function evidencias(){
  const cont=$('[data-evidencias]');cont.replaceChildren();cont.setAttribute('role','tablist');cont.setAttribute('aria-label','Fotos de la postura');
  fotos.forEach((f,i)=>{if(f.ladoOrigen&&f.ladoOrigen!==lado)return;const b=document.createElement('button');b.type='button';b.className='btn';b.setAttribute('role','tab');b.setAttribute('aria-selected',String(i===fotoVisible?.indiceFoto));b.textContent=f.vista==='PERFIL'?'Perfil':f.vista==='FRENTE'?'Frente':f.descripcion;b.onclick=()=>{
   for(const tab of cont.children)tab.setAttribute('aria-selected',String(tab===b));const t=tareas.findIndex(t=>t.indiceFoto===i&&t.ladoFijo===lado);
   if(t>=0){fotoVisible=tareas[t];analizador.seleccionar(t);fotoPrincipal();$('[data-estado-evidencia]').textContent='';}
   else {$('[data-estado-evidencia]').textContent=f.vista==='FRENTE'?'Frente: úsalo para revisar los ajustes de brazo, cuello y tronco.':'Detalle: úsalo para revisar el rango y el giro de la muñeca.';verApoyo(f);}
   refrescar();
  };cont.append(b);});
 }
 function verApoyo(f){evidenciaActiva=f;
  let im=$('[data-imagen-apoyo]');if(!im){im=document.createElement('img');im.dataset.imagenApoyo='';im.className='revision-apoyo';$('[data-foto-revision]').append(im);}
  im.src=f.preview;im.alt=f.descripcion;im.hidden=false;fotoCont.firstElementChild.hidden=true;
  $('[data-estado-evidencia]').textContent+=' Toca Perfil para volver a los puntos.';
 }
 function fotoPrincipal(){evidenciaActiva=null;const im=$('[data-imagen-apoyo]');if(im)im.hidden=true;fotoCont.firstElementChild.hidden=false;}
 $('[data-perfil]').onclick=()=>$('[data-archivo-perfil]').click();
 $$('[data-agregar]').forEach(b=>b.onclick=()=>{archivoTipo=b.dataset.agregar;$('[data-archivo-extra]').click();});
 async function recibir(input,tipo){
  const file=input.files[0];input.value='';if(!file)return;
  if(!conFotos())throw new Error('La persona no permite fotos. No se puede subir ninguna.');
  if(fotos.length>=10)throw new Error('Solo puedes agregar hasta 10 fotos.');
  if(!['image/jpeg','image/png'].includes(file.type)||!file.size||file.size>5*1024*1024)throw new Error('Elige una foto JPG o PNG de hasta 5 MB.');
  const perfil=tipo==='PERFIL',f={ladoOrigen:lado,file,preview:URL.createObjectURL(file),vista:perfil?'PERFIL':tipo==='FRENTE'?'FRENTE':'OTRA',descripcion:perfil?(fotos.some(x=>x.vista==='PERFIL')?'Perfil · otro lado':'Perfil'):tipo==='FRENTE'?'Frente':tipo==='OTRA'?'Detalle de mano y muñeca':'Foto adicional '+(++extras),uso:perfil?'MEDIR':'APOYO'};
  fotos.push(f);$('[data-agregar-foto]').open=false;confirmados.delete(lado);prepararTareas();$('[data-subir-perfil]').hidden=!!delLado().length;fotoPrincipal();refrescar();
  if(!perfil){
   lecturas++;botones();
   $('[data-estado-evidencia]').textContent='Revisando '+f.descripcion.toLowerCase()+'…';
   try{
    const r=await window.AplomoDeteccion.conTiempo(window.AplomoDeteccion.detectarPuntos(f.preview),60000,'La detección tardó demasiado.');
    f.deteccion=r;if(r&&tipo==='OTRA')await medirDetalle(f,r);
    $('[data-estado-evidencia]').textContent=r?'Foto agregada. Revisa sus ajustes; nada se marca solo.':'No se encontró una persona en la foto. Elige el rango a mano.';
    // No se convierten ángulos frontales en flexiones laterales. Evidencia complementaria, no otra evaluación.
    refrescar();
   }catch(_){$('[data-estado-evidencia]').textContent='Foto de apoyo conservada. Elige los rangos a mano.';}finally{lecturas--;botones();}
  }
 }
 $('[data-archivo-perfil]').onchange=e=>recibir(e.target,'PERFIL').catch(e=>aviso(e.message));
 $('[data-archivo-extra]').onchange=e=>recibir(e.target,archivoTipo).catch(e=>aviso(e.message));
 async function medirDetalle(f,r){
  const der=(r.raw[14].v+r.raw[16].v+r.raw[20].v)>=(r.raw[13].v+r.raw[15].v+r.raw[19].v),indices=der?[14,16,20,18]:[13,15,19,17];
  if(indices.some(i=>r.raw[i].v<.5))return;
  const imagen=new Image();imagen.src=f.preview;await imagen.decode();
  const p=new URLSearchParams({csrf:form.elements.csrf.value,visita:form.elements.visita.value,segmento:'MUNECA',lado,ancho:String(imagen.naturalWidth),alto:String(imagen.naturalHeight),MUNECA_modo:'PUNTOS',MUNECA_puntos:[r.L.codo,r.L.muneca,r.L.nudillos].map(q=>q.x.toFixed(6)+','+q.y.toFixed(6)).join(';'),ajustes_confirmados:'no'});
  const respuesta=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin'});if(!respuesta.ok)return;
  const n=new DOMParser().parseFromString(await respuesta.text(),'text/html').querySelector('[data-parcial]');if(!n)return;
  f.medicionMuneca={grados:Number(n.dataset.grados),rango:Number(n.dataset.rango)};
  const d=actual();if(!d)return;d.estado.alternativaMuneca=f;d.estado.conflictoApoyo||={};
  d.estado.conflictoApoyo.MUNECA=!!d.estado.propuestas?.MUNECA&&d.estado.propuestas.MUNECA.rango!==f.medicionMuneca.rango;
  if(d.estado.conflictoApoyo.MUNECA&&d.estado.resueltos)delete d.estado.resueltos.MUNECA;
 }
 function fila(k){return $('[data-revision-segmento="'+k+'"]');}
 function propuesta(d,k){return d.estado.propuestas?.[k];}
 function revisar(d,k){
  if(d.estado.resueltos?.[k])return false;
  if(k==='GIRO')return !d.estado.giro;
  if(k==='PIERNAS')return d.estado.piernas===null;
  const p=propuesta(d,k);return window.AplomoRevisionReglas.requiereRevision({segmento:k,grados:p?.grados,encontrado:!!p,discrepa:!!d.estado.conflictoApoyo?.[k]||!!p&&tareas.some(t=>t!==d.foto&&t.ladoFijo===lado&&t.analisis?.propuestas?.[k]&&t.analisis.propuestas[k].rango!==p.rango)});
 }
 function refrescar(){
  const d=actual();$('[data-foto-revision]').hidden=!d;$('[data-agregar-foto]').hidden=!d||!!continuar;
  if(!d){
   evidenciaActiva=null;fotoVisible=null;$('[data-respaldo]').hidden=true;$('[data-estado-evidencia]').textContent='';
   for(const k of orden){const f=fila(k);f.hidden=false;f.open=false;delete f.dataset.pulsado;f.classList.remove('revision-pulso');f.dataset.pendiente='true';f.querySelector('[data-resumen-segmento]').textContent='Sin medición';f.querySelector('[data-revisar]').hidden=false;f.querySelector('[data-fuente]').textContent='Perfil';for(const x of f.querySelectorAll('input'))x.checked=false;}
   botones();return;
  }
  if(fotoVisible===d.foto&&$('[data-imagen-apoyo]')?.hidden!==false)fotoPrincipal();
  const falla=['error','nada','otroPerfil'].includes(d.estado.ia);
  $('[data-respaldo]').hidden=!falla;$('[data-respaldo]').textContent=d.estado.ia==='otroPerfil'?'Esta foto muestra el otro perfil. Usa otra foto o elige los rangos por observación directa.':'No se encontró una persona en la foto. Marca los puntos a mano o elige un rango. Revisa un segmento a la vez.';
  let primeraPendiente=false;
  for(const k of orden){
   const f=fila(k),p=propuesta(d,k),manual=d.estado.manual[k],valor=k==='GIRO'?d.estado.giro:k==='PIERNAS'?(d.estado.piernas===null?'':d.estado.piernas?'si':'no'):manual??p?.rango;
   const r=[...f.querySelectorAll('[data-rango]')].find(x=>(angulares.includes(k)?Number(x.value)===Number(valor):String(x.value)===String(valor)));for(const x of f.querySelectorAll('[data-rango]'))x.checked=x===r;
   const texto=r?.closest('label').querySelector('.tour-franja>span').textContent||'Elige por observación';
   f.querySelector('[data-resumen-segmento]').textContent=angulares.includes(k)?(manual!==undefined?Number(manual).toLocaleString('es-CO'):p?p.grados.toLocaleString('es-CO',{maximumFractionDigits:2}):'—')+'° · '+texto+(manual===undefined&&p?' · Según la foto':''):texto;
   const pendiente=revisar(d,k);f.dataset.pendiente=String(pendiente);f.querySelector('[data-revisar]').hidden=!pendiente;
   const fuente=fuentes.get(d.foto.indiceFoto+':'+lado+':'+k);f.querySelector('[data-fuente]').textContent=fuente?(angulares.includes(k)&&manual===undefined?'Perfil · ajuste: '+fuente:fuente):'Perfil';
   if(pendiente&&!f.dataset.pulsado){f.dataset.pulsado='si';f.classList.add('revision-pulso');}
   for(const x of f.querySelectorAll('[data-ajuste-revision]'))x.checked=!!d.estado.ajustes[x.dataset.ajusteRevision];
   if(falla){f.hidden=primeraPendiente;if(!d.estado.resueltos?.[k]){primeraPendiente=true;f.open=true;}}else f.hidden=false;
  }
  let alternativa=$('[data-candidata-muneca]');const apoyo=d.estado.alternativaMuneca;
  if(apoyo){
   if(!alternativa){alternativa=document.createElement('button');alternativa.type='button';alternativa.className='btn';alternativa.dataset.candidataMuneca='';fila('MUNECA').querySelector('.revision-opciones').append(alternativa);}
   alternativa.textContent='Usar detalle · '+apoyo.medicionMuneca.grados.toLocaleString('es-CO',{maximumFractionDigits:2})+'°';
   alternativa.onclick=()=>{const actualAhora=actual();if(!actualAhora)return;const s=actualAhora.estado;s.manual.MUNECA=apoyo.medicionMuneca.grados;s.resueltos||={};s.resueltos.MUNECA=true;fuentes.set(actualAhora.foto.indiceFoto+':'+lado+':MUNECA','Detalle');apoyo.descripcion='Detalle de mano y muñeca · referencia de la muñeca';limpiarConfirmacion();analizador.actualizar('MUNECA');refrescar();};
  }else alternativa?.remove();
  botones();
 }
 async function medirPropuestas(){
  const d=actual();if(!d||!d.foto.ancho)return;
  const firma=JSON.stringify([d.estado.puntos,d.estado.manual,d.foto.ancho,d.foto.alto,d.estado.lado]);
  if(d.estado.firmaRevision===firma)return;
  const puntos=JSON.stringify(d.estado.puntos);if(d.estado.firmaPuntos!==puntos){for(const k of angulares)if(d.estado.manual[k]===undefined&&d.estado.resueltos)delete d.estado.resueltos[k];d.estado.firmaPuntos=puntos;}d.estado.firmaRevision=firma;d.estado.propuestas||={};confirmados.delete(lado);
  await Promise.all(angulares.map(async k=>{
   if(d.estado.manual[k]===undefined&&d.grupos[k].length!==3){delete d.estado.propuestas[k];return;}
   const p=analizador.parametrosActuales();p.set('segmento',k);p.set('ajustes_confirmados','no');p.set('csrf',form.elements.csrf.value);p.set('visita',form.elements.visita.value);
   let r=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin'});
   if(r.status===403&&await renovar()){p.set('csrf',form.elements.csrf.value);r=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin'});}
   if(!r.ok){delete d.estado.firmaRevision;throw new Error('No pudimos revisar la medición. Tus respuestas se conservan.');}
   const n=new DOMParser().parseFromString(await r.text(),'text/html').querySelector('[data-parcial]');
   if(d.estado.firmaRevision!==firma||!n)return;
   d.estado.propuestas[k]={grados:Number(n.dataset.grados),rango:Number(n.dataset.rango)};
  }));
  if(actual()?.estado===d.estado)refrescar();
 }
 form.addEventListener('aplomo:analisis',()=>{
  if(paso!==5)return;clearTimeout(temporizador);temporizador=setTimeout(()=>{medicionPendiente=medirPropuestas().catch(e=>aviso(e.message));},120);refrescar();
 });
 $$('[data-revision-segmento]').forEach(f=>f.addEventListener('toggle',()=>{if(f.open){analizador.destacar(f.dataset.revisionSegmento);}}));
 $$('[data-volver-foto]').forEach(b=>b.onclick=()=>{const d=actual(),k=b.dataset.volverFoto;if(!d)return;delete d.estado.manual[k];delete d.estado.resueltos?.[k];delete d.estado.firmaRevision;fuentes.delete(d.foto.indiceFoto+':'+lado+':'+k);limpiarConfirmacion();analizador.actualizar(k);});
 $$('[data-no-visible]').forEach(b=>b.onclick=()=>{const k=b.dataset.noVisible;fila(k).querySelector('[data-ayuda-no-visible]').hidden=false;const d=actual();if(d)fuentes.set(d.foto.indiceFoto+':'+lado+':'+k,'Observación directa');$('[data-agregar-foto]').open=true;refrescar();});
 $$('[data-resolver]').forEach(b=>b.onclick=()=>{
  const d=actual(),k=b.dataset.resolver;if(!d)return;
  const r=fila(k).querySelector('[data-rango]:checked');if(!r){aviso('Elige un rango para '+fila(k).querySelector('strong').textContent+'.');return;}
  d.estado.resueltos||={};d.estado.resueltos[k]=true;fila(k).open=false;limpiarConfirmacion();refrescar();
 });
 $('[data-confirmar-mediciones]').onclick=()=>accion(async()=>{
  if(lecturas)throw new Error('Espera a que termine la revisión de la foto agregada.');if(medicionPendiente)await medicionPendiente;
  const indice=actual()?.indice;
  for(const f of delLado()){
   analizador.seleccionar(tareas.indexOf(f));await medirPropuestas();const d=actual(),pendientes=orden.filter(k=>revisar(d,k));
   if(pendientes.length){fila(pendientes[0]).open=true;refrescar();throw new Error('Revisa las filas señaladas antes de confirmar las mediciones.');}
   d.estado.ajustesConfirmados=true;
  }
  if(indice!==undefined)analizador.seleccionar(indice);confirmados.add(lado);mostrar(6);
 });
 $('[data-evaluar-otro]').onclick=()=>{
  otros=true;lado=lado==='DERECHO'?'IZQUIERDO':'DERECHO';prepararTareas();confirmados.delete(lado);$('[data-subir-perfil]').hidden=!!delLado().length||!!continuar;mostrar(5);
 };
 form.addEventListener('submit',e=>{e.preventDefault();const boton=e.submitter;accion(async()=>{
  if(paso!==7)return;if(!continuar&&(!contextoListo()||!form.elements.acepta.checked||!form.elements.condiciones_respondidas.value))throw new Error('Completa el contexto, el consentimiento y las condiciones antes de guardar.');
  if(conFotos()){if(!fotos.some(f=>f.vista==='PERFIL')||!confirmados.has(lado))throw new Error('Confirma las mediciones antes de guardar.');await analizador.paso6();}
  const data=continuar?new URLSearchParams({csrf:form.elements.csrf.value}):new FormData(form);for(const k of [...data.keys()])if(k.startsWith('revision_'))data.delete(k);
  if(!continuar)data.set('asistente_completo','si');data.set('destino_guardado',boton?.value||'VOLVER');motivo();if(!continuar)data.set('motivo',form.elements.motivo.value);
  if(conFotos()){if(!continuar)fotos.forEach((f,i)=>{data.append('foto_'+i,f.file);data.append('foto_'+i+'_vista',f.vista);data.append('foto_'+i+'_descripcion',f.descripcion);});analizador.solicitudes().forEach((p,i)=>data.append('analisis_'+i,p));}
  const r=await fetch(form.action,{method:'POST',body:data,credentials:'same-origin',redirect:'manual'});
  if(r.status===201){memoria.guardar(tareas);location.assign(r.headers.get('Location'));return;}
  if(r.status===403&&await renovar())throw new Error('El formulario se actualizó. Tus respuestas se conservan; vuelve a guardar.');
  throw new Error((r.headers.get('Content-Type')||'').startsWith('text/plain')?await r.text():'No pudimos guardar. Revisa tu sesión; tus respuestas se conservan.');
 });});
 memoria.restaurar();$('[data-tarea-visible]').textContent=form.elements.postura.value;if(memoria.tareaAnterior()===form.elements.postura.value){const tag=document.createElement('span');tag.className='tag';tag.textContent='Del puesto anterior';$('[data-tarea-visible]').after(tag);}motivo();if(continuar){$('[data-subir-perfil]').hidden=true;$('[data-evaluar-otro]').hidden=true;mostrar(4);}else mostrar(1);
})();
