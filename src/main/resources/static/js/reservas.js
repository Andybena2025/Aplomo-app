/* RF-011: catálogos dependientes y altas sin perder los campos de la reserva. */
(() => {
  const reserva = document.querySelector('form[data-reserva]');
  if (!reserva) return;

  const altas = [...document.querySelectorAll('form[data-alta-reserva]')];
  const aviso = document.querySelector('[data-aviso-reserva]');
  const controles = () => ({
    empresa: reserva.elements.namedItem('empresa'),
    sede: reserva.elements.namedItem('sede'),
    salon: reserva.elements.namedItem('salon')
  });

  function filtrar(selector, padre) {
    for (const opcion of selector.options) {
      const disponible = !opcion.value || opcion.dataset.padre === padre;
      opcion.hidden = !disponible;
      opcion.disabled = !disponible;
    }
    if (selector.selectedOptions[0]?.disabled) selector.value = '';
  }

  function actualizar() {
    const campos = controles();
    filtrar(campos.sede, campos.empresa.value);
    filtrar(campos.salon, campos.sede.value);
    for (const alta of altas) {
      const empresa = alta.elements.namedItem('empresa');
      const sede = alta.elements.namedItem('sede');
      if (empresa) empresa.value = campos.empresa.value;
      if (sede) sede.value = campos.sede.value;
    }
  }

  reserva.addEventListener('change', evento => {
    if (['empresa', 'sede', 'salon'].includes(evento.target.name)) actualizar();
  });
  actualizar();

  for (const alta of altas) alta.addEventListener('submit', async evento => {
    evento.preventDefault();
    if(alta.dataset.enviando)return;
    const error = alta.querySelector('[data-error-alta]');
    const boton = alta.querySelector('button');
    const etiqueta = boton.textContent;
    error.textContent = '';
    const campos = controles();
    if (alta.dataset.altaReserva === 'sede' && !campos.empresa.value) {
      error.textContent = 'Selecciona primero la empresa de la nueva sede.';
      return;
    }
    if (alta.dataset.altaReserva === 'salon' && !campos.sede.value) {
      error.textContent = 'Selecciona primero la sede del nuevo salón.';
      return;
    }
    alta.dataset.enviando='si';
    const terminarProgreso=window.AplomoProgreso?.iniciar('Guardando el lugar…');
    boton.disabled = true;
    boton.textContent = 'Guardando…';
    try {
      const respuesta = await fetch(alta.action, {
        method: 'POST',
        credentials: 'same-origin',
        body: new URLSearchParams(new FormData(alta))
      });
      const datos = new URLSearchParams(await respuesta.text());
      if (!respuesta.ok || !datos.get('id')) {
        throw new Error(datos.get('aviso') || 'No pudimos guardar. Revisa tu sesión e inténtalo de nuevo.');
      }
      const seleccion = new URLSearchParams({
        empresa: campos.empresa.value, sede: campos.sede.value, salon: campos.salon.value
      });
      const tipo = alta.dataset.altaReserva;
      seleccion.set(tipo, datos.get('id'));
      if (tipo === 'empresa') { seleccion.set('sede', ''); seleccion.set('salon', ''); }
      if (tipo === 'sede') seleccion.set('salon', '');
      const respuestaCatalogo = await fetch('/reservas/catalogo?' + seleccion, {credentials: 'same-origin'});
      const fragmento = new DOMParser().parseFromString(await respuestaCatalogo.text(), 'text/html');
      const catalogo = fragmento.querySelector('[data-catalogo-reserva]');
      if (!respuestaCatalogo.ok || !catalogo) {
        throw new Error('El lugar se guardó, pero no pudimos actualizar la lista. Recarga cuando hayas conservado los datos de tu reserva.');
      }
      reserva.querySelector('[data-catalogo-reserva]').replaceWith(catalogo);
      alta.reset();
      actualizar();
      alta.closest('details').open = false;
      aviso.textContent = datos.get('aviso') || 'Lugar registrado. Puedes continuar con la reserva.';
      aviso.classList.add('aviso');
      if(!datos.get('aviso')||datos.get('aviso')==='Lugar registrado. Puedes continuar con la reserva.')window.AplomoProgreso?.confirmar(aviso,aviso.textContent);
    } catch (fallo) {
      error.textContent = fallo instanceof TypeError ? 'No se pudo conectar. Tus datos siguen escritos; revisa la conexión e inténtalo de nuevo.' : fallo.message || 'No pudimos completar la acción. Tus datos siguen escritos.';
    } finally {
      terminarProgreso?.();delete alta.dataset.enviando;
      boton.disabled = false;
      boton.textContent = etiqueta;
    }
  });
})();
