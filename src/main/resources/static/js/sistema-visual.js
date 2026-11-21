/* RNF-002/007: presentación compartida; el servidor conserva todos los permisos. */
(function () {
  'use strict';
  const normalizar = texto => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es');
  const hoy = new Intl.DateTimeFormat('sv-SE', {timeZone: 'America/Bogota'}).format(new Date());
  function coincide(fila, filtro) {
    const estado = fila.dataset.estado;
    if (filtro === 'TODOS') return true;
    if (filtro === 'ACTIVA') return estado !== 'INACTIVA';
    if (filtro === 'ABIERTOS') return !['ARREGLADO', 'HECHA'].includes(estado);
    if (filtro === 'PROXIMAS') return ['EN_COLA','EN_CURSO'].includes(estado) && fila.dataset.fecha >= hoy;
    if (filtro === 'PASADAS') return estado !== 'CANCELADA' && fila.dataset.fecha < hoy;
    if (filtro === 'ESTE_MES') return (fila.dataset.fecha || '').slice(0,7) === hoy.slice(0,7);
    if (filtro === 'ROJO') return fila.dataset.color === 'ROJO';
    return filtro === estado;
  }
  document.querySelectorAll('[data-lista]').forEach(lista => {
    const filas = [...lista.querySelectorAll('[data-registro]')];
    const chips = [...lista.querySelectorAll('[data-lista-filtro]')];
    const buscar = lista.querySelector('[data-lista-buscar]');
    const parametros = new URLSearchParams(location.search);
    if (buscar && parametros.has('q')) buscar.value = parametros.get('q');
    const defecto = {RESERVAS:'PROXIMAS',VISITAS:'TODOS',PENDIENTES:'ABIERTO'};
    let filtro = parametros.get('filtro_lista') || parametros.get('estado') ||
      (lista.dataset.inicial !== 'TODOS' ? lista.dataset.inicial : '');
    if (!filtro && parametros.has('registros')) filtro = {ACTIVOS:'ACTIVO',INACTIVOS:'INACTIVO',TODOS:'TODOS'}[parametros.get('registros')];
    if (!filtro) filtro = defecto[lista.dataset.seccion] || chips[0]?.dataset.listaFiltro || 'TODOS';
    if (!chips.some(chip => chip.dataset.listaFiltro === filtro)) filtro = 'TODOS';
    let persona = '';
    const personas = [...lista.querySelectorAll('[data-lista-persona]')];
    const incluir = lista.querySelector('[data-incluir-inactivas]');
    const selectores = [...lista.querySelectorAll('[data-filtro-local]')];
    function actualizar() {
      const texto = normalizar(buscar?.value || '');
      const coincideTexto = fila => fila.dataset.filtroExtra !== 'false'
        && (!persona || (fila.dataset.evaluadores || fila.dataset.evaluador || '').split(/[ ,]+/).includes(persona))
        && (!incluir || incluir.checked || fila.dataset.empresaActiva !== 'false')
        && selectores.every(c => !c.value || (c.value === '__SIN__' ? !fila.dataset[c.dataset.filtroLocal]
          : (fila.dataset[c.dataset.filtroLocal] || '') === c.value))
        && normalizar(fila.dataset.busqueda || fila.textContent).includes(texto);
      let visibles = 0;
      filas.forEach(fila => {
        const responder = lista.dataset.seccion === 'RESERVAS' &&
          fila.closest?.('.respuestas-reservas') && filtro === 'PROXIMAS' && fila.dataset.estado === 'ESPERANDO';
        fila.hidden = !(coincideTexto(fila) && (coincide(fila, filtro) || responder));
        if (!fila.hidden) visibles++;
      });
      chips.forEach(chip => {
        chip.setAttribute('aria-pressed', String(chip.dataset.listaFiltro === filtro));
        const cantidad = filas.filter(fila => coincideTexto(fila) && coincide(fila, chip.dataset.listaFiltro)).length;
        const etiqueta = chip.dataset.etiqueta || chip.textContent.replace(/(?:\s*·)?\s*\d+(?:\s*·\s*\d+)?$/, '').trim();
        chip.dataset.etiqueta = etiqueta;
        chip.textContent = `${etiqueta} · ${cantidad}`;
      });
      lista.querySelectorAll('[data-lista-grupo]').forEach(grupo => {
        const cantidad = [...grupo.querySelectorAll('[data-registro]')].filter(fila => !fila.hidden).length;
        grupo.hidden = cantidad === 0;
        const contador = grupo.querySelector('.lista-grupo > span');
        if (contador) contador.textContent = cantidad;
      });
      const respuestas = lista.querySelector('[data-respuestas-contador]');
      if (respuestas) respuestas.textContent = filas.filter(f => !f.hidden && f.dataset.estado === 'ESPERANDO').length;
      const contador = lista.querySelector('[data-lista-contador]');
      if (contador) {
        const unidad = lista.dataset.unidad || 'resultado';
        const plural = unidad.endsWith('ón') ? unidad.replace(/ón$/, 'ones') : unidad + 's';
        contador.textContent = `${visibles} ${visibles === 1 ? unidad : plural}`;
      }
      const limpiar = lista.querySelector('[data-lista-limpiar]');
      if (limpiar) limpiar.hidden = !texto && !persona && !selectores.some(c => c.value)
        && filtro === (defecto[lista.dataset.seccion] || chips[0]?.dataset.listaFiltro || 'TODOS')
        && !location.search && !filas.some(f => f.dataset.filtroExtra === 'false');
      const vacia = lista.querySelector('[data-lista-vacia]');
      if (vacia) vacia.hidden = visibles !== 0;
    }
    chips.forEach(chip => chip.addEventListener('click', () => { filtro = chip.dataset.listaFiltro; actualizar(); }));
    buscar?.addEventListener('input', actualizar);
    personas.forEach(chip => chip.addEventListener('click', () => {
      persona = chip.dataset.listaPersona;
      personas.forEach(c => c.setAttribute('aria-pressed', String(c.dataset.listaPersona === persona)));
      actualizar();
    }));
    incluir?.addEventListener('change', actualizar);
    selectores.forEach(c => c.addEventListener('change', actualizar));
    lista.querySelectorAll('[data-abrir-historial]').forEach(b => b.addEventListener('click', () => {
      filtro = b.dataset.abrirHistorial; actualizar();
    }));
    lista.addEventListener('filtros-cambiados', actualizar);
    const avanzados = lista.querySelector('[data-filtros-vivos]');
    if (avanzados) avanzados.addEventListener('change', evento => {
      const campo = evento.target;
      if (!campo.name || campo.type === 'hidden' && !campo.value || !avanzados.checkValidity()) return;
      function oculto(nombre, valor) {
        let entrada = avanzados.elements.namedItem(nombre);
        if (!entrada) { entrada = document.createElement('input'); entrada.type = 'hidden';
          entrada.name = nombre; avanzados.append(entrada); }
        entrada.value = valor;
      }
      oculto('q', buscar?.value || '');
      oculto('filtro_lista', filtro);
      avanzados.requestSubmit();
    });
    lista.querySelector('[data-lista-orden]')?.addEventListener('change', e => {
      const ordenadas = [...filas].sort((a,b) => e.target.value === 'recientes'
        ? Number(b.dataset.registro) - Number(a.dataset.registro)
        : normalizar(a.dataset.busqueda || '').localeCompare(normalizar(b.dataset.busqueda || ''),'es'));
      ordenadas.forEach(f => f.parentElement.append(f)); actualizar();
    });
    actualizar();
  });
  document.querySelectorAll('[data-auto-filtros]').forEach(form => form.addEventListener('change', () => {
    if (form.checkValidity()) form.requestSubmit();
  }));
  document.querySelectorAll('tr[data-registro]').forEach(fila => fila.addEventListener('click', e => {
    if (e.target.closest('a,button,input,select,textarea,summary') || window.getSelection()?.toString()) return;
    fila.querySelector('a[href]')?.click();
  }));
  document.querySelectorAll('[data-toast]').forEach(toast => setTimeout(() => { toast.hidden = true; }, 4000));
  document.querySelectorAll('[data-alerta]').forEach(alerta => {
    const formulario = alerta.closest('form') || document.querySelector('main form');
    if (!formulario) return;
    const texto = normalizar(alerta.textContent);
    const nombres = texto.includes('cita') ? ['fecha', 'inicio', 'fin']
      : ['correo', 'nombre', 'telefono', 'nit', 'sector'].filter(nombre => texto.includes(nombre));
    const campos = nombres.map(nombre => formulario.elements.namedItem(nombre)).filter(Boolean);
    campos.forEach(campo => { campo.setAttribute('aria-invalid', 'true');
      campo.closest('.tiempo-colombia')?.classList.add('campo-invalido'); });
    if (campos[0]) {
      const grupo = campos[0].closest('.field') || campos[0].parentElement;
      (campos[0].closest('[data-fecha-hora]') || grupo).before(alerta);
      if (texto.includes('cita')) {
        const ver = document.createElement('button'); ver.type = 'button'; ver.className = 'link';
        ver.dataset.verHoras = ''; ver.textContent = 'Ver horas libres'; alerta.append(ver);
      }
    }
  });
})();
