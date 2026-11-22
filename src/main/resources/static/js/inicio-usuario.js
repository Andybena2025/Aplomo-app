/* RF-033: ficha en línea; un contacto abierto, con botones nativos utilizables con teclado. */
(() => {
 'use strict';
 const tarjeta = document.querySelector('[data-ayuda-inicio]');
 if (!tarjeta) return;
 const botones = [...tarjeta.querySelectorAll('[data-contacto-inicio]')];
 botones.forEach(boton => boton.addEventListener('click', () => {
  const abrir = boton.getAttribute('aria-expanded') !== 'true';
  botones.forEach(otro => {
   const ficha = document.getElementById(otro.getAttribute('aria-controls'));
   const activo = otro === boton && abrir;
   otro.setAttribute('aria-expanded', String(activo));
   if (ficha) ficha.hidden = !activo;
  });
 }));
})();
