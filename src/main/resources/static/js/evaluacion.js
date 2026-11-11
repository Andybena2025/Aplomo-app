/* RF-014/015/016, RNF-002/007: pasos del mismo formulario; la validación definitiva es del servidor. */
(() => {
  const form = document.querySelector('[data-evaluacion]');
  if (!form) return;
  let paso = 1, enviando = false;
  const error = form.querySelector('[data-error-evaluacion]');
  const atras = form.querySelector('[data-atras-evaluacion]');
  const siguiente = form.querySelector('[data-siguiente]');
  const guardar = form.querySelector('[data-guardar-evaluacion]');
  form.querySelectorAll('.slotImg img').forEach(img=>img.dataset.muestra=img.src);
  const elegidas = form.querySelector('[data-fotos-elegidas]');
  let fotos = [], urls = [];
  function aviso(texto) {
    error.hidden = !texto;
    error.querySelector('span').textContent = texto;
    if (texto) error.scrollIntoView({block:'nearest',behavior:'smooth'});
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
    paso = n;
    form.querySelectorAll('[data-paso]').forEach(s => s.hidden = Number(s.dataset.paso) !== paso);
    document.querySelectorAll('[data-indicador]').forEach(s => {
      const p = Number(s.dataset.indicador);
      s.classList.toggle('on',p === paso); s.classList.toggle('done',p < paso);
      if (p === paso) s.setAttribute('aria-current','step'); else s.removeAttribute('aria-current');
    });
    document.querySelector('#pasoActual').textContent = `Paso ${paso} de 6`;
    atras.textContent = paso === 1 ? 'Cancelar' : '← Atrás';
    siguiente.hidden = paso === 3; guardar.hidden = paso !== 3;
    aviso('');
    form.querySelector(`[data-paso="${paso}"] h2`).setAttribute('tabindex','-1');
    form.querySelector(`[data-paso="${paso}"] h2`).focus({preventScroll:true});
    window.scrollTo({top:0,behavior:'smooth'});
  }
  siguiente.addEventListener('click', () => {
    const controles = [...form.querySelectorAll(`[data-paso="${paso}"] input, [data-paso="${paso}"] select, [data-paso="${paso}"] textarea`)];
    if (controles.some(c => !c.reportValidity())) return;
    mostrar(paso + 1);
  });
  atras.addEventListener('click', () => {
    if (enviando) return;
    if (paso > 1) mostrar(paso - 1);
    else if (confirm('¿Salir del asistente? Los datos que todavía no guardaste se perderán.')) {
      location.assign('/visitas/' + form.elements.visita.value);
    }
  });
  form.querySelectorAll('.opt input').forEach(c => c.addEventListener('change', () => c.closest('.opt').classList.toggle('on', c.checked)));
  form.querySelectorAll('[name="registro"]').forEach(r => r.addEventListener('change', () => {
    const conFotos = r.value === 'CON_FOTOS';
    form.querySelector('[data-con-fotos]').hidden = !conFotos;
    if (!conFotos) form.elements.acepta.checked = false;
    aviso('');
  }));
  form.querySelector('[data-otro-lado]').addEventListener('click', e => {
    const panel = form.querySelector('[data-lado-extra]');
    panel.hidden = !panel.hidden;
    form.querySelectorAll('[data-lado-principal]').forEach(p=>p.hidden=!panel.hidden);
    e.currentTarget.setAttribute('aria-expanded',String(!panel.hidden));
    e.currentTarget.querySelector('b').textContent=panel.hidden?'+ Otro lado del cuerpo':'← Volver al lado principal';
    e.currentTarget.querySelector('.hint').textContent=panel.hidden?'Si dudas cuál lado tiene más carga, puedes registrar también el otro lado.':'Perfil y detalle del otro lado. La foto de frente sirve para ambos lados.';
  });
  function renderFotos() {
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
      vista.value=foto.vista; vista.addEventListener('change',()=>foto.vista=vista.value);
      const descLabel=document.createElement('label'); descLabel.textContent=`Descripción de la foto ${i+1}`;
      const desc=document.createElement('textarea');desc.className='input';desc.maxLength=200;desc.value=foto.descripcion;
      desc.setAttribute('aria-label',descLabel.textContent);desc.addEventListener('input',()=>foto.descripcion=desc.value);
      const quitar=document.createElement('button');quitar.type='button';quitar.className='btn sm';quitar.textContent=`Quitar foto ${i+1}`;
      quitar.addEventListener('click',()=>{fotos.splice(i,1);renderFotos();});
      campos.append(vistaLabel,vista,descLabel,desc,quitar);fila.append(img,campos);elegidas.append(fila);
    });
    form.querySelectorAll('.slot').forEach(slot=>{
      const f=fotos.find(f=>f.slot===slot.dataset.slotId), img=slot.querySelector('.slotImg img');
      img.src=f&&f.preview?f.preview:img.dataset.muestra;
      slot.classList.toggle('lista',!!f);slot.querySelector('.slotTop .tag').textContent=f?'✓ Lista':'Muestra';
      slot.querySelector('.slotLbl').textContent=f?'Tu foto':'Muestra';
    });
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
    if (paso!==3) {siguiente.click();return;}
    const registro=form.querySelector('[name="registro"]:checked');
    if (!registro) {aviso('Elige Con registro fotográfico o Sin registro fotográfico.');return;}
    const conFotos=registro.value==='CON_FOTOS';
    if (conFotos && !form.elements.acepta.checked) {aviso('Marca la aceptación de las fotos o elige Sin registro fotográfico.');return;}
    if (conFotos && (fotos.length<1 || fotos.length>10)) {aviso('Agrega de 1 a 10 fotos antes de guardar.');return;}
    if (conFotos && fotos.some(f=>!f.descripcion.trim() || f.descripcion.length>200)) {aviso('Cada foto necesita una descripción de hasta 200 caracteres.');return;}
    const data=new FormData(form);
    if (!conFotos) data.delete('acepta');
    if (conFotos) fotos.forEach((foto,i)=>{data.append(`foto_${i}`,foto.file);data.append(`foto_${i}_vista`,foto.vista);data.append(`foto_${i}_descripcion`,foto.descripcion);});
    enviando=true;guardar.disabled=true;atras.disabled=true;guardar.textContent='Guardando…';aviso('');
    try {
      const respuesta=await fetch(form.action,{method:'POST',body:data,credentials:'same-origin',redirect:'manual'});
      if (respuesta.status===201) {location.assign(respuesta.headers.get('Location'));return;}
      if (respuesta.status===0 || respuesta.type==='opaqueredirect') aviso('Tu sesión venció. Inicia sesión en otra pestaña y vuelve aquí: tus datos siguen escritos.');
      else if (respuesta.status===403 && await actualizarFormulario()) aviso('El formulario se actualizó. Tus campos y fotos se conservan; vuelve a pulsar Guardar evaluación.');
      else if ((respuesta.headers.get('Content-Type')||'').startsWith('text/plain')) aviso(await respuesta.text());
      else aviso('No pudimos guardar. Comprueba tu sesión en otra pestaña y vuelve a intentarlo aquí; tus datos se conservan.');
    } catch (_) {aviso('No se pudo conectar. Tus campos y fotos se conservan; revisa la conexión e inténtalo de nuevo.');}
    finally {enviando=false;guardar.disabled=false;atras.disabled=false;guardar.textContent='Guardar evaluación';}
  });
  renderFotos();mostrar(1);
})();
