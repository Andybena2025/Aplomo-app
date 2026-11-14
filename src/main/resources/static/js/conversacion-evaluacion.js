/* Paso 15-C: dos posturas independientes; mediciones y resultados siempre del servidor. */
(() => {
 const form=document.querySelector('form[data-conversacion]');if(!form)return;
 const $=s=>form.querySelector(s),$$=s=>[...form.querySelectorAll(s)],valor=n=>form.elements[n]?.value||'';
 const reglas=window.AplomoPosturasCampo,angulos=['BRAZO','ANTEBRAZO','CUELLO','TRONCO'];
 const respuestas=new Map();let fotos=[],tareas=[],lado=null,paso=1,maximo=1,ocupada=false,abierta=null,peticion=0,leyendo=null;
 let posturas=[],indice=0,retornoLado=false;
 const existente=Number(form.dataset.continuar||0),clave='aplomo:campo:'+valor('cuenta_recuerdo')+':'+valor('visita');let anterior=null;
 try{anterior=JSON.parse(sessionStorage.getItem(clave));}catch(_){}
 function aviso(t){$('[data-error-evaluacion]').hidden=!t;$('[data-error-evaluacion] span').textContent=t;}
 async function renovar(){const r=await fetch(location.href,{cache:'no-store',credentials:'same-origin'});if(!r.ok)return false;const t=new DOMParser().parseFromString(await r.text(),'text/html').querySelector('[name="csrf"]')?.value;if(!t||t===valor('csrf'))return false;form.elements.csrf.value=t;return true;}
 const analizador=window.crearAnalizadorAplomo(form,()=>tareas,aviso,renovar);
 $('[data-foto-revision]').append($('[data-editor-foto] .measure').firstElementChild);
 const actual=()=>analizador.datos(),conFotos=()=>!!existente||valor('registro')==='CON_FOTOS',ultima=()=>indice===posturas.length-1;
 function nombreMomento(v){return $('[data-respuesta="momento_campo"][data-valor="'+v+'"] span')?.textContent||v;}
 function contexto(){
  const puesto=form.elements.puesto,op=puesto.selectedOptions?.[0]||puesto,nombre=op?.dataset.tarea||'Puesto de trabajo';
  const plan=posturas[indice]||{momento:valor('momento_campo'),criterio:'MAS_INADECUADA'};
  form.elements.postura.value=reglas.descripcion(nombre,nombreMomento(plan.momento),lado);
  form.elements.minutos.value=valor('tiempo_campo');form.elements.criterio.value=existente?form.dataset.criterio:plan.criterio;
  form.elements.motivo.value=plan.misma?'También es la que más tiempo se mantiene':plan.criterio==='MAYOR_EXPOSICION'?'La postura que más repite.':'El momento que señala el trabajador.';
  $$('[data-postura-aviso]').forEach(el=>el.textContent=reglas.aviso(indice,posturas.length||1,plan,nombreMomento(plan.momento)));
  $('[data-misma]').hidden=valor('duracion_campo')!=='MISMA';
 }
 function preguntas(panel=$('[data-paso="'+paso+'"]')){return [...panel.querySelectorAll('[data-pregunta]')].filter(q=>!q.dataset.cuando||$('[data-chip="'+q.dataset.cuando+'"]')?.checked);}
 function poner(n,v,origen='elección'){
  form.elements[n].value=String(v);respuestas.set(n,{valor:String(v),origen});
  const q=$('[data-pregunta="'+n+'"]');if(!q)return;
  const opcion=[...q.querySelectorAll('[data-respuesta]')].find(b=>(Number.isFinite(Number(v))&&Number.isFinite(Number(b.dataset.valor))?Number(b.dataset.valor)===Number(v):b.dataset.valor===String(v)));
  q.querySelector('.campo-respuesta span').textContent=(opcion?.querySelector('span')?.textContent||String(v))+' ✓';
  q.querySelectorAll('[data-respuesta]').forEach(b=>b.setAttribute('aria-pressed',String(b===opcion)));
  q.querySelector('[data-origen]')?.remove();if(origen==='foto'||origen==='anterior'){const tag=document.createElement('small');tag.className='tag';tag.dataset.origen='';tag.textContent=origen==='foto'?'Según la foto · confirma al seguir':'Del puesto anterior · revísala';q.querySelector('.campo-respuesta').after(tag);}
 }
 function desplegar(n){abierta=n;$$('[data-pregunta]').forEach(q=>{const si=q.dataset.pregunta===n;q.querySelector('.campo-lista').hidden=!si;q.querySelector('[data-abrir]').setAttribute('aria-expanded',String(si));});}
 function faltante(){
  if(paso===1){if(!valor('puesto'))return 'puesto de trabajo';if(!form.elements.acepta.checked)return 'consentimiento informado por escrito';}
  if(paso===3){if(!valor('condiciones_respondidas'))return 'condiciones del puesto';if(valor('observaciones').length>255)return 'observaciones: máximo 255 caracteres';return '';}
  if(paso===5){if(!actual()?.foto.ancho)return 'foto de perfil';if(!valor('muneca_campo'))return 'posición de la muñeca';}
  const q=preguntas().find(q=>!valor(q.dataset.pregunta));return q?q.querySelector('em').textContent.replace(/[“”]/g,''):'';
 }
 function dibujarPreguntas(abrirSiguiente=false){
  const panel=$('[data-paso="'+paso+'"]'),qs=preguntas(panel);let falta=false;
  for(const q of panel.querySelectorAll('[data-pregunta]')){if(!qs.includes(q)){q.hidden=true;continue;}q.hidden=paso===5?false:falta;if(!valor(q.dataset.pregunta))falta=true;}
  if(abrirSiguiente){const siguiente=qs.find(q=>!q.hidden&&!valor(q.dataset.pregunta));desplegar(siguiente?.dataset.pregunta||null);}
  const faltaTexto=faltante(),seguir=$('[data-siguiente]');seguir.hidden=paso===7||!!faltaTexto;seguir.disabled=ocupada;seguir.textContent=ocupada?'Cargando…':paso===5?'Confirmar y seguir':'Seguir →';
  $('[data-falta]').textContent=ocupada?'Un momento…':faltaTexto?'Falta responder: '+faltaTexto:'';
  $('[data-atras-evaluacion]').disabled=ocupada||posturas.some(p=>p.guardada);
  for(const b of $$('[data-guardar-volver],[data-agregar-postura]')){b.hidden=paso!==7||!ultima()||(b.hasAttribute('data-agregar-postura')&&!conFotos());b.disabled=ocupada;}
  $('[data-postura-siguiente]').hidden=paso!==7||ultima();$('[data-postura-siguiente]').disabled=ocupada;
 }
 function mostrar(n){
  paso=n;maximo=Math.max(maximo,n);$$('[data-paso]').forEach(s=>s.hidden=Number(s.dataset.paso)!==n);
  $('[data-titulo-fijo]').textContent=$('[data-paso="'+n+'"] h2').textContent;
  $('#pasoActual').textContent=indice>0&&n===4?'Cambiar lado':'Paso '+(indice>0?n-4:n)+' de '+(indice>0?3:7);$('progress').max=indice>0?3:7;$('progress').value=indice>0?Math.max(1,maximo-4):maximo;
  $$('[data-etapa]').forEach(e=>{e.hidden=indice>0&&Number(e.dataset.etapa)<5;if(Number(e.dataset.etapa)===n)e.setAttribute('aria-current','step');else e.removeAttribute('aria-current');});
  $('[data-lado-titulo]').textContent=!conFotos()&&n===7?'Sin fotos':lado==='DERECHO'?'Lado derecho':lado==='IZQUIERDO'?'Lado izquierdo':'';
  $$('[data-cambiar-lado]').forEach(b=>b.hidden=indice===0||n!==5);
  if(n===5){const principal=actual()?.foto;$('[data-foto-revision]').hidden=!principal;$('[data-medidas-compactas]').hidden=!principal?.ancho;$('[data-subir-perfil]').hidden=!!principal;$('[data-muestra-perfil]').hidden=!!principal;}
  contexto();aviso('');dibujarPreguntas(n!==5);const h=$('[data-paso="'+n+'"] h2');h.tabIndex=-1;h.focus({preventScroll:true});window.scrollTo({top:0,behavior:'instant'});requestAnimationFrame(()=>analizador.repintar());
 }
 const puerta=window.crearAccionEvaluacion({alCambiar(v){ocupada=v;form.dataset.ocupado=String(v);dibujarPreguntas();},alError(e){aviso(e instanceof TypeError?'No pudimos conectar. Tus respuestas se conservan; intenta de nuevo.':e.message);}});
 function trasladar(){const d=actual();if(!d)return;const s=d.estado;
  Object.assign(s.ajustes,reglas.ajustes(nombre=>$('[data-chip="'+nombre+'"]')?.checked));
  for(const k of ['cuello','tronco']){const si=$('[data-chip="'+k+'_campo"]').checked,tipo=valor(k+'_tipo'),mapa=$('[data-orientacion="'+k+'"][data-tipo="'+tipo+'"]');s.ajustes[k+'Girado']=si&&mapa?.dataset.girada==='true';s.ajustes[k+'Inclinado']=si&&mapa?.dataset.inclinada==='true';}
  if(valor('muneca_campo'))s.manual.MUNECA=Number(valor('muneca_campo'));
  s.giro=$('[data-chip="giro_extremo"]').checked?'EXTREMO':'MEDIO';s.piernas=!$('[data-chip="pies_sin_apoyo"]').checked;s.html=null;s.revision++;
 }
 $$('[data-abrir]').forEach(b=>b.onclick=()=>{if(!ocupada)desplegar(abierta===b.dataset.abrir?null:b.dataset.abrir);});
 $$('[data-respuesta]').forEach(b=>b.onclick=()=>{
  const n=b.dataset.respuesta;if(ocupada)return;poner(n,b.dataset.valor);contexto();
  if(n.startsWith('rango_')){const k=n.slice(6),d=actual();if(d){d.estado.manual[k]=Number(b.dataset.valor);analizador.actualizar(k);}}
  if(n==='registro'&&b.dataset.valor==='CON_FOTOS')window.AplomoDeteccion.preparar();
  if(n==='registro'&&b.dataset.valor==='SIN_REGISTRO_FOTOGRAFICO'&&!existente){fotos.forEach(f=>f.file&&URL.revokeObjectURL(f.preview));fotos=[];tareas=[];posturas=[];indice=0;}
  trasladar();if(paso===5)desplegar(null);dibujarPreguntas(paso!==5);
 });
 $$('[data-muneca]').forEach(b=>b.onclick=()=>{if(ocupada)return;poner('muneca_campo',b.dataset.muneca);$$('[data-muneca]').forEach(x=>x.setAttribute('aria-pressed',String(x===b)));trasladar();dibujarPreguntas();});
 form.addEventListener('change',e=>{
  const condiciones=['escritorio','pantalla','pies','apoyapies','apoyabrazos','regulable'],el=e.target;
  if(el?.name==='ninguna'&&el.checked)condiciones.forEach(k=>form.elements[k].checked=false);if(condiciones.includes(el?.name)&&el.checked)form.elements.ninguna.checked=false;
  form.elements.condiciones_respondidas.value=form.elements.ninguna.checked?'NINGUNA':condiciones.some(k=>form.elements[k].checked)?'SELECCION':'';
  if(el?.hasAttribute('data-chip')){trasladar();dibujarPreguntas();if(el.checked&&['cuello_campo','tronco_campo'].includes(el.dataset.chip))desplegar(el.dataset.chip.replace('_campo','_tipo'));}
  contexto();dibujarPreguntas();
 });
 form.addEventListener('input',()=>dibujarPreguntas());
 function configurarPosturas(){
  const nuevos=reglas.plan(valor('momento_campo'),valor('duracion_campo'));
  // Revisar la conversación conserva las fotos y mediciones ya hechas de cada postura.
  posturas=nuevos.map((p,i)=>({...posturas[i],...p}));indice=Math.min(indice,posturas.length-1);contexto();
 }
 function datosGuardar(){
  contexto();const datos=existente?new URLSearchParams({csrf:valor('csrf')}):new FormData(form);
  for(const k of [...datos.keys()])if(!['csrf','visita','puesto','recorrido','postura','motivo','criterio','minutos','condiciones_respondidas','acepta','registro','escritorio','pantalla','pies','apoyapies','apoyabrazos','regulable','observaciones'].includes(k))datos.delete(k);
  if(!existente)datos.set('asistente_completo','si');
  if(conFotos()){if(!existente)fotos.forEach((f,i)=>{datos.append('foto_'+i,f.file);datos.append('foto_'+i+'_vista',f.vista);datos.append('foto_'+i+'_descripcion',f.descripcion);});analizador.solicitudes().forEach((p,i)=>datos.append('analisis_'+i,p));}return datos;
 }
 function recordarActual(){Object.assign(posturas[indice],{fotos,tareas,lado,muneca:valor('muneca_campo'),respuestas:new Map(respuestas),chips:$$('[data-chip]').map(c=>[c.dataset.chip,c.checked]),datos:datosGuardar(),puntaje:Number($('[data-resultados] [data-puntaje-final]')?.dataset.puntajeFinal||0),html:$('[data-resultados]').innerHTML});}
 function resumen(){
  const destino=$('[data-resultados]');destino.replaceChildren();$('[data-resumen-titulo]').hidden=!ultima();
  const resultados=ultima()?posturas:[posturas[indice]],max=Math.max(...resultados.map(p=>Number(new DOMParser().parseFromString(p.html||'','text/html').querySelector('[data-puntaje-final]')?.dataset.puntajeFinal||0)));
  resultados.forEach(p=>{const seccion=document.createElement('section');seccion.className='card stack';const titulo=document.createElement('h3');titulo.textContent=reglas.aviso(posturas.indexOf(p),posturas.length,p,nombreMomento(p.momento));seccion.append(titulo);const parsed=new DOMParser().parseFromString(p.html||'','text/html'),score=Number(parsed.querySelector('[data-puntaje-final]')?.dataset.puntajeFinal||0);if(max&&score===max){seccion.classList.add('postura-critica');const tag=document.createElement('strong');tag.textContent='Postura más crítica';seccion.append(tag);}seccion.append(...parsed.body.childNodes);destino.append(seccion);});
 }
 function limpiarCuerpo(){
  for(const q of $$('[data-pregunta]'))if(Number(q.closest('[data-paso]').dataset.paso)>=5){const n=q.dataset.pregunta;form.elements[n].value='';respuestas.delete(n);q.querySelector('.campo-respuesta span').textContent='Elige una opción';q.querySelector('[data-origen]')?.remove();q.querySelectorAll('[data-respuesta]').forEach(b=>b.setAttribute('aria-pressed','false'));}
  form.elements.muneca_campo.value='';$$('[data-muneca]').forEach(b=>b.setAttribute('aria-pressed','false'));$$('[data-chip]').forEach(c=>c.checked=false);$$('[data-angulo]').forEach(e=>e.textContent='Sin medir');$$('[data-apoyo-vista]').forEach(d=>d.replaceChildren());$$('.campo-apoyo').forEach(d=>d.open=false);$('[data-resultados]').replaceChildren();
 }
 function activar(i){
  if(posturas[indice])Object.assign(posturas[indice],{fotos,tareas,lado,muneca:valor('muneca_campo'),respuestas:new Map(respuestas),chips:$$('[data-chip]').map(c=>[c.dataset.chip,c.checked])});
  indice=i;peticion++;limpiarCuerpo();const p=posturas[i];fotos=p.fotos||[];tareas=p.tareas||[];lado=p.lado||lado;
  if(p.respuestas)for(const [n,v]of p.respuestas)if(form.elements[n]&&Number($('[data-pregunta="'+n+'"]')?.closest('[data-paso]').dataset.paso)>=5)poner(n,v.valor,v.origen);
  form.elements.muneca_campo.value=p.muneca||'';
  for(const [n,v]of p.chips||[])$('[data-chip="'+n+'"]').checked=v;
  $$('[data-muneca]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.muneca===valor('muneca_campo'))));form.elements.lado_recorrido.value=lado;
  if(tareas.length){analizador.iniciar();analizador.seleccionar(0);delete tareas[0].analisis.firmaCampo;leyendo=medir().catch(e=>aviso(e.message));}
 }
 $('[data-siguiente]').onclick=()=>puerta.ejecutar(async()=>{
  if(faltante())return;
  if(paso===2)configurarPosturas();
  if(paso===3&&!conFotos()){for(const p of posturas)p.html='<p>Se guardará sin fotos ni puntaje.</p>';indice=posturas.length-1;mostrar(7);resumen();return;}
  if(paso===4){lado=valor('lado_recorrido');if(tareas.length){tareas[0].ladoFijo=lado;actual().estado.lado=lado;analizador.actualizar();}if(existente&&!tareas.length)activarExistente();contexto();mostrar(5);retornoLado=false;return;}
  if(paso===5){if(leyendo)await leyendo;if(faltante())return;trasladar();}
  if(paso===6){
   const peso=valor('peso_campo'),act=valor('esfuerzo_opcion'),golpe=valor('golpe_campo'),carga=$('[data-mapeos] [data-peso="'+peso+'"][data-actividad="'+act+'"][data-golpe="'+golpe+'"]')?.dataset.carga;
   if(!carga)throw new Error('Responde el peso, la frecuencia y los golpes.');const d=actual();trasladar();Object.assign(d.estado,{actividadEleccion:act,actividad:act!=='NINGUNA',carga,actividadConfirmada:true,cargaConfirmada:true,ajustesConfirmados:true});await analizador.paso6();recordarActual();mostrar(7);resumen();return;
  }mostrar(paso+1);
 });
 $('[data-postura-siguiente]').onclick=()=>puerta.ejecutar(async()=>{activar(indice+1);maximo=5;mostrar(5);});
 $$('[data-cambiar-lado]').forEach(b=>b.onclick=()=>{if(ocupada)return;retornoLado=true;mostrar(4);});
 $('[data-atras-evaluacion]').onclick=()=>{if(ocupada||posturas.some(p=>p.guardada))return;if(indice>0&&paso===5){activar(indice-1);mostrar(7);resumen();return;}if(indice>0&&paso===4&&retornoLado){mostrar(5);return;}if(paso===1||existente&&paso===4){let p=$('[data-salir-inline]');if(!p){p=document.createElement('p');p.dataset.salirInline='';p.className='aviso';const a=document.createElement('a');a.className='btn';a.href=existente?'/evaluaciones/'+existente:'/visitas/'+valor('visita');a.textContent='Salir sin guardar';p.append('Tus cambios aún no están guardados. ',a);form.prepend(p);}return;}mostrar(paso===7&&!conFotos()?3:paso-1);};
 $('[data-cerrar-error]').onclick=()=>aviso('');
 function iniciarFoto(f){tareas.push({...f,indiceFoto:fotos.indexOf(f),ladoFijo:lado});analizador.iniciar();analizador.seleccionar(tareas.length-1);$('[data-foto-revision]').hidden=false;$('[data-subir-perfil]').hidden=true;$('[data-muestra-perfil]').hidden=true;$('[data-falta]').textContent='Preparando la foto…';}
 function activarExistente(){const f=fotos.find(f=>f.vista==='PERFIL')||fotos[0];if(f){f.uso='MEDIR';iniciarFoto(f);}}
 async function recibir(file,tipo){if(!file)return;window.AplomoFotosCampo.validar(file,conFotos(),fotos.length);const perfil=tipo==='PERFIL',f={file,preview:URL.createObjectURL(file),vista:tipo,descripcion:perfil?'Perfil':tipo==='FRENTE'?'Frente':'Detalle de mano y muñeca · solo evidencia',uso:perfil?'MEDIR':'APOYO',ladoOrigen:lado};fotos.push(f);if(perfil){iniciarFoto(f);return;}const destino=$('[data-apoyo-vista="'+tipo+'"]'),im=document.createElement('img');im.src=f.preview;im.alt=f.descripcion;im.className='campo-foto';destino.replaceChildren(im);aviso('');}
 $('[data-perfil]').onclick=()=>$('[data-archivo-perfil]').click();$('[data-archivo-perfil]').onchange=e=>{const file=e.target.files[0];e.target.value='';recibir(file,'PERFIL').catch(e=>aviso(e.message));};
 $$('[data-agregar]').forEach(b=>b.onclick=()=>b.parentElement.querySelector('[data-archivo-extra]').click());$$('[data-archivo-extra]').forEach(i=>i.onchange=()=>{const file=i.files[0];i.value='';recibir(file,i.dataset.archivoExtra).catch(e=>aviso(e.message));});
 $$('[data-usar-ejemplo]').forEach(b=>b.onclick=()=>puerta.ejecutar(async()=>{await window.AplomoFotosCampo.ejemplo(b.dataset.usarEjemplo,file=>recibir(file,b.dataset.vistaEjemplo));b.closest('.galeria-demo').open=false;}));
 async function medir(){const d=actual();if(!d?.foto.ancho)return;$('[data-medidas-compactas]').hidden=false;const firma=JSON.stringify([d.estado.puntos,d.estado.manual,d.foto.ancho,d.estado.lado]);if(d.estado.firmaCampo===firma)return;d.estado.firmaCampo=firma;const turno=++peticion;d.estado.propuestas||={};
  await Promise.all([...angulos,'MUNECA'].map(async k=>{
   if(d.estado.manual[k]===undefined&&d.grupos[k].length!==3){if(angulos.includes(k)){delete d.estado.propuestas[k];if(respuestas.get('rango_'+k)?.origen==='foto'){form.elements['rango_'+k].value='';respuestas.delete('rango_'+k);const q=$('[data-pregunta="rango_'+k+'"]');q.querySelector('.campo-respuesta span').textContent='Elige una opción';q.querySelector('[data-origen]')?.remove();$('[data-angulo="'+k+'"]').textContent='Sin medir';}}return;}
   const p=analizador.parametrosActuales();p.set('segmento',k);p.set('csrf',valor('csrf'));p.set('visita',valor('visita'));p.set('ajustes_confirmados','no');let r=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin'});if(r.status===403&&await renovar()){p.set('csrf',valor('csrf'));r=await fetch('/evaluaciones/calcular',{method:'POST',body:p,credentials:'same-origin'});}if(!r.ok){delete d.estado.firmaCampo;throw new Error('No pudimos medir. Tus puntos se conservan; intenta de nuevo.');}const nodo=new DOMParser().parseFromString(await r.text(),'text/html').querySelector('[data-parcial]');if(!nodo||turno!==peticion||actual()?.estado!==d.estado)return;const medida={grados:Number(nodo.dataset.grados),rango:Number(nodo.dataset.rango)};d.estado.propuestas[k]=medida;
   if(angulos.includes(k)){$('[data-angulo="'+k+'"]').textContent=medida.grados.toLocaleString('es-CO',{maximumFractionDigits:2})+'°';if(d.estado.manual[k]===undefined)poner('rango_'+k,medida.rango,'foto');$('[data-medida="'+k+'"] [data-revisar]').hidden=!window.AplomoRevisionReglas.requiereRevision({segmento:k,grados:medida.grados,encontrado:true});}
  }));dibujarPreguntas();
 }

 let temporizador;
 form.addEventListener('aplomo:analisis',()=>{if(!actual())return;clearTimeout(temporizador);temporizador=setTimeout(()=>{leyendo=medir().catch(e=>aviso(e.message));},80);});
 form.addEventListener('submit',e=>{e.preventDefault();const agregar=e.submitter?.hasAttribute('data-agregar-postura');puerta.ejecutar(async()=>{
  if(paso!==7||!ultima())return;if(!existente&&(!form.elements.acepta.checked||!valor('criterio')||!valor('minutos')||!valor('condiciones_respondidas')))throw new Error('Completa el consentimiento y la conversación inicial.');
  if(conFotos()){await analizador.paso6();recordarActual();}else{for(let i=0;i<posturas.length;i++){indice=i;posturas[i].datos=datosGuardar();}indice=posturas.length-1;}
  // Un envío por postura usa el contrato existente; conservar los acuses evita repetir los ya guardados.
  if(!existente){const compartidos=new FormData(form);for(const p of posturas)for(const k of ['minutos','condiciones_respondidas','acepta','registro','escritorio','pantalla','pies','apoyapies','apoyabrazos','regulable','observaciones']){if(compartidos.has(k))p.datos.set(k,compartidos.get(k));else p.datos.delete(k);}}
  // La última evaluación determina el estado actual del puesto: guardar al final la más crítica.
  await reglas.guardarPendientes(reglas.ordenGuardado(posturas),async p=>{
   p.datos.set('csrf',valor('csrf'));const r=await fetch(form.action,{method:'POST',body:p.datos,credentials:'same-origin',redirect:'manual'});
   if(r.status===201)return r.headers.get('Location');if(r.status===403&&await renovar())throw new Error('Actualizamos el formulario. Tus respuestas se conservan; vuelve a guardar.');throw new Error((r.headers.get('Content-Type')||'').startsWith('text/plain')?await r.text():'No pudimos guardar. Tus respuestas y fotos se conservan.');
  });
  if(!existente)try{sessionStorage.setItem(clave,JSON.stringify({tiempo_campo:valor('tiempo_campo')}));}catch(_){}
  const ruta=reglas.ordenGuardado(posturas).at(-1).guardada;if(agregar){const base=ruta.split('?')[0];location.assign(base+'/analizar');}else location.assign('/visitas/'+valor('visita')+'?aviso='+encodeURIComponent('Evaluación guardada.'));
 });});
 if(existente){fotos=$$('[data-foto-id]').map(el=>({id:Number(el.dataset.fotoId),vista:el.dataset.vista,descripcion:el.dataset.descripcion,src:el.dataset.src,preview:el.dataset.src}));posturas=[{momento:'Análisis adicional',criterio:form.dataset.criterio}];$$('.campo-apoyo').forEach(d=>d.hidden=true);mostrar(4);}else{if(anterior?.tiempo_campo&&$('[data-respuesta="tiempo_campo"][data-valor="'+anterior.tiempo_campo+'"]'))poner('tiempo_campo',anterior.tiempo_campo,'anterior');mostrar(1);}
})();
