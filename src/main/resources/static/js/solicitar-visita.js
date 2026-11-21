/* RF-002: cambia solo los campos elegidos; las validaciones siempre vuelven a ejecutarse en el servidor. */
(() => {
  'use strict';
  const form = document.querySelector('[data-solicitar-visita]');
  if (!form) return;
  const valor = (nombre, texto) => {
    const campo = form.elements.namedItem(nombre);
    campo.value = texto;
    campo.dispatchEvent(new Event('input', {bubbles: true}));
    campo.dispatchEvent(new Event('change', {bubbles: true}));
  };
  const ingeniero = form.elements.namedItem('ingeniero');
  const mostrarIngeniero = () => {
    const opcion = ingeniero.selectedOptions[0];
    const color = form.querySelector('[data-ingeniero-color]');
    color.textContent = opcion?.value ? '● ' + opcion.textContent : '';
    if (/^#[A-Fa-f0-9]{6}$/.test(opcion?.dataset.color || '')) color.style.color = opcion.dataset.color;
  };
  ingeniero.addEventListener('change', mostrarIngeniero); mostrarIngeniero();
  form.elements.namedItem('sede').addEventListener('change', () => {
    const datos = new FormData(form);
    const consulta = new URLSearchParams();
    for (const [clave, texto] of datos) if (clave !== 'csrf' && clave !== 'salon') consulta.set(clave, texto);
    location.href = '/solicitar-visita?' + consulta;
  });
  form.addEventListener('click', event => {
    const hora = event.target.closest('[data-elegir-hora]');
    const ingeniero = event.target.closest('[data-elegir-ingeniero]');
    if (hora) { valor('inicio', hora.dataset.elegirHora); valor('fin', hora.dataset.fin); }
    if (ingeniero) valor('ingeniero', ingeniero.dataset.elegirIngeniero);
  });
})();
