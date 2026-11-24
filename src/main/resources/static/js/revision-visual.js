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
  let marcarPies = () => {}, copiarNota = () => {};
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
    if (pregunta && !medida) { const repetida = pregunta.querySelector(':scope > p'); if (repetida) repetida.hidden = true; }
    if (clave === 'PIERNAS') {
      ajustes.querySelector('.ajustes-titulo').textContent = '¿Cómo apoya los pies?';
      piernas(ajustes);
      ajustes.append(uno('[data-ayuda-pies]'));
    }
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
  // Andy 24-nov: todas las herramientas siempre a la vista, en dos grupos (Puntos y Foto), sin menú "Más".
  detectar.className = 'btn p';
  const grupoHerramientas = (titulo, selectores) => {
    const fila = nodo('div', 'foto-herramientas');
    fila.append(nodo('span', 'foto-herramientas-titulo', titulo));
    for (const selector of selectores) {
      const boton = uno(selector);
      if (boton) { if (boton !== detectar) boton.className = 'btn'; fila.append(boton); }
    }
    return fila;
  };
  const puntosFoto = grupoHerramientas('Puntos', ['[data-detectar-puntos]', '[data-calcular-manual]',
    '[data-puntos-deshacer]', '[data-puntos-borrar]']);
  const archivoFoto = grupoHerramientas('Foto', ['[data-cambiar-foto]', '[data-quitar-foto]']);
  const apoyo = form.querySelector('details.campo-apoyo');
  if (apoyo) {
    const otra = nodo('button', 'btn', '+ Agregar otra foto');
    otra.type = 'button';
    otra.dataset.abrirApoyo = '';
    otra.onclick = () => { apoyo.open = true; apoyo.scrollIntoView({block: 'start', behavior: 'smooth'}); };
    archivoFoto.append(otra);
  }
  controlesFoto.prepend(puntosFoto, archivoFoto);
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
    marcarPies();
    copiarNota();
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
    if (estado.paso === 5) {
      seguir.textContent = estado.ocupada ? 'Guardando…' : 'Guardar y terminar';
      const agregar = uno('[data-agregar-postura]');
      if (agregar) pie.insertBefore(agregar,exportar);
      pie.insertBefore(exportar,uno('[data-seguir-zona]'));
    }
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
  // RULA piernas (RNF-007): dos tarjetas que marcan o desmarcan la casilla original pies_sin_apoyo; no cambia el cálculo.
  function piernas(ajustes) {
    const casilla = ajustes.querySelector('[data-chip="pies_sin_apoyo"]') || uno('[data-chip="pies_sin_apoyo"]');
    if (!casilla) return;
    casilla.closest('label').hidden = true;
    const lista = nodo('div', 'campo-lista pies-opciones');
    const tarjeta = (texto, ayuda, sinApoyo) => {
      const boton = nodo('button', 'campo-opcion');
      boton.type = 'button';
      boton.dataset.piesApoyo = sinApoyo ? 'no' : 'si';
      boton.append(nodo('span', '', texto), nodo('small', 'hint', ayuda));
      boton.onclick = () => {
        if (casilla.checked !== sinApoyo) {
          casilla.checked = sinApoyo;
          casilla.dispatchEvent(new Event('input', {bubbles: true}));
          casilla.dispatchEvent(new Event('change', {bubbles: true}));
        }
        marcarPies();
      };
      return boton;
    };
    const con = tarjeta('Pies con buen apoyo', 'Los dos pies en el piso o en un apoyapiés, en equilibrio.', false);
    const sin = tarjeta('Pies sin buen apoyo', 'Pies colgando, de puntillas o con el peso en un solo pie.', true);
    lista.append(con, sin);
    marcarPies = () => {
      con.setAttribute('aria-pressed', String(!casilla.checked));
      sin.setAttribute('aria-pressed', String(casilla.checked));
    };
    casilla.addEventListener('change', marcarPies);
    const nota = nodo('label', 'pies-nota');
    nota.append(nodo('span', '', 'Otra observación (opcional)'));
    const texto = nodo('textarea', 'input');
    texto.rows = 2;
    texto.maxLength = 255;
    texto.placeholder = 'Por ejemplo: usa un apoyapiés improvisado.';
    const original = form.elements.observaciones;
    if (original) {
      texto.value = original.value;
      texto.oninput = () => { original.value = texto.value; original.dispatchEvent(new Event('input', {bubbles: true})); };
      copiarNota = () => { if (document.activeElement !== texto) texto.value = original.value; };
    }
    nota.append(texto, nodo('small', 'hint', 'Se guarda con las observaciones del puesto.'));
    ajustes.append(lista, nota);
    marcarPies();
  }
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
  // RF-020/023: el resultado explicado usa únicamente puntajes y proyección recibidos del servidor.
  function resultado(posturas, critica) {
    const destino = uno('[data-resultados]');
    destino.innerHTML = window.AlineoResultado40.render(posturas.map(p => ({
      titulo:p.titulo,analisis:p.explicaciones || []
    })));
    dibujar();
  }
  window.AlineoRevision = {resultado};
  dibujar();
})();
