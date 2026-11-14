/* 13F-3: tomas de una en una; metadatos explícitos por toma, sin cambiar el formato de guardado. */
window.crearFotosGuiadas=(form,obtenerFotos,aviso,alCambiar)=>{
  const raiz=form.querySelector('[data-fotos-guiadas]');if(!raiz)return null;
  let etapa='permiso',otro=false,adicional=0;
  const omitidas=new Set(),urls=new Map();
  const secuencia=()=>['permiso','perfil','frente','detalle',...(otro?['perfil_otro','frente_otro','detalle_otro']:[]),'resumen'];
  const base=()=>etapa.replace('_otro',''),foto=clave=>obtenerFotos().find(f=>f.slot===clave);
  const pantalla=()=>raiz.querySelector(`[data-foto-pantalla="${base()}"]`);
  const metadata=clave=>{const b=clave.replace('_otro',''),lado=clave.endsWith('_otro')?' · otro lado':'';return{vista:b==='perfil'?'PERFIL':b==='frente'?'FRENTE':'OTRA',descripcion:b==='perfil'?'Perfil'+lado:b==='frente'?'Frente'+lado:'Detalle de mano y muñeca',uso:b==='detalle'?'APOYO':'MEDIR'};};
  function enfocar(){const h=pantalla().querySelector('h2');h.tabIndex=-1;h.focus({preventScroll:true});window.scrollTo({top:0,behavior:'instant'});}
  function render(){
    const fotos=obtenerFotos();form.dataset.fotos=String(fotos.length);
    raiz.querySelectorAll('[data-foto-pantalla]').forEach(p=>p.hidden=p.dataset.fotoPantalla!==base());
    if(['perfil','frente','detalle'].includes(base())){
      const panel=pantalla(),f=foto(etapa),img=panel.querySelector('img');
      panel.querySelector('h2').textContent=(base()==='perfil'?'Foto de perfil':base()==='frente'?'Foto de frente':'Detalle de mano y muñeca')+(etapa.endsWith('_otro')?' · otro lado':'');
      img.onload=()=>img.closest('figure').style.setProperty('--foto-proporcion',img.naturalWidth/img.naturalHeight);
      img.src=f?f.preview:`/static/img/muestra_${base()}.jpg`;if(img.complete&&img.naturalHeight)img.onload();img.alt=f?metadata(etapa).descripcion+' de la evaluación':'Muestra de '+panel.querySelector('h2').textContent.toLowerCase();
      panel.querySelector('figcaption').textContent=f?'Tu foto':'Muestra';
      for(const k of ['tomar','subir'])panel.querySelector('[data-foto-'+k+']').hidden=!!f;
      for(const k of ['cambiar','quitar'])panel.querySelector('[data-foto-'+k+']').hidden=!f;
      panel.querySelector('[data-foto-omitir]').hidden=etapa==='perfil';
    }
    const mini=raiz.querySelector('[data-fotos-elegidas]');mini.replaceChildren();
    fotos.forEach((f,i)=>{const figura=document.createElement('figure'),img=document.createElement('img'),nombre=document.createElement('figcaption'),quitar=document.createElement('button');figura.className='foto-miniatura';img.src=f.preview;img.alt=f.descripcion;nombre.textContent=f.descripcion;quitar.type='button';quitar.className='btn sm';quitar.textContent='Quitar';quitar.setAttribute('aria-label','Quitar '+f.descripcion);quitar.addEventListener('click',()=>{retirar(f);if(f.slot==='perfil'){etapa='perfil';render();enfocar();}else render();});figura.append(img,nombre,quitar);mini.append(figura);});
    raiz.querySelector('[data-cuenta-fotos]').textContent=fotos.length+' de 10 fotos';
    raiz.querySelector('[data-foto-agregar]').disabled=fotos.length>=10;
    raiz.querySelector('[data-foto-otro-lado]').textContent=otro?'Revisar el otro lado':'+ Otro lado del cuerpo';
    const dificil=form.querySelector('[name="criterio"]:checked')?.value==='MAS_INADECUADA';
    raiz.querySelectorAll('[data-instruccion-foto]').forEach(p=>{p.textContent=p.closest('[data-foto-pantalla]').dataset.fotoPantalla==='detalle'?'Toma la foto de la mano, la muñeca y el inicio del antebrazo.':dificil?'Toma la foto justo cuando los brazos están más altos o la espalda más doblada.':'Toma la foto en la postura habitual, a mitad de la tarea.';});
    alCambiar();
  }
  function retirar(f){const fotos=obtenerFotos(),i=fotos.indexOf(f);if(i>=0)fotos.splice(i,1);if(urls.has(f.file)){URL.revokeObjectURL(urls.get(f.file));urls.delete(f.file);}}
  function recibir(input,extra=false){const nuevas=[...input.files];input.value='';if(!nuevas.length)return;const fotos=obtenerFotos(),anterior=extra?null:foto(etapa);
    if(fotos.length-(anterior?1:0)+nuevas.length>10){aviso('Solo puedes agregar hasta 10 fotos. Las fotos anteriores se conservan.');return;}
    if(nuevas.some(f=>!['image/jpeg','image/png'].includes(f.type))){aviso('Elige una foto JPG o PNG. Las fotos anteriores se conservan.');return;}
    if(nuevas.some(f=>f.size===0||f.size>5*1024*1024)){aviso('Cada foto debe tener contenido y pesar como máximo 5 MB. Las fotos anteriores se conservan.');return;}
    if(anterior)retirar(anterior);
    for(const file of nuevas){const preview=URL.createObjectURL(file);urls.set(file,preview);const meta=extra?{vista:'OTRA',descripcion:'Foto adicional '+(++adicional),uso:'APOYO'}:metadata(etapa);fotos.push({file,preview,slot:extra?'extra_'+adicional:etapa,...meta});}
    omitidas.delete(etapa);aviso('');render();
  }
  for(const panel of raiz.querySelectorAll('.foto-pregunta')){
    const cam=panel.querySelector('[data-foto-archivo="camara"]'),archivo=panel.querySelector('[data-foto-archivo="archivo"]');
    panel.querySelector('[data-foto-tomar]').addEventListener('click',()=>cam.click());for(const k of ['subir','cambiar'])panel.querySelector('[data-foto-'+k+']').addEventListener('click',()=>archivo.click());
    for(const input of [cam,archivo])input.addEventListener('change',()=>recibir(input));
    panel.querySelector('[data-foto-quitar]').addEventListener('click',()=>{const f=foto(etapa);if(f)retirar(f);render();});
    panel.querySelector('[data-foto-omitir]').addEventListener('click',()=>{omitidas.add(etapa);avanzar();});
  }
  const extra=raiz.querySelector('[data-foto-extra]');raiz.querySelector('[data-foto-agregar]').addEventListener('click',()=>extra.click());extra.addEventListener('change',()=>recibir(extra,true));
  raiz.querySelector('[data-foto-otro-lado]').addEventListener('click',()=>{otro=true;etapa='perfil_otro';render();enfocar();});
  function completa(){if(etapa==='permiso')return !!form.querySelector('[name="registro"]:checked');if(etapa==='resumen')return !!foto('perfil');return !!foto(etapa)||omitidas.has(etapa);}
  function avanzar(){if(etapa==='resumen')return false;if(!completa())return true;etapa=secuencia()[secuencia().indexOf(etapa)+1];render();enfocar();return true;}
  return{render,completa,avanzar,etapa:()=>etapa,
    registro(){if(form.querySelector('[name="registro"]:checked')?.value==='CON_FOTOS'&&etapa==='permiso')etapa='perfil';render();enfocar();},
    mostrar(){render();enfocar();},
    atras(){const n=secuencia().indexOf(etapa);if(n<=0)return false;etapa=secuencia()[n-1];render();enfocar();return true;},
    progreso(){return{actual:secuencia().indexOf(etapa),total:secuencia().length};},
    tienePerfil:()=>!!foto('perfil')
  };
};
