/* RF-033: sondeo privado; una pestaña oculta no consulta ni mantiene viva la sesión. */
(function (global) {
  'use strict';
  function clave(nota) { return `${nota.reserva}:${nota.texto}`; }
  function procesar(notas, almacenamiento, mostrar) {
    notas.forEach(nota => {
      const id = `alineo-nota:${clave(nota)}`;
      let vista = false;
      try { vista = almacenamiento.getItem(id) === '1'; } catch (_) { /* Puede estar deshabilitado. */ }
      if (vista) return;
      mostrar(nota);
      try { almacenamiento.setItem(id, '1'); } catch (_) { /* El servidor conserva el acuse. */ }
    });
  }
  function montar(doc, obtener, almacenamiento) {
    const campo = doc.querySelector('[data-notas-csrf]');
    if (!campo) return null;
    let ocupado = false;
    const mostrar = nota => {
      let region = doc.querySelector('[data-avisos-notas]');
      if (!region) {
        region = doc.createElement('aside');
        region.className = 'notas-vivas stack';
        region.dataset.avisosNotas = '';
        region.setAttribute('aria-live', 'polite');
        doc.body.append(region);
      }
      const aviso = doc.createElement('section');
      aviso.className = 'card stack';
      const titulo = doc.createElement('strong');
      titulo.textContent = 'Nota nueva del Administrador';
      const fecha = doc.createElement('small');
      fecha.textContent = `${nota.fecha} · ${nota.hora} · ${nota.empresa}`;
      const texto = doc.createElement('p');
      texto.textContent = nota.texto;
      const enlace = doc.createElement('a');
      enlace.className = 'btn';
      enlace.href = `/reservas/${Number(nota.reserva)}`;
      enlace.textContent = 'Ver cita';
      const form = doc.createElement('form');
      form.method = 'post';
      form.action = `/reservas/${Number(nota.reserva)}/entendido`;
      const csrf = doc.createElement('input');
      csrf.type = 'hidden'; csrf.name = 'csrf'; csrf.value = campo.dataset.notasCsrf;
      const boton = doc.createElement('button');
      boton.className = 'btn'; boton.textContent = 'Entendido';
      form.append(csrf, boton);
      aviso.append(titulo, fecha, texto, enlace, form);
      region.append(aviso);
    };
    return async function consultar() {
      if (doc.hidden || ocupado) return;
      ocupado = true;
      try {
        const respuesta = await obtener('/notas/nuevas', { credentials: 'same-origin', cache: 'no-store' });
        if (respuesta.ok) procesar(await respuesta.json(), almacenamiento, mostrar);
      } catch (_) { /* La siguiente consulta vuelve a intentar sin interrumpir el trabajo. */ }
      finally { ocupado = false; }
    };
  }
  if (typeof module !== 'undefined') module.exports = { procesar, montar };
  if (global.document) {
    let almacenamiento;
    try { almacenamiento = global.sessionStorage; } catch (_) { almacenamiento = null; }
    const consultar = montar(global.document, global.fetch.bind(global), almacenamiento);
    if (consultar) {
      consultar();
      global.setInterval(consultar, 15000);
      global.document.addEventListener('visibilitychange', consultar);
    }
  }
})(typeof window !== 'undefined' ? window : globalThis);
