/* RF-014/015/016, RNF-002/007: pasos del mismo formulario; la validación definitiva es del servidor. */
(() => {
  const form = document.querySelector('[data-evaluacion]');
  if (!form) return;
  const continuar = Number(form.dataset.continuar || 0);
  const tour=!!form.dataset.tour, inicio=tour?Number(form.dataset.inicio):1;
  const corporal=!!form.dataset.cuerpo;
  const FOTOS=tour?6:3, LADO=7, ANGULOS=corporal?8:tour?7:4, AJUSTES=corporal?9:tour?8:5, RESULTADO=corporal?11:tour?9:6;
  let paso = continuar ? FOTOS : inicio, enviando = false, avanceAutomatico=null, intentado=false, clavePregunta=null;
  const reducirMovimiento=()=>matchMedia('(prefers-reduced-motion: reduce)').matches;
  const error = form.querySelector('[data-error-evaluacion]');
  const atras = form.querySelector('[data-atras-evaluacion]');
  const siguiente = form.querySelector('[data-siguiente]');
  const guardar = form.querySelector('[data-guardar-evaluacion]');
  const volver = form.querySelector('[data-guardar-volver]');
  const memoria=window.crearMemoriaEvaluacion(form);
  form.querySelectorAll('.slotImg img').forEach(img=>img.dataset.muestra=img.src);
  const elegidas = form.querySelector('[data-fotos-elegidas]');
  let fotos = continuar ? [...form.querySelectorAll('[data-foto-id]')].map(el=>({id:Number(el.dataset.fotoId),vista:el.dataset.vista,descripcion:el.dataset.descripcion,src:el.dataset.src,preview:el.dataset.src})) : [], urls = [];
  let tareas=[];
  const analizador = window.crearAnalizadorAplomo(form,()=>corporal?tareas:fotos,aviso,actualizarFormulario);
  const cuerpo=corporal?window.crearRecorridoCorporal(form,analizador,aviso,actualizarFormulario,actualizarGuia,()=>mostrar(AJUSTES)):null;
  const esfuerzo=corporal?window.crearEsfuerzoGuiado(form,analizador,memoria,actualizarGuia):null;
  function aviso(texto) {
    error.hidden = !texto;
    error.querySelector('span').textContent = texto;
    if (texto) error.scrollIntoView({block:'nearest',behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth'});
  }
  form.querySelector('[data-cerrar-error]').addEventListener('click', () => aviso(''));
  async function actualizarFormulario() {
    const respuesta = await fetch(location.href, {credentials:'same-origin',cache:'no-store'});
    if (!respuesta.ok) return false;
    const pagina = new DOMParser().parseFromString(await respuesta.text(), 'text/html');
    const token = pagina.querySelector('[data-evaluacion] input[name="csrf"]');
    if (!token || token.value === form.elements.csrf.value) return false;
    form.elements.csrf.value = token.value;
    return true;
  }
  function mostrar(n) {
    clearTimeout(avanceAutomatico);intentado=false; paso = n;
    if(corporal&&paso!==ANGULOS)cuerpo.cancelar();esfuerzo?.cancelar();
    form.querySelectorAll('[data-paso]').forEach(s => s.hidden = Number(s.dataset.paso) !== paso);
    document.querySelectorAll('[data-indicador]').forEach(s => {
      const p = Number(s.dataset.indicador);
      s.classList.toggle('on',p === paso); s.classList.toggle('done',p < paso);
      if (p === paso) s.setAttribute('aria-current','step'); else s.removeAttribute('aria-current');
    });
    const sinFotos=tour&&form.querySelector('[name="registro"]:checked')?.value==='SIN_REGISTRO_FOTOGRAFICO';
    actualizarProgreso();
    atras.textContent = tour ? '← Atrás' : paso === 1 ? 'Cancelar' : continuar && paso === ANGULOS ? 'Volver a la evaluación' : '← Atrás';
    siguiente.hidden = paso === RESULTADO || (sinFotos&&paso===FOTOS); guardar.hidden = !siguiente.hidden;if(volver)volver.hidden=guardar.hidden;
    aviso('');
    if(corporal&&paso===ANGULOS)cuerpo.mostrar();
    if(corporal&&(paso===9||paso===10))esfuerzo.mostrar(paso);memoria.aviso(paso);
    if(tour){const panel=form.querySelector(`[data-paso="${paso}"]`);panel.classList.remove('tour-entra');void panel.offsetWidth;panel.classList.add('tour-entra');actualizarGuia();}
    form.querySelector(`[data-paso="${paso}"] h2`).setAttribute('tabindex','-1');
    form.querySelector(`[data-paso="${paso}"] h2`).focus({preventScroll:true});
    window.scrollTo({top:0,behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth'});
  }
  function validarFotos() {
    if(continuar){if(!fotos.length||fotos.some(f=>!f.uso)||!fotos.some(f=>f.uso==='MEDIR'))throw new Error('Elige Para medir o De apoyo en cada foto; al menos una debe servir Para medir.');return true;}
    const registro=form.querySelector('[name="registro"]:checked');
    if (!registro) throw new Error('Elige Con registro fotográfico o Sin registro fotográfico.');
    const conFotos=registro.value==='CON_FOTOS';
    if (!form.elements.acepta.checked) throw new Error('Confirma que la persona firmó en papel el consentimiento para ser evaluada.');
    if (conFotos && (fotos.length<1 || fotos.length>10)) throw new Error('Agrega de 1 a 10 fotos antes de continuar.');
    if (conFotos && fotos.some(f=>!f.descripcion.trim() || f.descripcion.length>200)) throw new Error('Cada foto necesita una descripción de hasta 200 caracteres.');
    if(tour&&conFotos&&fotos.some(f=>!f.uso))throw new Error('Elige Para medir o De apoyo en cada foto.');
    if(tour&&conFotos&&!fotos.some(f=>f.uso==='MEDIR'))throw new Error('Elige al menos una foto Para medir.');
    return conFotos;
  }
  siguiente.addEventListener('click', async () => {
    if(enviando||cuerpo?.ocupado())return;
    if(tour&&!respuestaCompleta()){intentado=true;actualizarGuia();return;}
    try {
      if(tour&&!continuar&&paso<FOTOS){
        if(!respuestaCompleta())return;
        if(paso===0)actualizarModo();
        armarMotivo();mostrar(paso+1);return;
      }
      if(!tour&&paso<FOTOS) {
        const controles=[...form.querySelectorAll(`[data-paso="${paso}"] input, [data-paso="${paso}"] select, [data-paso="${paso}"] textarea`)];
        if(controles.some(c=>!c.reportValidity()))return;
        mostrar(paso+1);return;
      }
      siguiente.disabled=true;atras.disabled=true;siguiente.textContent='Cargando…';
      if(paso===FOTOS) {
        if(!validarFotos()) {
          mostrar(RESULTADO);form.querySelector('[data-resultados]').textContent='Sin registro fotográfico. Se conservarán los datos de la evaluación sin fotos ni puntaje RULA.';
        }else if(corporal){mostrar(LADO);}else{mostrar(ANGULOS);analizador.iniciar();}
      }else if(corporal&&paso===LADO){
        tareas=cuerpo.preparar(fotos,form.querySelector('[name="lado_recorrido"]:checked').value);esfuerzo.preparar(tareas);analizador.iniciar();mostrar(ANGULOS);
      }else if(corporal&&paso===ANGULOS){await cuerpo.continuar();}
      else if(paso===ANGULOS){await analizador.paso5();mostrar(AJUSTES);}
      else if(corporal&&(paso===9||paso===10)){const destino=esfuerzo.continuar();if(destino===RESULTADO)await analizador.paso6();if(destino)mostrar(destino);}
      else if(paso===AJUSTES){await analizador.paso6();mostrar(RESULTADO);}
    }catch(e){aviso(e instanceof TypeError?'No se pudo conectar. Tus campos y fotos se conservan; revisa la conexión e inténtalo de nuevo.':e.message);}
    finally{siguiente.disabled=false;atras.disabled=false;siguiente.textContent=tour?'Continuar →':'Siguiente →';if(tour)actualizarGuia();}
  });
  atras.addEventListener('click', () => {
    if(enviando)return;
    if(corporal&&paso===ANGULOS){if(!cuerpo.atras())mostrar(LADO);return;}
    if(corporal&&(paso===9||paso===10)){const n=esfuerzo.atras();if(n===ANGULOS)cuerpo.ultimo();mostrar(n);return;}
    if(corporal&&paso===RESULTADO){esfuerzo.ultimo();mostrar(10);return;}
    if(continuar&&paso===ANGULOS) {
      if(confirm('¿Volver a la evaluación? Los análisis que todavía no guardaste se perderán.'))location.assign('/evaluaciones/'+continuar);
    }else if(paso===RESULTADO&&!continuar&&form.querySelector('[name="registro"]:checked')?.value==='SIN_REGISTRO_FOTOGRAFICO')mostrar(FOTOS);
    else if(paso>inicio){mostrar(paso-1);if(paso===ANGULOS)analizador.iniciar();}
    else if(confirm('¿Salir del asistente? Los datos que todavía no guardaste se perderán.'))location.assign(continuar?'/evaluaciones/'+continuar:'/visitas/'+form.elements.visita.value);
  });
  form.querySelectorAll('.opt input').forEach(c => c.addEventListener('change', () => c.closest('.opt').classList.toggle('on', c.checked)));
  form.querySelectorAll('[name="registro"]').forEach(r => r.addEventListener('change', () => {
    const conFotos = r.value === 'CON_FOTOS';
    form.querySelector('[data-con-fotos]').hidden = !conFotos;
    if(tour){mostrar(paso);}else if (!conFotos) form.elements.acepta.checked = false;
    aviso('');
  }));
  form.querySelector('[data-otro-lado]')?.addEventListener('click', e => {
    const panel = form.querySelector('[data-lado-extra]');
    panel.hidden = !panel.hidden;
    form.querySelectorAll('[data-lado-principal]').forEach(p=>p.hidden=!panel.hidden);
    e.currentTarget.setAttribute('aria-expanded',String(!panel.hidden));
    e.currentTarget.querySelector('b').textContent=panel.hidden?'+ Otro lado del cuerpo':'← Volver al lado principal';
    e.currentTarget.querySelector('.hint').textContent=panel.hidden?'Si dudas cuál lado tiene más carga, puedes registrar también el otro lado.':'Perfil y detalle del otro lado. La foto de frente sirve para ambos lados.';
  });
  function renderFotos() {
    if(continuar)return;
    form.dataset.fotos=String(fotos.length);
    urls.forEach(URL.revokeObjectURL); urls = [];
    elegidas.replaceChildren();
    const cuenta = document.createElement('span'); cuenta.className='eyebrow'; cuenta.textContent=`${fotos.length} de 10 fotos seleccionadas`;
    elegidas.append(cuenta);
    fotos.forEach((foto, i) => {
      const fila = document.createElement('div'); fila.className='foto-elegida';
      const img = document.createElement('img'); img.alt=`Foto seleccionada ${i+1}`;
      // El navegador solo muestra una vista previa. El servidor comprueba firma y decodificación reales.
      if (['image/jpeg','image/png'].includes(foto.file.type)) {
        const url = URL.createObjectURL(foto.file); urls.push(url); img.src=url; foto.preview=url;
      } else img.alt='Archivo pendiente de validación';
      const campos = document.createElement('div'); campos.className='stack';
      const vistaLabel=document.createElement('label'); vistaLabel.textContent=`Vista de la foto ${i+1}`;
      const vista=document.createElement('select'); vista.className='input'; vista.setAttribute('aria-label',vistaLabel.textContent);
      [['PERFIL','Perfil'],['FRENTE','Frente'],['OTRA','Otra / detalle']].forEach(([v,t])=>{const o=document.createElement('option');o.value=v;o.textContent=t;vista.append(o);});
      vista.value=foto.vista; vista.addEventListener('change',()=>{foto.vista=vista.value;if(tour)actualizarGuia();});
      const descLabel=document.createElement('label'); descLabel.textContent=`Descripción de la foto ${i+1}`;
      const desc=document.createElement('textarea');desc.className='input';desc.maxLength=200;desc.value=foto.descripcion;
      desc.setAttribute('aria-label',descLabel.textContent);desc.addEventListener('input',()=>{foto.descripcion=desc.value;if(tour)actualizarGuia();});
      const quitar=document.createElement('button');quitar.type='button';quitar.className='btn sm';quitar.textContent=`Quitar foto ${i+1}`;
      quitar.addEventListener('click',()=>{fotos.splice(i,1);renderFotos();});
      campos.append(vistaLabel,vista,descLabel,desc);
      if(tour){
        const grupo=document.createElement('div');grupo.className='tour-opciones tour-uso';grupo.setAttribute('role','radiogroup');grupo.setAttribute('aria-label',`Uso de la foto ${i+1}`);
        for(const[v,t]of[['MEDIR','Para medir'],['APOYO','De apoyo']]){
          const label=document.createElement('label');label.className='tour-opcion';const radio=document.createElement('input');radio.type='radio';radio.name=`uso_foto_${i}`;radio.value=v;radio.checked=foto.uso===v;
          const texto=document.createElement('span'),franja=document.createElement('span'),titulo=document.createElement('span'),ayuda=document.createElement('small');
          franja.className='tour-franja';titulo.id=`uso-${i}-${v}-texto`;titulo.textContent=t;ayuda.className='tour-ayuda';ayuda.id=`uso-${i}-${v}-ayuda`;
          ayuda.textContent=v==='MEDIR'?'Perfil o frente nítido, con el cuerpo completo. Con esta foto se calculan los ángulos.':'Foto de contexto: el puesto, la herramienta o la mano. Se guarda, pero no se mide.';
          radio.setAttribute('aria-labelledby',titulo.id);radio.setAttribute('aria-describedby',ayuda.id);franja.append(titulo,ayuda);texto.append(franja);label.append(radio,texto);grupo.append(label);
          radio.addEventListener('change',()=>{foto.uso=v;if(foto.analisis)foto.analisis.habilitada=v==='MEDIR';actualizarGuia();});
        }
        campos.append(grupo);
      }
      campos.append(quitar);fila.append(img,campos);elegidas.append(fila);
    });
    form.querySelectorAll('.slot').forEach(slot=>{
      const f=fotos.find(f=>f.slot===slot.dataset.slotId), img=slot.querySelector('.slotImg img');
      img.src=f&&f.preview?f.preview:img.dataset.muestra;img.alt=f?'Tu foto de '+slot.querySelector('.slotTop .eyebrow').textContent.toLowerCase():'Muestra de '+slot.querySelector('.slotTop .eyebrow').textContent.toLowerCase();
      slot.classList.toggle('lista',!!f);slot.querySelector('.slotTop .tag').textContent=f?'✓ Lista':'Muestra';
      slot.querySelector('.slotLbl').textContent=f?'Tu foto':'Muestra';
    });
    if(tour)actualizarGuia();
  }
  form.querySelectorAll('[data-elegir-foto]').forEach(b=>b.addEventListener('click',()=>document.getElementById(b.dataset.elegirFoto).click()));
  form.querySelectorAll('[data-fotos]').forEach(input=>input.addEventListener('change',()=>{
    const nuevas=[...input.files]; input.value='';
    if (fotos.length+nuevas.length>10) {aviso('Solo puedes agregar hasta 10 fotos. Las fotos anteriores se conservan.');return;}
    if (nuevas.some(f=>f.size>5*1024*1024 || f.size===0)) {aviso('Cada foto debe tener contenido y pesar como máximo 5 MB. Las fotos anteriores se conservan.');return;}
    fotos.push(...nuevas.map(file=>({file,slot:input.closest('.slot').dataset.slotId,vista:input.dataset.vista,descripcion:input.dataset.titulo})));
    aviso('');renderFotos();
  }));
  form.addEventListener('submit',async e=>{
    e.preventDefault();if (enviando) return;
    if(paso!==RESULTADO&&!(tour&&paso===FOTOS&&form.querySelector('[name="registro"]:checked')?.value==='SIN_REGISTRO_FOTOGRAFICO')){siguiente.click();return;}
    if(tour&&!continuar){for(let n=inicio;n<FOTOS;n++)if(!respuestaCompleta(n)){mostrar(n);intentado=true;actualizarGuia();return;}armarMotivo();}
    enviando=true;guardar.disabled=true;if(volver)volver.disabled=true;atras.disabled=true;
    const boton=e.submitter||guardar,destinoGuardar=boton.value||'SIGUIENTE';
    let conFotos=true;
    try{if(!continuar)conFotos=validarFotos();if(conFotos)await analizador.paso6();}catch(x){enviando=false;guardar.disabled=false;if(volver)volver.disabled=false;atras.disabled=false;aviso(x instanceof TypeError?'No se pudo conectar. Tus campos y fotos se conservan; revisa la conexión e inténtalo de nuevo.':x.message);return;}
    const data=continuar?new URLSearchParams({csrf:form.elements.csrf.value}):new FormData(form);
    data.set('destino_guardado',destinoGuardar);
    if(!continuar){
      for(const clave of [...data.keys()])if(/^(uso_foto_|rango_|ajuste_|ninguno_|esfuerzo_|carga_)/.test(clave)||['postura_opcion','lado_recorrido','ninguna','cuenta_recuerdo'].includes(clave))data.delete(clave);
      data.set('asistente_completo','si');
      if(!conFotos&&!tour)data.delete('acepta');
      if(conFotos)fotos.forEach((foto,i)=>{data.append(`foto_${i}`,foto.file);data.append(`foto_${i}_vista`,foto.vista);data.append(`foto_${i}_descripcion`,foto.descripcion);});
    }
    if(conFotos)analizador.solicitudes().forEach((datos,i)=>data.append(`analisis_${i}`,datos));
    enviando=true;guardar.disabled=true;atras.disabled=true;boton.textContent='Guardando…';aviso('');const terminarProgreso=window.AplomoProgreso?.iniciar('Guardando la evaluación…');
    try {
      const respuesta=await fetch(form.action,{method:'POST',body:data,credentials:'same-origin',redirect:'manual'});
      if (respuesta.status===201) {if(!continuar)memoria.guardar(conFotos?tareas:[]);location.assign(respuesta.headers.get('Location'));return;}
      if (respuesta.status===0 || respuesta.type==='opaqueredirect') aviso('Tu sesión venció. Inicia sesión en otra pestaña y vuelve aquí: tus datos siguen escritos.');
      else if (respuesta.status===403 && await actualizarFormulario()) aviso('El formulario se actualizó. Tus campos y fotos se conservan; vuelve a pulsar Guardar evaluación.');
      else if ((respuesta.headers.get('Content-Type')||'').startsWith('text/plain')) aviso(await respuesta.text());
      else aviso('No pudimos guardar. Comprueba tu sesión en otra pestaña y vuelve a intentarlo aquí; tus datos se conservan.');
    } catch (_) {aviso('No se pudo conectar. Tus campos y fotos se conservan; revisa la conexión e inténtalo de nuevo.');}
    finally {terminarProgreso?.();enviando=false;guardar.disabled=false;if(volver)volver.disabled=false;atras.disabled=false;guardar.textContent='Guardar y evaluar siguiente puesto';if(volver)volver.textContent='Guardar y volver a la visita';}
  });
  function respuestaCompleta(n=paso){
    if(corporal&&(n===9||n===10))return esfuerzo.respondido();
    if(continuar&&n===FOTOS){try{return validarFotos();}catch(_){return false;}}
    if(n===0)return !!form.elements.puesto.value;
    if(n===1)return form.elements.acepta.checked;
    if(n===2)return !!form.querySelector('[name="postura_opcion"]:checked')&&!!form.elements.postura.value.trim()&&form.elements.postura.value.length<=150;
    if(n===3)return !!form.querySelector('[name="criterio"]:checked');
    if(n===4)return !!form.querySelector('[name="minutos"]:checked');
    if(n===5)return !!form.elements.condiciones_respondidas.value&&form.elements.observaciones.value.length<=255;
    if(n===FOTOS){try{validarFotos();return true;}catch(_){return false;}}
    if(corporal&&n===LADO)return !!form.querySelector('[name="lado_recorrido"]:checked');
    if(corporal&&n===ANGULOS)return cuerpo.respondido();
    if(n===ANGULOS)return analizador.medicionesCompletas();
    if(n===AJUSTES)return analizador.respuestasCompletas();
    return true;
  }
  function actualizarGuia(){
    if(!tour)return;
    const clave=corporal?(paso===ANGULOS?paso+':'+cuerpo.progreso().actual:(paso===9||paso===10)?paso+':'+esfuerzo.progreso().actual:paso):paso;
    if(clave!==clavePregunta){intentado=false;clavePregunta=clave;}
    const completa=respuestaCompleta();siguiente.disabled=enviando||!!cuerpo?.ocupado();guardar.disabled=enviando;if(volver)volver.disabled=enviando;
    siguiente.setAttribute('aria-disabled',String(enviando||!completa));guardar.setAttribute('aria-disabled',String(enviando||!completa));
    actualizarProgreso();
    const pendiente=form.querySelector('[data-pregunta-pendiente]');pendiente.hidden=completa||!intentado;
    pendiente.textContent=paso===FOTOS&&form.querySelector('[name="registro"]:checked')?.value==='CON_FOTOS'?'Agrega de 1 a 10 fotos, completa sus descripciones y elige Para medir o De apoyo (al menos una Para medir).':'Responde para continuar.';
    form.querySelectorAll('[data-paso]').forEach(p=>p.classList.toggle('tour-sin-respuesta',Number(p.dataset.paso)===paso&&!completa&&intentado));
  }
  function actualizarProgreso(){
    const sinFotos=tour&&form.querySelector('[name="registro"]:checked')?.value==='SIN_REGISTRO_FOTOGRAFICO';
    const recorrido=cuerpo?.progreso(),extra=tour&&inicio===0?1:0;
    let total=tour?(sinFotos?FOTOS:RESULTADO)+extra:6,numero=paso+extra;
    if(corporal&&!sinFotos){const base=continuar?2:7,esf=esfuerzo.progreso();total=base+recorrido.total+esf.total+1+extra;
      if(continuar)numero=paso===FOTOS?1:2;
      if(paso===ANGULOS)numero=base+1+recorrido.actual+extra;
      else if(paso===9||paso===10)numero=base+1+recorrido.total+esf.actual+extra;
      else if(paso===RESULTADO)numero=total;
    }
    document.querySelector('#pasoActual').textContent=`Paso ${numero} de ${total}`;
    if(tour){const progreso=form.querySelector('progress');progreso.max=total;progreso.value=numero;}
  }
  function armarMotivo(){
    if(!tour||continuar)return;
    const criterio=form.querySelector('[name="criterio"]:checked');
    form.elements.motivo.value=`Momento: ${criterio?.value==='MAS_INADECUADA'?'La postura más exigente':'La que más tiempo dura'}. Postura: ${form.elements.postura.value.trim()}.`;
  }
  function actualizarModo(){
    if(!tour||continuar)return;const puesto=form.elements.puesto;
    const tipo=puesto.tagName==='SELECT'?puesto.selectedOptions[0]?.dataset.tipo:puesto.dataset.tipo;
    const oficina=tipo==='ESCRITORIO';
    const chip=document.querySelector('[data-modo]');chip.textContent=oficina?'Oficina':'Planta';chip.hidden=!tipo;
    form.querySelector('[data-consejo-modo]').textContent=oficina?'Revisa la silla, la pantalla y el teclado':'Observa varios ciclos de la tarea antes de elegir';
    const grupo=form.querySelector('[data-posturas]'),orden=oficina?['computador','sentado','de_pie','inclinado','brazos_arriba','cargando','herramienta','agachado']:['de_pie','computador','sentado','inclinado','brazos_arriba','cargando','herramienta','agachado'];
    orden.forEach(k=>grupo.append(grupo.querySelector(`[data-postura="${k}"]`).closest('label')));
    if(puesto.tagName==='SELECT'&&puesto.value)document.querySelector('[data-contexto-puesto]').textContent=puesto.selectedOptions[0].textContent;
  }
  if(tour){
    form.querySelector('[data-analizar-foto]').closest('label').hidden=true;
    actualizarModo();memoria.restaurar();
    form.addEventListener('aplomo:analisis',actualizarGuia);
    form.querySelector('[data-editar-postura]')?.addEventListener('click',e=>{clearTimeout(avanceAutomatico);const campo=form.querySelector('[data-descripcion-postura]');campo.hidden=!campo.hidden;e.currentTarget.setAttribute('aria-expanded',String(!campo.hidden));if(!campo.hidden)form.elements.postura.focus();});
    form.addEventListener('input',()=>{armarMotivo();actualizarGuia();});
    form.addEventListener('change',e=>{
      clearTimeout(avanceAutomatico);const el=e.target;
      if(continuar){actualizarGuia();if(el.name==='lado_recorrido'&&respuestaCompleta()&&!reducirMovimiento())avanceAutomatico=setTimeout(()=>siguiente.click(),250);return;}
      if(el.name==='postura_opcion')form.elements.postura.value=el.value;
      if(el.name==='puesto')actualizarModo();
      const condiciones=['escritorio','pantalla','pies','apoyapies','apoyabrazos','regulable'];
      if(el.name==='ninguna'&&el.checked)condiciones.forEach(k=>form.elements[k].checked=false);
      if(condiciones.includes(el.name)&&el.checked)form.elements.ninguna.checked=false;
      form.elements.condiciones_respondidas.value=form.elements.ninguna.checked?'NINGUNA':condiciones.some(k=>form.elements[k].checked)?'SELECCION':'';
      armarMotivo();actualizarGuia();
      if(['postura_opcion','criterio','minutos','lado_recorrido'].includes(el.name)&&respuestaCompleta()&&!reducirMovimiento()&&(el.name!=='postura_opcion'||form.querySelector('[data-descripcion-postura]').hidden)){
        const actual=paso;avanceAutomatico=setTimeout(()=>{if(paso===actual&&respuestaCompleta())siguiente.click();},250);
      }
    });
  }
  form.querySelectorAll('[data-uso-guardada]').forEach(r=>r.addEventListener('change',()=>{const id=Number(r.closest('[data-foto-id]').dataset.fotoId);fotos.find(f=>f.id===id).uso=r.value;actualizarGuia();}));
  renderFotos();mostrar(paso);
})();
