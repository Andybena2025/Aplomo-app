(() => {
 'use strict';
 const tarjeta = document.querySelector('[data-ayuda-inicio]');
 if (!tarjeta) return;
 const clave = 'alineo-ayuda-inicio-oculta';
 try { tarjeta.hidden = sessionStorage.getItem(clave) === '1'; } catch (_) { /* La ayuda sigue disponible. */ }
 tarjeta.querySelector('[data-cerrar-ayuda-inicio]')?.addEventListener('click', () => {
  tarjeta.hidden = true;
  try { sessionStorage.setItem(clave, '1'); } catch (_) { /* Se oculta hasta volver a cargar. */ }
 });
})();
