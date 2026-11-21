/* RF-001/003, RNF-002: control compartido de visibilidad, sin conservar ni enviar contraseñas. */
(function (global) {
  'use strict';
  function alternar(campo, boton) {
    const visible = campo.type === 'password';
    campo.type = visible ? 'text' : 'password';
    boton.textContent = visible ? 'Ocultar' : '👁 Mostrar';
    boton.setAttribute('aria-label', visible ? 'Ocultar contraseña' : 'Mostrar contraseña');
    boton.setAttribute('aria-pressed', String(visible));
  }
  function montar(doc) {
    doc.querySelectorAll('input[type="password"],input[data-clave-temporal]').forEach(campo => {
      if (campo.closest('.clave-caja')) return;
      const caja = doc.createElement('span'); caja.className = 'clave-caja';
      campo.before(caja); caja.append(campo); campo.type = 'password';
      const boton = doc.createElement('button'); boton.type = 'button'; boton.className = 'clave-mostrar';
      boton.textContent = '👁 Mostrar'; boton.setAttribute('aria-label', 'Mostrar contraseña');
      boton.setAttribute('aria-pressed', 'false');
      boton.addEventListener('click', () => alternar(campo, boton)); caja.append(boton);
    });
  }
  if (typeof module !== 'undefined') module.exports = {alternar, montar};
  if (global.document) montar(global.document);
})(typeof window !== 'undefined' ? window : globalThis);
