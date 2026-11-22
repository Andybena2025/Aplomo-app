/* RNF-007: enlaces a artículos utilizables sin JS; conserva enlaces anteriores y respuesta local de utilidad. */
(() => {
  if (!location.search && /^#[a-z-]+$/.test(location.hash)) {
    const tema = location.hash.slice(1);
    const enlace = document.querySelector(`[data-tema-id="${tema}"]`);
    if (enlace) location.replace(enlace.href);
  }
  document.querySelectorAll('[data-util]').forEach(boton => {
    boton.addEventListener('click', () => {
      boton.parentElement.querySelectorAll('[data-util]').forEach(otro => {
        otro.setAttribute('aria-pressed', String(otro === boton));
      });
    });
  });
})();
