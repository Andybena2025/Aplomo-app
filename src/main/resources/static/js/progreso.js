/* RNF-003/007: indicador neutro después de un segundo; no altera reglas ni solicitudes. */
(() => {
  const estado=document.createElement('div');estado.className='aviso progreso';estado.hidden=true;
  estado.setAttribute('role','status');estado.setAttribute('aria-live','polite');document.body.append(estado);
  let pendientes=0,temporizador;const confirmaciones=new WeakMap(),etiquetas=new WeakMap();
  function limpiar(){clearTimeout(temporizador);pendientes=0;estado.hidden=true;estado.textContent='';document.documentElement.removeAttribute('aria-busy');}
  function iniciar(texto='Procesando…'){
    pendientes++;if(pendientes===1)temporizador=setTimeout(()=>{estado.textContent=texto;estado.hidden=false;document.documentElement.setAttribute('aria-busy','true');},1000);
    let terminado=false;return ()=>{if(terminado)return;terminado=true;pendientes=Math.max(0,pendientes-1);if(!pendientes)limpiar();};
  }
  function confirmar(elemento,texto){
    clearTimeout(confirmaciones.get(elemento));
    elemento.replaceChildren(document.createTextNode(texto));elemento.classList.add('aviso');elemento.setAttribute('role','status');
    confirmaciones.set(elemento,setTimeout(()=>{if(elemento.textContent===texto){elemento.replaceChildren();elemento.classList.remove('aviso');}},4000));
  }
  window.AplomoProgreso={iniciar,confirmar};
  document.addEventListener('submit',e=>{if(!e.defaultPrevented)iniciar(e.target.method.toUpperCase()==='GET'?'Cargando…':'Guardando…');});
  document.addEventListener('click',e=>{
    const a=e.target.closest('a[href]');if(!a||e.defaultPrevented||e.button!==0||e.metaKey||e.ctrlKey||e.shiftKey||e.altKey||a.target||a.hasAttribute('download'))return;
    const destino=new URL(a.href,location.href);if(destino.origin!==location.origin||!['http:','https:'].includes(destino.protocol)||destino.href===location.href||a.getAttribute('href').startsWith('#'))return;
    if(a.hasAttribute('data-navegando')){e.preventDefault();return;}
    a.setAttribute('data-navegando','');a.setAttribute('aria-disabled','true');
    if(a.classList.contains('btn')){const original=document.createDocumentFragment();while(a.firstChild)original.append(a.firstChild);etiquetas.set(a,original);a.textContent='Cargando…';}
    iniciar('Cargando…');
  });
  window.addEventListener('pageshow',()=>{limpiar();document.querySelectorAll('[data-navegando]').forEach(a=>{const original=etiquetas.get(a);if(original){a.replaceChildren(original);etiquetas.delete(a);}a.removeAttribute('data-navegando');a.removeAttribute('aria-disabled');});});
})();
