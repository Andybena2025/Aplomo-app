/* RF-002/033: bandeja de los tres roles; solo Entendido acusa recibo en el servidor. */
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
    const contador = doc.querySelector('[data-contador-avisos]');
    const vistos = new Set();
    let ocupado = false;
    let lanzador;
    async function abrir(url = '/avisos') {
      const respuesta = await obtener(url, {credentials: 'same-origin', cache: 'no-store',
        headers: {'X-Alineo-Panel': '1'}});
      if (!respuesta.ok) return;
      panel.querySelector('[data-contenido-avisos]').innerHTML = await respuesta.text();
      if (!panel.open) panel.showModal();
      panel.querySelector('button')?.focus();
    }
    doc.addEventListener('click', event => {
      const boton = event.target.closest('[data-abrir-avisos]');
      const enlace = event.target.closest('[data-lista-avisos]');
      if (boton || enlace) {
        event.preventDefault();
        lanzador = boton || lanzador;
        abrir(enlace ? enlace.getAttribute('href') : '/avisos').catch(() => { global.location.href = '/avisos'; });
      }
      if (event.target.closest('[data-cerrar-avisos]')) { panel.close(); lanzador?.focus(); }
    });
    const mostrar = nota => {
      let region = doc.querySelector('[data-avisos-notas]');
      if (!region) {
        region = doc.createElement('aside');
        region.className = 'notas-vivas'; region.dataset.avisosNotas = '';
        region.setAttribute('aria-live', 'polite'); doc.body.append(region);
      }
      const aviso = doc.createElement('section'); aviso.className = 'card stack';
      const titulo = doc.createElement('strong'); titulo.textContent = nota.tipo || 'Aviso nuevo';
      const texto = doc.createElement('p'); texto.textContent = nota.texto;
      const boton = doc.createElement('button'); boton.type = 'button'; boton.className = 'btn';
      boton.textContent = 'Ver avisos'; boton.addEventListener('click', () => abrir());
      const cerrar = doc.createElement('button'); cerrar.type = 'button'; cerrar.className = 'btn';
      cerrar.textContent = 'Cerrar'; cerrar.addEventListener('click', () => aviso.remove());
      aviso.append(titulo, texto, boton, cerrar); region.replaceChildren(aviso);
    };
    return async function consultar() {
      if (doc.hidden || ocupado) return;
      ocupado = true;
      try {
        const respuesta = await obtener('/avisos/nuevos', {credentials: 'same-origin', cache: 'no-store'});
        if (respuesta.status === 401) {
          if (contador) { contador.hidden = false; contador.textContent = '!'; }
          procesar([{id: 'sesion-vencida', tipo: 'Sesión cerrada', texto: 'Tu sesión terminó. Vuelve a iniciar sesión.'}], vistos, mostrar);
          return;
        }
        if (!respuesta.ok) return;
        const notas = await respuesta.json();
        if (contador) { contador.hidden = !notas.length; contador.textContent = String(notas.length); }
        procesar(notas, vistos, mostrar);
      } catch (_) { /* Una desconexión no marca leído ni impide el siguiente intento. */ }
      finally { ocupado = false; }
    };
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
