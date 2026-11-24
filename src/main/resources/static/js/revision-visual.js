/* Paso 27A: presentación por partes; no detecta, mide, puntúa ni cambia valores enviados. */
(() => {
  'use strict';
  const form = document.querySelector('form[data-conversacion]');
  if (!form) return;
  const uno = selector => form.querySelector(selector);
  const todos = selector => [...form.querySelectorAll(selector)];
  const panel = uno('[data-medidas-compactas]');
  const pie = uno('.wizfoot');
  const seguir = uno('[data-siguiente]');
  const atras = uno('[data-atras-evaluacion]');
  atras.setAttribute('aria-label','Atrás');
  const aviso = uno('[data-falta]');
  let estado = {paso: 1, ocupada: false, indice: 0};
  let parte = 0;
  let ultimaPostura = -1, ultimaFoto = null;
  const hechas = new Set();
  const definiciones = [
    ['BRAZO', 'Brazo', 'rango_BRAZO', ['hombroElevado', 'brazoSeparado', 'brazoApoyado']],
    ['ANTEBRAZO', 'Antebrazo', 'rango_ANTEBRAZO', ['antebrazoCruza']],
    ['MUNECA', 'Muñeca', 'muneca_campo', []],
    ['CUELLO', 'Cuello', 'rango_CUELLO', ['cuelloGirado', 'cuelloInclinado']],
    ['TRONCO', 'Tronco', 'rango_TRONCO', ['troncoGirado', 'troncoInclinado']],
    ['PIERNAS', 'Piernas', null, ['pies_sin_apoyo']],
    ['ACTIVIDAD', 'Esfuerzo muscular', 'actividad_campo', []],
    ['CARGA', 'Carga', 'carga_campo', []],
    ['TIRONES', 'Golpes o tirones', 'tirones_campo', []]
  ];
  const navegacion = document.createElement('nav');
  navegacion.className = 'partes-cuerpo';
  navegacion.setAttribute('aria-label', 'Revisar partes del cuerpo');
  panel.prepend(navegacion);
  const grupos = definiciones.map(([clave, nombre, campo, chips], indice) => {
    const grupo = document.createElement('section');
    grupo.className = 'revision-parte stack';
    grupo.dataset.parte = clave;
    const titulo = document.createElement('h3');
    titulo.textContent = `${nombre} · ¿cómo está?`;
    grupo.append(titulo);
    const medida = uno(`[data-medida="${clave}"]`);
    const pregunta = campo ? uno(`[data-pregunta="${campo}"]`) : null;
    if (medida) { medida.dataset.parteVisible = ''; grupo.append(medida); }
    else if (clave === 'MUNECA') grupo.append(uno('.campo-muneca'));
    else if (pregunta) grupo.append(pregunta);
    const ajustes = document.createElement('div');
    ajustes.className = 'revision-ajustes';
    if (chips.length) {
      const texto = document.createElement('p');
      texto.className = 'hint';
      texto.className = 'ajustes-titulo';
      texto.textContent = clave === 'PIERNAS' ? 'Revisa cómo apoya los pies.' :
        `¿Algo más en ${clave === 'BRAZO' ? 'el brazo' : nombre.toLowerCase()}?`;
      ajustes.append(texto);
      if (['BRAZO','ANTEBRAZO'].includes(clave)) {
        const ayuda = document.createElement('small');ayuda.className = 'hint';
        ayuda.textContent = 'Solo el brazo que hace el esfuerzo';ajustes.append(ayuda);
      }
    }
    chips.forEach(chip => {
      const entrada = uno(`[data-chip="${chip}"]`);
      const etiqueta = entrada?.closest('label');
      if (etiqueta) {
        const cortos = {hombroElevado:'Hombro subido',brazoSeparado:'Brazo separado del cuerpo',
          brazoApoyado:'Brazo apoyado',antebrazoCruza:'Antebrazo cruza o sale hacia un lado',
          cuelloGirado:'Cabeza torcida',cuelloInclinado:'Cabeza inclinada a un lado',
          troncoGirado:'Espalda torcida',troncoInclinado:'Espalda inclinada a un lado',
          pies_sin_apoyo:'Pies sin buen apoyo'};
        etiqueta.querySelector('span').textContent = cortos[chip];ajustes.append(etiqueta);
      }
    });
    if (clave === 'PIERNAS') ajustes.append(uno('[data-ayuda-pies]'));
    grupo.append(ajustes);
    panel.append(grupo);
    if (indice < 6) {
      const boton = document.createElement('button');
      boton.type = 'button';
      boton.className = 'chip';
      boton.dataset.parteIr = String(indice);
      boton.textContent = nombre;
      boton.onclick = () => { if (!estado.ocupada) { parte = indice; dibujar(); } };
      navegacion.append(boton);
    }
    return {grupo, campo, nombre};
  });
  panel.querySelectorAll(':scope > h3,:scope > .campo-ajustes').forEach(elemento => { elemento.hidden = true; });
  // RF-017: reorganiza los controles existentes, conservando sus manejadores.
  const detectar = uno('[data-detectar-puntos]');
  const controlesFoto = detectar.parentElement;
  controlesFoto.classList.add('foto-controles-claros');
  const statusFoto = uno('[data-estado-deteccion]');controlesFoto.before(statusFoto);
  const deshacer = uno('[data-puntos-deshacer]');deshacer.className = 'link';
  const masFoto = document.createElement('details');masFoto.className = 'foto-mas';
  const resumenFoto = document.createElement('summary');resumenFoto.textContent = 'Más ⋯';
  const opcionesFoto = document.createElement('div');opcionesFoto.className = 'foto-mas-opciones';
  masFoto.append(resumenFoto,opcionesFoto);controlesFoto.append(masFoto);
  for (const selector of ['[data-puntos-borrar]','[data-cambiar-foto]','[data-quitar-foto]','[data-calcular-manual]']) {
    const boton = uno(selector);if (boton) {boton.className = 'link';opcionesFoto.append(boton);}
  }
  statusFoto.parentElement.querySelector(':scope > span.hint')?.setAttribute('hidden','');
  // RF-017, RNF-007: tarjeta visual; no transforma el valor utilizado en el cálculo.
  grupos.slice(0,6).forEach(({grupo,nombre},i) => {
    const medida = grupo.querySelector('[data-medida]');
    const pregunta = grupo.querySelector('[data-pregunta]');
    if (medida) {
      grupo.querySelector(':scope > h3').hidden = true;
      const cabecera = nodo('div','medida-resumen');
      const angulo = medida.querySelector('[data-angulo]');cabecera.append(angulo);
      const identidad = nodo('div','medida-identidad');
      identidad.append(nodo('span','medida-etiqueta',nombre.toUpperCase()),nodo('strong','medida-origen','Medido sobre la foto'));
      const sugerencia = nodo('span','medida-sugerencia');cabecera.append(identidad,sugerencia);medida.prepend(cabecera);
      const actualizar = () => {
        const elegida = pregunta?.querySelector('[data-sugerido]');
        sugerencia.hidden = !elegida;
        if (elegida) sugerencia.textContent = 'Sugerencia: ' + elegida.querySelector('span').textContent;
        const origen = identidad.querySelector('.medida-origen');
        origen.textContent = angulo.dataset.origenMedida === 'manual' ? 'Escrito a mano' : 'Medido sobre la foto';
        pregunta?.querySelectorAll('.campo-opcion').forEach(b => {
          let etiqueta = b.querySelector('.rango-sugerido');
          if (!etiqueta) {etiqueta=nodo('small','rango-sugerido','SUGERIDO');b.append(etiqueta);}
          etiqueta.hidden = b.dataset.sugerido !== 'true';
        });
      };
      new MutationObserver(actualizar).observe(angulo,{childList:true,attributes:true,subtree:true});
      if (pregunta) new MutationObserver(actualizar).observe(pregunta,{attributes:true,subtree:true,
        attributeFilter:['aria-pressed','data-sugerido']});
      actualizar();
    }
    if (pregunta) {
      pregunta.querySelector(':scope > p').hidden = true;
      const ayuda = nodo('div','rangos-titulo');
      ayuda.append(nodo('strong','','¿Cuál dibujo se parece más?'),nodo('small','hint',
        'Marcamos la sugerencia. Cámbiala si no coincide.'));pregunta.prepend(ayuda);
      const lista = pregunta.querySelector('.campo-lista');
      const fichas = [...lista.querySelectorAll('.campo-opcion')].sort((a,b)=>Number(a.dataset.valor)-Number(b.dataset.valor));
      fichas.forEach(b=>lista.append(b));lista.style.setProperty('--opciones',String(fichas.length));
    }
  });
  pie.prepend(atras);
  const contador = document.createElement('span');
  contador.className = 'hint revision-contador';
  contador.setAttribute('role', 'status');
  pie.insertBefore(contador, uno('[data-seguir-zona]'));
  const exportar = document.createElement('a');
  exportar.className = 'btn';
  exportar.textContent = 'Exportar PDF';
  exportar.hidden = true;
  pie.insertBefore(exportar, uno('[data-seguir-zona]'));
  const empresa = Number(form.dataset.enfoqueEmpresa);
  const visita = Number(form.dataset.enfoqueVisita);
  if (empresa) exportar.href = `/informes/empresa/${empresa}?visita=${visita}`;

  function activa() {
    return estado.paso === 3 && !estado.manual && !estado.errorFoto && !panel.hidden;
  }
  function respondida(indice) {
    const campo = grupos[indice].campo;
    return !campo || Boolean(form.elements[campo]?.value);
  }
  function errorActual() {
    const grupo = grupos[parte].grupo;
    grupo.classList.add('campo-falta');
    let error = grupo.querySelector('[data-error-opcion]');
    if (!error) {
      error = document.createElement('p');
      error.className = 'alerta';
      error.dataset.errorOpcion = '';
      error.setAttribute('role', 'alert');
      error.textContent = '⚠ Elige una opción para seguir.';
      grupo.append(error);
    }
    aviso.textContent = 'Falta 1 respuesta';
    grupo.querySelector('button,input')?.focus({preventScroll: true});
    grupo.scrollIntoView({block: 'nearest', behavior: 'instant'});
  }
  function dibujar() {
    const revision = activa();
    form.classList.toggle('revision-grupos', revision);
    navegacion.hidden = !revision;
    grupos.forEach(({grupo, campo}, indice) => {
      grupo.hidden = !revision || indice !== parte;
      if (campo && respondida(indice)) {
        grupo.classList.remove('campo-falta');
        grupo.querySelector('[data-error-opcion]')?.remove();
      }
      if (revision && indice === parte) grupo.querySelectorAll('.campo-lista').forEach(lista => { lista.hidden = false; });
    });
    navegacion.querySelectorAll('button').forEach((boton, indice) => {
      boton.setAttribute('aria-pressed', String(indice === parte));
      boton.classList.toggle('parte-hecha',hechas.has(indice));
      boton.disabled = estado.ocupada;
    });
    if (revision) {
      seguir.disabled = estado.ocupada;
      seguir.style.pointerEvents = '';
      seguir.textContent = estado.ocupada ? 'Cargando…' : parte < 5 ? `Siguiente: ${grupos[parte+1].nombre} →` : 'Continuar →';
      contador.textContent = parte < 6 ? `${grupos[parte].nombre} · Parte ${parte + 1} de 6` : `${grupos[parte].nombre} · Esfuerzo ${parte - 5} de 3`;
    } else contador.textContent = uno('#pasoActual').textContent;
    exportar.hidden = estado.paso !== 5 || !empresa;
    if (estado.paso === 5) seguir.textContent = estado.ocupada ? 'Guardando…' : 'Guardar y terminar';
    document.body.classList.toggle('viendo-resultado', estado.paso === 5);
    document.body.classList.toggle('en-resultado', estado.paso === 5);
    form.classList.toggle('resultado-final', estado.paso === 5);
  }
  form.addEventListener('alineo:presentacion', evento => {
    estado = evento.detail;
    if (ultimaPostura !== estado.indice || ultimaFoto !== estado.foto) { ultimaPostura = estado.indice; ultimaFoto = estado.foto; parte = 0; hechas.clear(); }
    dibujar();
  });
  const continuarOriginal = seguir.onclick;
  seguir.onclick = evento => {
    if (activa() && !estado.ocupada) {
      if (!respondida(parte)) { errorActual(); return; }
      hechas.add(parte);
      if (parte < grupos.length - 1) { parte++; dibujar(); return; }
    }
    return continuarOriginal.call(seguir, evento);
  };
  const atrasOriginal = atras.onclick;
  atras.onclick = evento => {
    if (activa() && parte > 0 && !estado.ocupada) { parte--; dibujar(); return; }
    return atrasOriginal.call(atras, evento);
  };
  function nodo(etiqueta, clase, texto) {
    const elemento = document.createElement(etiqueta);
    if (clase) elemento.className = clase;
    if (texto) elemento.textContent = texto;
    return elemento;
  }
  function anillo(puntaje, color, pequeno) {
    const aro = nodo('span', `resultado-anillo ${color}${pequeno ? ' pequeno' : ''}`);
    aro.append(nodo('b', '', String(puntaje)));
    return aro;
  }
  function resultado(posturas, critica) {
    const destino = uno('[data-resultados]');
    const documentos = posturas.map(postura => new DOMParser().parseFromString(postura.html || '', 'text/html'));
    const datos = documentos.map(doc => [...doc.querySelectorAll('[data-puntaje-final]')]
      .sort((a, b) => Number(b.dataset.puntajeFinal) - Number(a.dataset.puntajeFinal))[0]);
    const peor = datos.map((punto, indice) => ({punto, indice})).filter(dato => dato.punto)
      .sort((a, b) => Number(b.punto.dataset.puntajeFinal) - Number(a.punto.dataset.puntajeFinal))[0];
    if (!peor) return;
    destino.replaceChildren();
    destino.className = 'resultado-final';
    const principal = peor.punto.closest('[data-calculo]');
    const color = ['VERDE', 'AMARILLO', 'ROJO'].find(nombre => peor.punto.classList.contains(nombre)) || '';
    const cabecera = nodo('section', `resultado-principal riesgo-${color.toLowerCase()}`);
    cabecera.append(anillo(peor.punto.dataset.puntajeFinal, color));
    const texto = nodo('div', 'stack');
    const nivel = principal.querySelector('.pill');
    if (nivel) texto.append(nivel.cloneNode(true));
    texto.append(nodo('h2', '', principal.querySelector('.result h3')?.textContent || 'Resultado del puesto'));
    texto.append(nodo('p', 'hint', 'El puesto toma el puntaje de la postura más crítica.'));
    cabecera.append(texto);
    destino.append(cabecera);
    const tarjetas = nodo('div', 'resultado-posturas');
    posturas.forEach((postura, indice) => {
      const tarjeta = nodo('article', 'card');
      tarjeta.append(nodo('h3', '', postura.titulo));
      const punto = datos[indice];
      if (punto) {
        const clase = ['VERDE', 'AMARILLO', 'ROJO'].find(nombre => punto.classList.contains(nombre)) || '';
        tarjeta.append(anillo(punto.dataset.puntajeFinal, clase, true));
        const etiqueta = punto.closest('[data-calculo]')?.querySelector('.pill');
        if (etiqueta) tarjeta.append(etiqueta.cloneNode(true));
      }
      if (indice === critica.indice) tarjeta.append(nodo('small', 'postura-mas-critica', 'MÁS CRÍTICA'));
      const detalle = nodo('details', 'desglose-postura');
      detalle.append(nodo('summary', '', 'Ver desglose ›'));
      detalle.append(...documentos[indice].body.childNodes);
      tarjeta.append(detalle);
      tarjetas.append(tarjeta);
    });
    const sugerencias = [...new Set([...tarjetas.querySelectorAll('.sug')]
      .map(sugerencia => sugerencia.textContent.trim()))];
    destino.append(tarjetas);
    if (sugerencias.length) {
      const seccion = nodo('section', 'resultado-primeros');
      seccion.append(nodo('h3', '', 'Qué hacer primero'));
      const lista = nodo('ol', '');
      sugerencias.slice(0, 3).forEach(textoSugerencia => lista.append(nodo('li', '', textoSugerencia)));
      seccion.append(lista);
      if (sugerencias.length > 3) {
        const ver = nodo('button', 'link', `Ver las ${sugerencias.length} sugerencias`);
        ver.type = 'button';
        ver.onclick = () => tarjetas.querySelectorAll('details').forEach(detalle => { detalle.open = true; });
        seccion.append(ver);
      }
      destino.append(seccion);
    }
    dibujar();
  }
  window.AlineoRevision = {resultado};
  dibujar();
})();
