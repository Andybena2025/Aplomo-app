/* RNF-002, RNF-007: avisos accesibles y envío con indicador; las reglas se validan en Java. */
document.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => b.closest('.aviso').remove()));
document.querySelectorAll('[data-confirmacion]').forEach(a => setTimeout(() => a.remove(), 5000));
const formulariosNormales=document.querySelectorAll('form:not([data-filtros-usuarios]):not([data-alta-reserva]):not([data-evaluacion]):not([data-pendiente-form]):not([data-sugerencia-form])');
formulariosNormales.forEach(f=>f.addEventListener('submit',e=>{
  if(e.defaultPrevented)return;
  if(f.dataset.enviando){e.preventDefault();return;}
  const b=e.submitter||f.querySelector('button[type="submit"],button:not([type])');
  // El botón desactivado deja de enviarse: conserva su nombre/valor antes de bloquearlo.
  f.querySelectorAll('[data-submitter-envio]').forEach(el=>el.remove());
  if(b?.name){const elegido=document.createElement('input');elegido.type='hidden';elegido.name=b.name;elegido.value=b.value;elegido.dataset.submitterEnvio='si';f.append(elegido);}
  f.dataset.enviando='si';
  const guardar=f.method.toUpperCase()==='POST'&&!['/login','/logout'].includes(new URL(f.action,location.href).pathname);
  if(b){b.dataset.textoAnterior=b.textContent;b.dataset.envioNormal='si';b.disabled=true;b.textContent=guardar?'Guardando…':'Cargando…';}
}));
/* Volver con el historial restaura controles conservados por la caché del navegador. */
window.addEventListener('pageshow',()=>{
  formulariosNormales.forEach(f=>{delete f.dataset.enviando;f.querySelectorAll('[data-submitter-envio]').forEach(el=>el.remove());});
  document.querySelectorAll('[data-envio-normal]').forEach(b=>{b.disabled=false;b.textContent=b.dataset.textoAnterior;delete b.dataset.textoAnterior;delete b.dataset.envioNormal;});
});
/* Resaltar la pestaña activa según la ruta actual (como el mock). */
(function(){
  var path = location.pathname;
  document.querySelectorAll('.nav-tab').forEach(function(tab){
    var href = tab.getAttribute('href');
    const seccion = /^\/(sedes|salones|areas|cargos|puestos)(?:\/|$)/.test(path) ? '/empresas' : path;
    if (href === '/' ? path === '/' : seccion.startsWith(href)) { tab.classList.add('on'); tab.setAttribute('aria-current', 'page'); }
  });
})();
const rol = document.querySelector('#rol');
const empresa = document.querySelector('#empresa');
if (rol && empresa) {
  const actualizarEmpresa = () => {
    const requiere = rol.value === 'USUARIO';
    empresa.required = requiere;
    empresa.disabled = !requiere;
    empresa.closest('[data-empresa]').hidden = !requiere;
    if (!requiere) empresa.value = '';
  };
  rol.addEventListener('change', actualizarEmpresa);
  actualizarEmpresa();
}

// RF-003: el rol maestro no recibe asignaciones; el servidor vuelve a validarlo.
const grupoEmpresas=document.querySelector('[data-empresas]');
if(rol&&grupoEmpresas){const actualizar=()=>{const oculta=rol.value==='ADMINISTRADOR';grupoEmpresas.hidden=oculta;grupoEmpresas.querySelectorAll('input').forEach(i=>{i.disabled=oculta;});};rol.addEventListener('change',actualizar);actualizar();}
