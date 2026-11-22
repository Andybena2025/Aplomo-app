/* RF-002/033: mensajes en ventana local; solo Entendido acusa recibo con CSRF. */
(function (global) {
  'use strict';
  function procesar(notas, vistas, mostrar) {
    [...notas].reverse().forEach(nota => {
      const id = String(nota.id ?? `${nota.reserva}:${nota.texto}`);
      if (vistas.has(id)) return;
      mostrar(nota);
      vistas.add(id);
    });
  }
  function montar(doc, obtener) {
    const campo = doc.querySelector('[data-notas-csrf]');
    if (!campo) return null;
    const panel = doc.querySelector('[data-panel-avisos]');
    const modal = doc.querySelector('[data-aviso-dialog]');
    const contador = doc.querySelector('[data-contador-avisos]');
    const burbuja = doc.querySelector('[data-avisos-saludo]');
    let notas = [], actual = null, ocupado = false, guardando = false, oculta = false, lanzador, revision = 0;
    function pintar(cantidad = notas.length) {
      if (contador) { contador.hidden = cantidad === 0; contador.textContent = String(cantidad); }
      if (burbuja) {
        burbuja.hidden = oculta || cantidad === 0;
        const boton = burbuja.querySelector?.('[data-ver-aviso-saludo]');
        if (boton) boton.textContent = `Tienes ${cantidad} ${cantidad === 1 ? 'aviso' : 'avisos'} · Ver ›`;
      }
    }
    async function abrir(url = '/avisos') {
      const respuesta = await obtener(url, {credentials: 'same-origin', cache: 'no-store',
        headers: {'X-Alineo-Panel': '1'}});
      if (!respuesta.ok) return;
      panel.querySelector('[data-contenido-avisos]').innerHTML = await respuesta.text();
      if (!panel.open) panel.showModal();
      panel.querySelector('button')?.focus();
    }
    function abrirNota(nota) {
      if (!nota || !modal?.showModal) return;
      actual = nota;
      modal.querySelector('[data-aviso-titulo]').textContent = nota.tipo || 'Aviso';
      modal.querySelector('[data-aviso-texto]').textContent = nota.texto;
      modal.querySelector('[data-aviso-error]').textContent = '';
      modal.querySelector('[data-aviso-entendido]').hidden = nota.leido === true;
      if (!modal.open) modal.showModal();
      modal.querySelector(nota.leido ? '[data-aviso-cerrar]' : '[data-aviso-entendido]').focus();
    }
    async function entender() {
      if (!actual || guardando) return;
      const boton = modal.querySelector('[data-aviso-entendido]');
      guardando = true; boton.disabled = true;
      try {
        const cuerpo = new URLSearchParams({csrf: campo.dataset.notasCsrf});
        const respuesta = await obtener(`/avisos/${actual.id}/entendido`, {method:'POST',
          credentials:'same-origin', headers:{'Content-Type':'application/x-www-form-urlencoded',
            'X-Alineo-Panel':'1'}, body:cuerpo.toString()});
        if (!respuesta.ok) throw new Error('No guardado');
        const datos = await respuesta.json();
        notas = notas.filter(nota => String(nota.id) !== String(actual.id));
        doc.querySelector(`[data-mensaje="${actual.id}"]`)?.remove();
        pintar(datos.nuevos);
        revision++; actual = null; modal.close();
        (lanzador?.isConnected ? lanzador : doc.querySelector('[data-abrir-avisos]'))?.focus();
      } catch (_) {
        modal.querySelector('[data-aviso-error]').textContent = 'No pudimos confirmar la lectura. Inténtalo de nuevo.';
      } finally { guardando = false; boton.disabled = false; }
    }
    doc.addEventListener('click', event => {
      const objetivo = event.target;
      if (event.defaultPrevented) return;
      if (objetivo.closest('a[href]') && (event.button > 0 || event.ctrlKey || event.metaKey ||
          event.shiftKey || event.altKey)) return;
      if (objetivo.closest('[data-aviso-entendido]')) { entender(); return; }
      if (objetivo.closest('[data-aviso-cerrar]')) { modal.close(); lanzador?.focus(); return; }
      if (objetivo.closest('[data-ocultar-aviso]')) { oculta = true; pintar(); return; }
      const saludo = objetivo.closest('[data-ver-aviso-saludo]');
      const ver = objetivo.closest('[data-ver-mensaje]');
      const tarjeta = objetivo.closest('[data-mensaje]');
      if (saludo || ver || tarjeta && !objetivo.closest('a,button,summary,textarea,input,form')) {
        event.preventDefault(); lanzador = saludo || ver || tarjeta;
        if (saludo) abrirNota(notas[0]);
        else {
          const id = (ver || tarjeta).dataset.verMensaje || tarjeta.dataset.mensaje;
          const nota = notas.find(n => String(n.id) === id) || {id,
            tipo: tarjeta.querySelector('h3').textContent,
            texto: tarjeta.querySelector('[data-texto-mensaje]').textContent,
            leido:!tarjeta.classList.contains('nuevo')};
          abrirNota(nota);
        }
        return;
      }
      const boton = objetivo.closest('[data-abrir-avisos]');
      const enlace = objetivo.closest('[data-lista-avisos]');
      if (boton || enlace) {
        event.preventDefault(); lanzador = boton || enlace;
        abrir(enlace ? enlace.getAttribute('href') : '/avisos').catch(() => {});
      }
      if (objetivo.closest('[data-cerrar-avisos]')) { panel.close(); lanzador?.focus(); }
    }, true);
    doc.addEventListener('input', event => {
      const texto = event.target.closest('[data-nuevo-aviso-texto]');
      if (texto) texto.closest('form').querySelector('[data-nuevo-aviso-contador]').textContent =
        `${texto.value.length} / 500`;
    });
    doc.addEventListener('submit', async event => {
      const form = event.target;
      if (form.matches('[data-nuevo-aviso]') && panel.open && panel.contains(form)) {
        event.preventDefault();
        if (guardando) return;
        guardando = true;
        const boton = form.querySelector('button[type="submit"]');
        const error = form.querySelector('[data-nuevo-aviso-error]');
        boton.disabled = true; boton.textContent = 'Enviando…'; error.textContent = '';
        try {
          const respuesta = await obtener('/avisos/nuevo', {method:'POST', credentials:'same-origin',
            headers:{'Content-Type':'application/x-www-form-urlencoded', 'X-Alineo-Panel':'1'},
            body:new URLSearchParams(new FormData(form)).toString()});
          if (!respuesta.ok && respuesta.status !== 400) throw new Error('No enviado');
          const html = await respuesta.text();
          const fragmento = new DOMParser().parseFromString(html, 'text/html');
          if (!fragmento.querySelector('[data-bandeja-avisos],[data-nuevo-aviso]')) throw new Error('No disponible');
          if (!panel.open || !panel.contains(form)) return;
          panel.querySelector('[data-contenido-avisos]').innerHTML = html;
          panel.querySelector('button')?.focus();
          revision++;
        } catch (_) {
          if (panel.open && panel.contains(form))
            error.textContent = 'No pudimos enviar el mensaje. Conservamos el texto: revisa el destinatario o inténtalo de nuevo.';
        } finally { guardando = false; boton.disabled = false; boton.textContent = 'Enviar mensaje'; }
        return;
      }
      const ruta = form.getAttribute('action') || '';
      if (/^\/avisos\/[1-9][0-9]*\/entendido$/.test(ruta)) {
        event.preventDefault();
        const id = ruta.split('/')[2];
        const nota = notas.find(n => String(n.id) === id);
        if (nota) { abrirNota(nota); entender(); }
      }
    }, true);
    async function consultar() {
      if (doc.hidden || ocupado || guardando) return;
      ocupado = true;
      const version = revision;
      try {
        const respuesta = await obtener('/avisos/nuevos', {credentials:'same-origin', cache:'no-store'});
        if (respuesta.status === 401) {
          if (contador) { contador.hidden = false; contador.textContent = '!'; }
          return;
        }
        if (!respuesta.ok) return;
        const nuevas = await respuesta.json();
        if (version !== revision) return;
        notas = nuevas; pintar();
      } catch (_) { /* No se marca leído ni se bloquea el siguiente intento. */ }
      finally { ocupado = false; }
    }
    return Object.assign(consultar, {abrirNota, entender});
  }
  if (typeof module !== 'undefined') module.exports = {procesar, montar};
  if (global.document) {
    const consultar = montar(global.document, global.fetch.bind(global));
    if (consultar) {
      consultar(); global.setInterval(consultar, 15000);
      global.document.addEventListener('visibilitychange', consultar);
    }
  }
})(typeof window !== 'undefined' ? window : globalThis);
