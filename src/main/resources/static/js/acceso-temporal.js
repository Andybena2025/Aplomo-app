/* RF-001/003: solo copia tras un toque; no conserva ni registra la contraseña. */
(() => {
  'use strict';
  const boton = document.querySelector('[data-copiar-temporal]');
  if (!boton) return;
  boton.addEventListener('click', async () => {
    const campo = document.querySelector('[data-clave-temporal]');
    const estado = document.querySelector('[data-copia-estado]');
    try {
      await navigator.clipboard.writeText(campo.value);
      estado.textContent = 'Contraseña copiada.';
    } catch (_) {
      campo.focus(); campo.select();
      estado.textContent = 'La contraseña está seleccionada. Mantén pulsado y elige Copiar.';
    }
  });
})();
