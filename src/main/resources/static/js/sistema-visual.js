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
    if (filtro === 'PROXIMAS') return !['CANCELADA', 'FINALIZADA'].includes(estado) && fila.dataset.fecha >= hoy;
    return filtro === estado;
  }
  document.querySelectorAll('[data-lista]').forEach(lista => {
    const filas = [...lista.querySelectorAll('[data-registro]')];
    const chips = [...lista.querySelectorAll('[data-lista-filtro]')];
    const buscar = lista.querySelector('[data-lista-buscar]');
    const parametros = new URLSearchParams(location.search);
    if (buscar && parametros.has('q')) buscar.value = parametros.get('q');
    const defecto = {RESERVAS: 'PROXIMAS', VISITAS: 'EN_CURSO', PENDIENTES: 'ABIERTOS'};
    let filtro = parametros.get('filtro_lista') || lista.dataset.inicial || parametros.get('estado');
    if (!filtro && parametros.has('registros')) filtro = {ACTIVOS:'ACTIVO',INACTIVOS:'INACTIVO',TODOS:'TODOS'}[parametros.get('registros')];
    if (!filtro) filtro = defecto[lista.dataset.seccion] || chips[0]?.dataset.listaFiltro || 'TODOS';
    if (!chips.some(chip => chip.dataset.listaFiltro === filtro)) filtro = 'TODOS';
    function actualizar() {
      const texto = normalizar(buscar?.value || '');
      const coincideTexto = fila => fila.dataset.filtroExtra !== 'false' && normalizar(fila.dataset.busqueda || fila.textContent).includes(texto);
      let visibles = 0;
      filas.forEach(fila => {
        fila.hidden = !(coincideTexto(fila) && coincide(fila, filtro));
        if (!fila.hidden) visibles++;
      });
      chips.forEach(chip => {
        chip.setAttribute('aria-pressed', String(chip.dataset.listaFiltro === filtro));
        const cantidad = filas.filter(fila => coincideTexto(fila) && coincide(fila, chip.dataset.listaFiltro)).length;
        const etiqueta = chip.textContent.split(' · ')[0];
        chip.textContent = `${etiqueta} · ${cantidad}`;
      });
      lista.querySelectorAll('[data-lista-grupo]').forEach(grupo => {
        const cantidad = [...grupo.querySelectorAll('[data-registro]')].filter(fila => !fila.hidden).length;
        grupo.hidden = cantidad === 0;
        const contador = grupo.querySelector('.lista-grupo > span');
        if (contador) contador.textContent = cantidad;
      });
      const contador = lista.querySelector('[data-lista-contador]');
      if (contador) contador.textContent = `${visibles} ${visibles === 1 ? 'resultado' : 'resultados'}`;
      const vacia = lista.querySelector('[data-lista-vacia]');
      if (vacia) vacia.hidden = visibles !== 0;
    }
    chips.forEach(chip => chip.addEventListener('click', () => { filtro = chip.dataset.listaFiltro; actualizar(); }));
    buscar?.addEventListener('input', actualizar);
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
    actualizar();
  });
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
