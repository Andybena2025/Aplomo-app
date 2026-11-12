/* Presentación: el dibujo se anima una vez por sesión, exclusivamente en Inicio. */
(() => {
  const fondo = document.querySelector('.fondo');
  if (!fondo || location.pathname !== '/') return;
  try {
    if (sessionStorage.getItem('aplomo-fondo-visto')) return;
    sessionStorage.setItem('aplomo-fondo-visto', '1');
    if (!matchMedia('(prefers-reduced-motion: reduce)').matches) fondo.classList.add('animar');
  } catch (_) { /* Sin almacenamiento disponible, el dibujo queda terminado. */ }
})();
