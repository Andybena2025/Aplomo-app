/* RNF-002, RNF-007: avisos accesibles y envío con indicador; las reglas se validan en Java. */
document.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => b.closest('.aviso').remove()));
document.querySelectorAll('[data-confirmacion]').forEach(a => setTimeout(() => a.remove(), 5000));
document.querySelectorAll('form:not([data-filtros-usuarios]):not([data-alta-reserva]):not([data-evaluacion]):not([data-pendiente-form]):not([data-sugerencia-form])').forEach(f => f.addEventListener('submit', () => {
  const b = f.querySelector('button[type="submit"],button:not([type])');
  if (b) { b.disabled = true; b.textContent = 'Procesando…'; }
}));
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
