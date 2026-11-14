/* Paso 15 · Una foto, una conversación. Las mediciones y el puntaje los calcula Java. */
(() => {
 const form=document.querySelector('form[data-conversacion]');if(!form)return;
 const $=s=>form.querySelector(s),$$=s=>[...form.querySelectorAll(s)],valor=n=>form.elements[n]?.value||'';
 const angulos=['BRAZO','ANTEBRAZO','CUELLO','TRONCO'],corporales=['hombroElevado','brazoSeparado','brazoApoyado','antebrazoCruza','munecaDesviada'];
 const respuestas=new Map(),bancos=new Map(),esfuerzos=new Map();let fotos=[],tareas=[],lado=null,paso=1,maximo=1,ocupada=false,abierta=null,peticion=0,leyendo=null,medirMuneca=false,dosLados=false;
 const existente=Number(form.dataset.continuar||0),clave='aplomo:campo:'+valor('cuenta_recuerdo')+':'+valor('visita');let anterior=null;
 try{anterior=JSON.parse(sessionStorage.getItem(clave));}catch(_){}
 function aviso(t){$('[data-error-evaluacion]').hidden=!t;$('[data-error-evaluacion] span').textContent=t;}
 async function renovar(){const r=await fetch(location.href,{cache:'no-store',credentials:'same-origin'});if(!r.ok)return false;const t=new DOMParser().parseFromString(await r.text(),'text/html').querySelector('[name="csrf"]')?.value;if(!t||t===valor('csrf'))return false;form.elements.csrf.value=t;return true;}
 const analizador=window.crearAnalizadorAplomo(form,()=>tareas,aviso,renovar);
 $('[data-foto-revision]').append($('[data-editor-foto] .measure').firstElementChild);
 function actual(){const d=analizador.datos();return d?.foto.ladoFijo===lado?d:null;}
 const fotoActual=()=>tareas.find(f=>f.ladoFijo===lado);
 const conFotos=()=>!!existente||valor('registro')==='CON_FOTOS';
 function contexto(){
  const puesto=form.elements.puesto,op=puesto.selectedOptions?.[0]||puesto,nombre=op?.dataset.tarea||'Puesto de trabajo';
  form.elements.postura.value=Array.from(nombre+' · '+(!conFotos()&&paso===9?'sin fotos':dosLados?'ambos lados':lado==='DERECHO'?'lado derecho':lado==='IZQUIERDO'?'lado izquierdo':'lado por elegir')).slice(0,150).join('');
  $('[data-tarea-visible]').textContent=valor('postura');form.elements.minutos.value=valor('tiempo_campo');
  if(!existente)form.elements.criterio.value=$('[data-momento="'+valor('momento_campo')+'"]')?.dataset.criterio||'';
  form.elements.motivo.value=valor('criterio')==='MAYOR_EXPOSICION'?'La postura que más repite.':'El momento que señala el trabajador.';
 }
 function preguntas(panel=$('[data-paso="'+paso+'"]')){return [...panel.querySelectorAll('[data-pregunta]')].filter(q=>!q.dataset.cuando||valor(q.dataset.cuando)==='si');}
 function poner(n,v,origen='elección'){
  form.elements[n].value=String(v);respuestas.set(n,{valor:String(v),origen});
  const q=$('[data-pregunta="'+n+'"]');if(!q)return;const opcion=[...q.querySelectorAll('[data-respuesta]')].find(b=>Number.isFinite(Number(v))&&Number.isFinite(Number(b.dataset.valor))?Number(b.dataset.valor)===Number(v):b.dataset.valor===String(v));
  const texto=n==='muneca_campo'&&v==='FOTO'?'Medida en el perfil · '+(actual()?.estado.propuestas?.MUNECA?.grados?.toLocaleString('es-CO',{maximumFractionDigits:2})||'—')+'°':opcion?.querySelector('span')?.textContent||String(v);q.querySelector('.campo-respuesta span').textContent=texto+' ✓';
  q.querySelectorAll('[data-respuesta]').forEach(b=>b.setAttribute('aria-pressed',String(b===opcion)));
  q.querySelector('[data-origen]')?.remove();if(origen==='foto'||origen==='anterior'){const tag=document.createElement('small');tag.className='tag';tag.dataset.origen='';tag.textContent=origen==='foto'?'Según la foto · confirma al seguir':'Del puesto anterior · revísala';q.querySelector('.campo-respuesta').after(tag);}
 }
 function desplegar(n){abierta=n;$$('[data-pregunta]').forEach(q=>{const si=q.dataset.pregunta===n;q.querySelector('.campo-lista').hidden=!si;q.querySelector('[data-abrir]').setAttribute('aria-expanded',String(si));});}
 function faltante(){
  if(paso===1){if(!valor('puesto'))return 'puesto de trabajo';if(!form.elements.acepta.checked)return 'consentimiento en papel';}
  if(paso===3){if(!valor('condiciones_respondidas'))return 'condiciones del puesto';if(valor('observaciones').length>255)return 'observaciones: máximo 255 caracteres';return '';}
  if(paso===5){const d=actual();if(!d?.foto.ancho)return 'foto de perfil';}
  const q=preguntas().find(q=>!valor(q.dataset.pregunta));return q?q.querySelector('em').textContent.replace(/[“”]/g,''):'';
 }
 function dibujarPreguntas(abrirSiguiente=false){
  const panel=$('[data-paso="'+paso+'"]'),qs=preguntas(panel);let falta=false;
  for(const q of panel.querySelectorAll('[data-pregunta]')){if(!qs.includes(q)){q.hidden=true;continue;}q.hidden=falta;if(!valor(q.dataset.pregunta))falta=true;}
  if(abrirSiguiente){const siguiente=qs.find(q=>!q.hidden&&!valor(q.dataset.pregunta));desplegar(siguiente?.dataset.pregunta||null);}
  const faltaTexto=faltante(),seguir=$('[data-siguiente]');seguir.hidden=paso===9||!!faltaTexto;seguir.disabled=ocupada;seguir.textContent=ocupada?'Cargando…':paso===5?'Confirmar ángulos y seguir':'Seguir →';
  $('[data-falta]').textContent=ocupada?'Un momento…':faltaTexto?'Falta responder: '+faltaTexto:'';
  $('[data-atras-evaluacion]').disabled=ocupada;for(const b of $$('[data-guardar-evaluacion],[data-guardar-volver]')){b.hidden=paso!==9;b.disabled=ocupada;}
  $('[data-evaluar-otro]').hidden=!conFotos()||dosLados||!!existente;
 }
 function cambiarLado(nuevo){
  if(lado===nuevo)return;
  if(lado)bancos.set(lado,new Map(respuestas));lado=nuevo;
  const banco=bancos.get(lado);
  for(const q of $$('[data-pregunta]'))if(Number(q.closest('[data-paso]').dataset.paso)>=5){
   const n=q.dataset.pregunta,previa=banco?.get(n);form.elements[n].value='';respuestas.delete(n);q.querySelector('.campo-respuesta span').textContent='Elige una opción';q.querySelector('[data-origen]')?.remove();q.querySelectorAll('[data-respuesta]').forEach(b=>b.setAttribute('aria-pressed','false'));if(previa)poner(n,previa.valor,previa.origen);
  }
  $$('[data-angulo]').forEach(e=>e.textContent='Sin medir');const i=tareas.findIndex(f=>f.ladoFijo===lado);if(i>=0){analizador.seleccionar(i);delete tareas[i].analisis.firmaCampo;leyendo=medir().catch(e=>aviso(e.message));}
 }
 function mostrar(n){
  paso=n;maximo=Math.max(maximo,n);$$('[data-paso]').forEach(s=>s.hidden=Number(s.dataset.paso)!==n);
  $('[data-titulo-fijo]').textContent=$('[data-paso="'+n+'"] h2').textContent;$('#pasoActual').textContent='Paso '+n+' de 9';$('progress').value=maximo;
  $$('[data-etapa]').forEach(e=>{if(Number(e.dataset.etapa)===n)e.setAttribute('aria-current','step');else e.removeAttribute('aria-current');});
  $('[data-lado-titulo]').textContent=!conFotos()&&n===9?'Sin fotos':lado==='DERECHO'?'Lado derecho':lado==='IZQUIERDO'?'Lado izquierdo':'Lado por elegir';
  const principal=fotoActual(),frente=fotos.find(f=>f.ladoOrigen===lado&&f.vista==='FRENTE');$$('[data-foto-conversacion]').forEach(im=>{im.src=(Number(im.closest('[data-paso]').dataset.paso)===7&&frente?frente.preview:principal?.preview)||'/static/img/muestra_perfil.jpg';});
  if(n===8&&!esfuerzos.has(lado)&&Array.isArray(anterior?.esfuerzos)){const previa=anterior.esfuerzos.find(([l])=>l===lado)?.[1];if(previa){for(const campo of ['peso_campo','esfuerzo_opcion','golpe_campo'])if(!valor(campo)&&previa[campo])poner(campo,previa[campo],'anterior');$('[data-recuerdo-esfuerzo]').hidden=false;}}
  if(n===5){$('[data-foto-revision]').hidden=!principal;$('[data-medidas-compactas]').hidden=!principal?.ancho;$('[data-subir-perfil]').hidden=!!principal;}
  contexto();aviso('');dibujarPreguntas(true);const h=$('[data-paso="'+n+'"] h2');h.tabIndex=-1;h.focus({preventScroll:true});window.scrollTo({top:0,behavior:'instant'});
 }
 const puerta=window.crearAccionEvaluacion({alCambiar(v){ocupada=v;form.dataset.ocupado=String(v);dibujarPreguntas();},alError(e){aviso(e instanceof TypeError?'No pudimos conectar. Tus respuestas se conservan; intenta de nuevo.':e.message);}});
 function trasladar(){const d=actual();if(!d)return;const s=d.estado;for(const k of corporales)s.ajustes[k]=valor(k)==='si';
  for(const k of ['cuello','tronco']){const respuesta=valor(k+'_campo'),tipo=valor(k+'_tipo');const mapa=$('[data-orientacion="'+k+'"][data-tipo="'+tipo+'"]');s.ajustes[k+'Girado']=respuesta==='si'&&mapa?.dataset.girada==='true';s.ajustes[k+'Inclinado']=respuesta==='si'&&mapa?.dataset.inclinada==='true';}
  if(valor('muneca_campo')&&valor('muneca_campo')!=='FOTO')s.manual.MUNECA=Number(valor('muneca_campo'));else if(valor('muneca_campo')==='FOTO')delete s.manual.MUNECA;
  s.giro=valor('giro_campo');s.piernas=valor('piernas_campo')?valor('piernas_campo')==='si':null;s.html=null;s.revision++;
 }
 $$('[data-abrir]').forEach(b=>b.onclick=()=>{desplegar(abierta===b.dataset.abrir?null:b.dataset.abrir);});
 $$('[data-respuesta]').forEach(b=>b.onclick=()=>{
  const n=b.dataset.respuesta;if(ocupada)return;poner(n,b.dataset.valor);contexto();
  if(n.startsWith('rango_')){const k=n.slice(6),d=actual();if(d){d.estado.manual[k]=Number(b.dataset.valor);analizador.actualizar(k);}}
  if(n==='registro'&&b.dataset.valor==='CON_FOTOS')window.AplomoDeteccion.preparar();
  if(n==='registro'&&b.dataset.valor==='SIN_REGISTRO_FOTOGRAFICO'&&!existente){fotos.forEach(f=>f.file&&URL.revokeObjectURL(f.preview));fotos=[];tareas=[];bancos.clear();}
  trasladar();dibujarPreguntas(true);const siguiente=abierta?$('[data-abrir="'+abierta+'"]'):$('[data-siguiente]');if(siguiente&&!siguiente.hidden)siguiente.focus({preventScroll:false});
 });
 form.addEventListener('change',e=>{const condiciones=['escritorio','pantalla','pies','apoyapies','apoyabrazos','regulable'];const el=e.target;if(el?.name==='ninguna'&&el.checked)condiciones.forEach(k=>form.elements[k].checked=false);if(condiciones.includes(el?.name)&&el.checked)form.elements.ninguna.checked=false;form.elements.condiciones_respondidas.value=form.elements.ninguna.checked?'NINGUNA':condiciones.some(k=>form.elements[k].checked)?'SELECCION':'';contexto();dibujarPreguntas();});
 form.addEventListener('input',()=>dibujarPreguntas());
 $('[data-siguiente]').onclick=()=>puerta.ejecutar(async()=>{
  if(faltante())return;
  if(paso===3&&!conFotos()){form.elements.postura.value=valor('postura').replace('lado por elegir','sin fotos');$('[data-resultados]').textContent='Se guardará sin fotos ni puntaje.';mostrar(9);return;}
  if(paso===4){cambiarLado(valor('lado_recorrido')==='AMBOS'?'DERECHO':valor('lado_recorrido'));contexto();if(existente)activarExistente();mostrar(5);return;}
  if(paso===5){if(leyendo)await leyendo;if(faltante())return;if(medirMuneca){medirMuneca=false;mostrar(6);return;}}
  if(paso===6||paso===7)trasladar();
  if(paso===8){const peso=valor('peso_campo'),act=valor('esfuerzo_opcion'),golpe=valor('golpe_campo');const carga=$('[data-mapeos] [data-peso="'+peso+'"][data-actividad="'+act+'"][data-golpe="'+golpe+'"]')?.dataset.carga;if(!carga)throw new Error('Responde el peso, la frecuencia y los golpes.');form.elements.carga_opcion.value=carga;const d=actual();trasladar();Object.assign(d.estado,{actividadEleccion:act,actividad:act!=='NINGUNA',carga,actividadConfirmada:true,cargaConfirmada:true,ajustesConfirmados:true});esfuerzos.set(lado,{peso_campo:peso,esfuerzo_opcion:act,golpe_campo:golpe});await analizador.paso6();mostrar(9);return;}
  mostrar(paso+1);
 });
 $('[data-atras-evaluacion]').onclick=()=>{if(ocupada)return;if(paso===1){let p=$('[data-salir-inline]');if(!p){p=document.createElement('p');p.dataset.salirInline='';p.className='aviso';const a=document.createElement('a');a.className='btn';a.href=existente?'/evaluaciones/'+existente:'/visitas/'+valor('visita');a.textContent='Salir sin guardar';p.append('Tus cambios aún no están guardados. ',a);form.prepend(p);}return;}mostrar(paso===9&&!conFotos()?3:paso-1);};
 $('[data-cerrar-error]').onclick=()=>aviso('');
 function iniciarFoto(f){tareas.push({...f,indiceFoto:fotos.indexOf(f),ladoFijo:lado});analizador.iniciar();analizador.seleccionar(tareas.length-1);$('[data-foto-revision]').hidden=false;$('[data-subir-perfil]').hidden=true;$('[data-falta]').textContent='Preparando la foto…';}
 function activarExistente(){if(fotoActual())return;const f=fotos.find(f=>f.vista==='PERFIL')||fotos[0];if(f){f.uso='MEDIR';f.ladoOrigen=lado;iniciarFoto(f);}}
 async function recibir(file,tipo){if(!file)return;window.AplomoFotosCampo.validar(file,conFotos(),fotos.length);const perfil=tipo==='PERFIL',f={file,preview:URL.createObjectURL(file),vista:tipo,descripcion:perfil?(tareas.length?'Perfil · otro lado':'Perfil'):tipo==='FRENTE'?'Frente':'Detalle de mano y muñeca · solo evidencia',uso:perfil?'MEDIR':'APOYO',ladoOrigen:lado};fotos.push(f);if(perfil){iniciarFoto(f);return;}const destino=$('[data-apoyo-vista="'+tipo+'"]');const im=document.createElement('img');im.src=f.preview;im.alt=f.descripcion;im.className='campo-foto';destino.replaceChildren(im);if(tipo==='FRENTE')$('[data-paso="7"] [data-foto-conversacion]').src=f.preview;aviso('');}
 $('[data-perfil]').onclick=()=>$('[data-archivo-perfil]').click();$('[data-archivo-perfil]').onchange=e=>{const file=e.target.files[0];e.target.value='';recibir(file,'PERFIL').catch(e=>aviso(e.message));};
 $$('[data-agregar]').forEach(b=>b.onclick=()=>b.parentElement.querySelector('[data-archivo-extra]').click());$$('[data-archivo-extra]').forEach(i=>i.onchange=()=>{const file=i.files[0];i.value='';recibir(file,i.dataset.archivoExtra).catch(e=>aviso(e.message));});
 $$('[data-usar-ejemplo]').forEach(b=>b.onclick=()=>puerta.ejecutar(async()=>{await window.AplomoFotosCampo.ejemplo(b.dataset.usarEjemplo,file=>recibir(file,b.dataset.vistaEjemplo));b.closest('.galeria-demo').open=false;}));
 async function medir(){const d=actual();if(!d?.foto.ancho)return;$('[data-medidas-compactas]').hidden=false;const firma=JSON.stringify([d.estado.puntos,d.estado.manual,d.foto.ancho,d.estado.lado]);if(d.estado.firmaCampo===firma)return;d.estado.firmaCampo=firma;const turno=++peticion;d.estado.propuestas||={};
  await Promise.all([...angulos,'MUNECA'].map(async k=>{
   if(d.estado.manual[k]===undefined&&d.grupos[k].length!==3){if(angulos.includes(k)){delete d.estado.propuestas[k];if(respuestas.get('rango_'+k)?.origen==='foto'){form.elements['rango_'+k].value='';respuestas.delete('rango_'+k);const q=$('[data-pregunta="rango_'+k+'"]');q.querySelector('.campo-respuesta span').textContent='Elige una opción';q.querySelector('[data-origen]')?.remove();$('[data-angulo="'+k+'"]').textContent='Sin medir';}}return;}
   const p=analizador.parametrosActuales();p.set('segmento',k);p.set('csrf',valor('csrf'));p.set('visita',valor('visita'));p.set('ajustes_confirmados','no');let r=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin'});if(r.status===403&&await renovar()){p.set('csrf',valor('csrf'));r=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin'});}if(!r.ok){delete d.estado.firmaCampo;throw new Error('No pudimos medir. Tus puntos se conservan; intenta de nuevo.');}const nodo=new DOMParser().parseFromString(await r.text(),'text/html').querySelector('[data-parcial]');if(!nodo||turno!==peticion||actual()?.estado!==d.estado)return;const medida={grados:Number(nodo.dataset.grados),rango:Number(nodo.dataset.rango)};d.estado.propuestas[k]=medida;
   if(angulos.includes(k)){$('[data-angulo="'+k+'"]').textContent=medida.grados.toLocaleString('es-CO',{maximumFractionDigits:2})+'°';if(d.estado.manual[k]===undefined)poner('rango_'+k,medida.rango,'foto');$('[data-medida="'+k+'"] [data-revisar]').hidden=!window.AplomoRevisionReglas.requiereRevision({segmento:k,grados:medida.grados,encontrado:true});}
  }));dibujarPreguntas(paso===5);
 }
 let temporizador;
 form.addEventListener('aplomo:analisis',()=>{if(!actual())return;clearTimeout(temporizador);temporizador=setTimeout(()=>{leyendo=medir().catch(e=>aviso(e.message));},80);});
 $('[data-medir-muneca]').onclick=()=>{medirMuneca=true;const d=actual();delete d.estado.manual.MUNECA;analizador.destacar('MUNECA');analizador.actualizar('MUNECA');mostrar(5);let b=$('[data-usar-muneca]');if(!b){b=document.createElement('button');b.type='button';b.className='btn';b.dataset.usarMuneca='';b.textContent='Usar la medición de la muñeca';$('[data-medidas-compactas]').append(b);b.onclick=()=>puerta.ejecutar(async()=>{if(leyendo)await leyendo;await medir();const d=actual();if(!d?.estado.propuestas?.MUNECA){aviso('Marca codo, muñeca y nudillos en la foto.');return;}medirMuneca=false;poner('muneca_campo','FOTO');mostrar(6);});}};
 $('[data-evaluar-otro]').onclick=()=>{dosLados=true;cambiarLado(lado==='DERECHO'?'IZQUIERDO':'DERECHO');contexto();mostrar(5);};
 form.addEventListener('submit',e=>{e.preventDefault();const submitter=e.submitter;puerta.ejecutar(async()=>{
  if(paso!==9)return;if(!existente&&(!form.elements.acepta.checked||!valor('criterio')||!valor('minutos')||!valor('condiciones_respondidas')))throw new Error('Completa el consentimiento y la conversación inicial.');
  if(conFotos())await analizador.paso6();const datos=existente?new URLSearchParams({csrf:valor('csrf')}):new FormData(form);
  for(const k of [...datos.keys()])if(!['csrf','visita','puesto','recorrido','postura','motivo','criterio','minutos','condiciones_respondidas','acepta','registro','escritorio','pantalla','pies','apoyapies','apoyabrazos','regulable','observaciones'].includes(k))datos.delete(k);
  datos.set('destino_guardado',submitter?.value||'VOLVER');if(!existente)datos.set('asistente_completo','si');if(conFotos()){if(!existente)fotos.forEach((f,i)=>{datos.append('foto_'+i,f.file);datos.append('foto_'+i+'_vista',f.vista);datos.append('foto_'+i+'_descripcion',f.descripcion);});analizador.solicitudes().forEach((p,i)=>datos.append('analisis_'+i,p));}
  const r=await fetch(form.action,{method:'POST',body:datos,credentials:'same-origin',redirect:'manual'});if(r.status===201){if(!existente)try{sessionStorage.setItem(clave,JSON.stringify({tiempo_campo:valor('tiempo_campo'),momento_campo:valor('momento_campo'),esfuerzos:[...esfuerzos]}));}catch(_){}location.assign(r.headers.get('Location'));return;}if(r.status===403&&await renovar())throw new Error('Actualizamos el formulario. Tus respuestas se conservan; vuelve a guardar.');throw new Error((r.headers.get('Content-Type')||'').startsWith('text/plain')?await r.text():'No pudimos guardar. Tus respuestas y fotos se conservan.');
 });});
 if(existente){fotos=$$('[data-foto-id]').map(el=>({id:Number(el.dataset.fotoId),vista:el.dataset.vista,descripcion:el.dataset.descripcion,src:el.dataset.src,preview:el.dataset.src}));form.elements.criterio.value=form.dataset.criterio;$$('.campo-apoyo').forEach(d=>d.hidden=true);mostrar(4);}else{
  for(const n of ['tiempo_campo','momento_campo'])if(anterior?.[n]&&$('[data-respuesta="'+n+'"][data-valor="'+anterior[n]+'"]'))poner(n,anterior[n],'anterior');mostrar(1);
 }
})();
