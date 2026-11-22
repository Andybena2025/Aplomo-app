/* RF-033: conversación de agenda; navegación normal disponible sin JS y sin destinatarios de cita en el cliente. */
(function (global) {
  'use strict';
  function debeAbrir(evento) {
    return !evento.defaultPrevented && (evento.button == null || evento.button === 0) &&
      !evento.ctrlKey && !evento.metaKey && !evento.shiftKey && !evento.altKey;
  }
  function montar(doc, obtener) {
    const panel = doc.querySelector('[data-agenda-panel]');
    if (!panel || typeof panel.showModal !== 'function') return null;
    const contenido = panel.querySelector('[data-agenda-contenido]');
    let version = 0, guardando = false, actualizando = false, lanzador;
    function error(texto) {
      const salida = contenido.querySelector('[data-agenda-error]');
      if (salida) salida.textContent = texto;
      else contenido.textContent = texto;
    }
    function leer(html) {
      const documento = new DOMParser().parseFromString(html, 'text/html');
      if (!documento.querySelector('[data-agenda-url]')) throw new Error('No disponible');
      return documento;
    }
    function contador() {
      const texto = contenido.querySelector('[data-agenda-texto]');
      const salida = contenido.querySelector('[data-agenda-contador]');
      if (texto && salida) salida.textContent = `${texto.value.length} / 500`;
    }
    async function acuse(url, token, revision, sondeo = false) {
      try {
        const respuesta = await obtener(`${url}/leidos`, {method:'POST', credentials:'same-origin',
          headers:{'Content-Type':'application/x-www-form-urlencoded', 'X-Alineo-Agenda':'1',
            ...(sondeo ? {'X-Alineo-Sondeo':'1'} : {})},
          body:new URLSearchParams({csrf:token}).toString()});
        if (!respuesta.ok) throw new Error('Sin acuse');
      } catch (_) {
        if (version === revision && panel.open)
          error('Los mensajes se ven, pero no pudimos confirmar su lectura. Vuelve a abrir la cita.');
      }
    }
    async function abrir(url, origen) {
      const revision = ++version;
      lanzador = origen;
      contenido.textContent = 'Cargando mensajes…';
      if (!panel.open) panel.showModal();
      panel.querySelector('[data-agenda-cerrar]').focus();
      try {
        const respuesta = await obtener(url, {credentials:'same-origin', cache:'no-store',
          headers:{'X-Alineo-Agenda':'1'}});
        if (!respuesta.ok) throw new Error('No disponible');
        const html = await respuesta.text();
        leer(html);
        if (version !== revision || !panel.open) return;
        contenido.innerHTML = html;
        contador();
        const raiz = contenido.querySelector('[data-agenda-url]');
        await acuse(raiz.dataset.agendaUrl, raiz.dataset.agendaCsrf, revision);
      } catch (_) {
        if (version === revision && panel.open)
          error('No pudimos abrir los mensajes. Revisa tu conexión o vuelve a cargar la agenda.');
      }
    }
    function cerrar() {
      version++;
      panel.close();
      lanzador?.focus();
    }
    doc.addEventListener('click', evento => {
      const enlace = evento.target.closest('.cal-cita,.cal-proxima,[data-agenda-evaluador]');
      if (enlace && debeAbrir(evento)) {
        const href = enlace.getAttribute('href');
        const cita = /^\/reservas\/([1-9][0-9]*)$/.exec(href);
        const general = /^\/agenda\/evaluadores\/[1-9][0-9]*$/.test(href);
        if (!cita && !general) return;
        evento.preventDefault();
        abrir(cita ? `/agenda/citas/${cita[1]}` : href, enlace);
        return;
      }
      if (evento.target.closest('[data-agenda-cerrar]')) cerrar();
      const chip = evento.target.closest('[data-agenda-plantilla]');
      if (chip) {
        const texto = contenido.querySelector('[data-agenda-texto]');
        if (texto) { texto.value = chip.dataset.agendaPlantilla; contador(); texto.focus(); }
      }
    }, true);
    panel.addEventListener('cancel', () => { version++; });
    panel.addEventListener('close', () => { version++; lanzador?.focus(); });
    doc.addEventListener('input', evento => {
      if (evento.target.matches('[data-agenda-texto]')) contador();
    });
    doc.addEventListener('submit', async evento => {
      const form = evento.target;
      if (!form.matches('[data-agenda-enviar]')) return;
      evento.preventDefault();
      if (guardando) return;
      guardando = true;
      const revision = version;
      const boton = form.querySelector('button[type="submit"]');
      boton.disabled = true; boton.textContent = 'Enviando…';
      error('');
      try {
        const respuesta = await obtener(form.getAttribute('action'), {method:'POST', credentials:'same-origin',
          headers:{'Content-Type':'application/x-www-form-urlencoded', 'X-Alineo-Agenda':'1'},
          body:new URLSearchParams(new FormData(form)).toString()});
        if (!respuesta.ok) throw new Error('No enviado');
        const html = await respuesta.text();
        leer(html);
        if (version !== revision || !panel.open) return;
        contenido.innerHTML = html;
        contador();
        contenido.querySelector('[data-agenda-texto]')?.focus();
        const raiz = contenido.querySelector('[data-agenda-url]');
        await acuse(raiz.dataset.agendaUrl, raiz.dataset.agendaCsrf, revision);
      } catch (_) {
        if (version === revision && panel.open)
          error('No pudimos enviar el mensaje. El texto sigue aquí: revisa tu conexión e inténtalo de nuevo.');
      } finally { guardando = false; boton.disabled = false; boton.textContent = 'Enviar'; }
    }, true);
    async function actualizar() {
      if (!panel.open || doc.hidden || guardando || actualizando) return;
      const raiz = contenido.querySelector('[data-agenda-url]');
      if (!raiz) return;
      actualizando = true;
      const revision = version;
      try {
        const respuesta = await obtener(raiz.dataset.agendaUrl, {credentials:'same-origin', cache:'no-store',
          headers:{'X-Alineo-Agenda':'1', 'X-Alineo-Sondeo':'1'}});
        if (!respuesta.ok) return;
        const nuevo = leer(await respuesta.text()).querySelector('[data-cita-mensajes]');
        if (version !== revision || !panel.open) return;
        const lista = contenido.querySelector('[data-cita-mensajes]');
        if (nuevo && lista && lista.innerHTML !== nuevo.innerHTML) lista.innerHTML = nuevo.innerHTML;
        await acuse(raiz.dataset.agendaUrl, raiz.dataset.agendaCsrf, revision, true);
      } catch (_) { /* El siguiente intento continúa; nunca se borra el borrador. */ }
      finally { actualizando = false; }
    }
    return {abrir, actualizar};
  }
  if (typeof module !== 'undefined') module.exports = {montar, debeAbrir};
  if (global.document) {
    const control = montar(global.document, global.fetch.bind(global));
    if (control) global.setInterval(control.actualizar, 15000);
  }
})(typeof window !== 'undefined' ? window : globalThis);
