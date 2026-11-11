/* RF-023b, RNF-007: conserva el formulario ante errores de validación, conexión o sesión. */
(() => {
  document.querySelectorAll('[data-sugerencia-form]').forEach(form=>{
  const error=form.querySelector('[data-error-sugerencia]'),boton=form.querySelector('[type="submit"]'),etiqueta=boton.textContent;let enviando=false;
  const aviso=texto=>{error.hidden=!texto;error.querySelector('span').textContent=texto;if(texto)error.scrollIntoView({block:'nearest',behavior:'smooth'});};
  form.querySelector('[data-cerrar-error]').addEventListener('click',()=>aviso(''));
  form.addEventListener('submit',async e=>{
    e.preventDefault();if(enviando)return;enviando=true;boton.disabled=true;boton.textContent='Guardando…';aviso('');
    try{
      const r=await fetch(form.action,{method:'POST',body:new URLSearchParams(new FormData(form)),credentials:'same-origin',redirect:'manual',headers:{Accept:'text/plain'}});
      if(r.status===201){location.assign(r.headers.get('Location'));return;}
      if(r.status===403){
        const pagina=await fetch(location.href,{credentials:'same-origin',cache:'no-store'});
        const dom=pagina.ok?new DOMParser().parseFromString(await pagina.text(),'text/html'):null;
        const token=dom?.querySelector('[data-sugerencia-form] input[name="csrf"]');
        if(token&&token.value!==form.elements.csrf.value){form.elements.csrf.value=token.value;aviso('El formulario se actualizó. Tus campos se conservan; vuelve a guardar.');return;}
      }
      if(r.status===0||r.type==='opaqueredirect')aviso('Tu sesión venció. Inicia sesión en otra pestaña y vuelve aquí: tus campos se conservan.');
      else if(r.status>=500)aviso('No pudimos guardar. Tus campos siguen escritos; revisa la conexión e inténtalo de nuevo.');
      else{const dom=new DOMParser().parseFromString(await r.text(),'text/html');const mensaje=dom.querySelector('[role="alert"] span');aviso(mensaje?.textContent||'No pudimos guardar. Revisa los datos y tu sesión; tus campos se conservan.');}
    }catch(_){aviso('No se pudo conectar. Tus campos siguen escritos; revisa la conexión e inténtalo de nuevo.');}
    finally{enviando=false;boton.disabled=false;boton.textContent=etiqueta;}
  });
  });
})();
