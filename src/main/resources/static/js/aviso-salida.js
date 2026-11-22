/* Paso 38 (RNF-007): si hay respuestas o fotos sin guardar, avisa antes de salir de la evaluación. */
(() => {
  const form = document.querySelector('form[data-evaluacion],form[data-revision]');
  if (!form) return;
  let sucio = false;
  const marcar = () => { sucio = true; };
  form.addEventListener('input', marcar, true);
  form.addEventListener('change', marcar, true);
  window.AlineoSalida = {limpiar() { sucio = false; }, sucio: () => sucio};
  // Guardado correcto (201): ya no hay nada que perder.
  const original = window.fetch.bind(window);
  window.fetch = async (...args) => {
    const r = await original(...args);
    const metodo = (args[1]?.method || 'GET').toUpperCase();
    if (metodo === 'POST' && r.status === 201) sucio = false;
    return r;
  };
  // Recargar, cerrar la pestaña o escribir otra dirección (computador).
  window.addEventListener('beforeunload', e => {
    if (!sucio) return;
    e.preventDefault();
    e.returnValue = '';
  });
  // Tocar el menú u otro enlace de la página (también en iPad).
  document.addEventListener('click', e => {
    const a = e.target.closest('a[href]');
    if (!a || e.defaultPrevented || a.target === '_blank' || a.getAttribute('href').startsWith('#')) return;
    if (form.contains(a)) { sucio = false; return; }
    if (!sucio) return;
    if (confirm('Tienes respuestas sin guardar en esta evaluación. Si sales ahora, se perderán. ¿Salir de todos modos?'))
      sucio = false;
    else e.preventDefault();
  }, true);
})();
