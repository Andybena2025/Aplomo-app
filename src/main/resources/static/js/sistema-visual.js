/* RF-024: reglas de presentación puras, compartidas con las pruebas sin navegador. */
(function (root) {
  const reglas = {
    riesgo(f) {
      if (!f.puntaje) return 'sin';
      return Number(f.nivel) >= 3 || f.color === 'ROJO' ? 'alto'
        : Number(f.nivel) === 2 || f.color === 'AMARILLO' ? 'medio' : 'bajo';
    },
    mes(filas, expandido) {
      const resumen = {total: filas.length, restantes: expandido ? 0 : Math.max(0, filas.length - 5),
        alto: 0, medio: 0, bajo: 0, sin: 0};
      filas.forEach(f => resumen[reglas.riesgo(f)]++);
      return resumen;
    },
    conteos(filas, hoy) {
      return {curso:filas.filter(f => f.estado === 'EN_CURSO').length,
        mes:filas.filter(f => (f.fecha || '').slice(0,7) === hoy.slice(0,7)).length,
        finalizadas:filas.filter(f => f.estado === 'FINALIZADA').length,
        alto:filas.filter(f => reglas.riesgo(f) === 'alto').length};
    },
    claves(f, modo) {
      if (modo === 'ROL') return [f.rol];
      if (modo === 'EVALUADOR') return f.evaluadores ? f.evaluadores.split(',').filter(Boolean) : ['SIN_ASIGNAR'];
      if (modo === 'CIUDAD') return f.ciudad ? f.ciudad.split(', ').filter(Boolean) : ['Sin ciudad registrada'];
      if (modo === 'EMPRESA') return f.rol === 'ADMINISTRADOR' ? ['TODAS']
        : f.empresa ? f.empresa.split(' ').filter(Boolean) : ['SIN_EMPRESA'];
      return [];
    }
  };
  root.AlineoListas39 = reglas;
  if (typeof module !== 'undefined' && module.exports) module.exports = reglas;
})(typeof globalThis !== 'undefined' ? globalThis : this);
/* RNF-002/007: presentación compartida; el servidor conserva todos los permisos. */
(function () {
  'use strict';
  if (typeof document === 'undefined') return;
  const normalizar = texto => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es');
  const hoy = new Intl.DateTimeFormat('sv-SE', {timeZone: 'America/Bogota'}).format(new Date());
  const fin = new Date(hoy + 'T12:00:00Z'); fin.setUTCDate(fin.getUTCDate()+14);
  const finProximas = fin.toISOString().slice(0,10);
  const mes = new Date(hoy.slice(0,7) + '-01T12:00:00Z'); mes.setUTCMonth(mes.getUTCMonth()-1);
  const anterior = mes.toISOString().slice(0,7);
  function coincide(fila, filtro) {
    const estado = fila.dataset.estado;
    if (filtro === 'TODOS') return true;
    if (filtro === 'ACTIVA') return estado !== 'INACTIVA';
    if (filtro === 'ABIERTOS') return !['ARREGLADO', 'HECHA'].includes(estado);
    if (filtro === 'PROXIMAS') return ['EN_COLA','EN_CURSO'].includes(estado) && fila.dataset.fecha >= hoy && fila.dataset.fecha.slice(0,10) <= finProximas;
    if (filtro === 'PASADAS') return estado !== 'CANCELADA' && fila.dataset.fecha < hoy;
    if (filtro === 'MES_ANTERIOR') return (fila.dataset.fecha || '').slice(0,7) === anterior;
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
    const estadoEmpresa = lista.querySelector('[data-incluir-estado]');
    const mas = lista.querySelector('[data-lista-mas]');
    let limite = 10, cursoCompleto = false;
    const meses = [...lista.querySelectorAll('[data-mes-visitas]')];
    const mesesAbiertos = new Set();
    const opcionesVista = [...lista.querySelectorAll('[data-ver-por]')];
    const tablaBase = lista.querySelector('[data-tabla-base]');
    const gruposVista = lista.querySelector('[data-ver-por-grupos]');
    const claveVista = 'alineo:ver-por:' + lista.dataset.seccion;
    let vista = lista.querySelector('[data-ver-por-defecto]')?.dataset.verPorDefecto || '';
    try { const guardada = localStorage.getItem(claveVista);
      if (opcionesVista.some(b => b.dataset.verPor === guardada)) vista = guardada;
    } catch (_) { /* RNF-007: la lista funciona aunque se bloquee el almacenamiento. */ }
    function ordenarVista() {
      if (!opcionesVista.length) return;
      const nombre = fila => fila.querySelector('.fila-enlace strong')?.textContent || fila.dataset.busqueda || '';
      filas.sort((a,b) => vista === 'PROXIMA'
        ? (a.dataset.proxima || '9999').localeCompare(b.dataset.proxima || '9999')
          || nombre(a).localeCompare(nombre(b), 'es')
        : vista === 'ROL' ? ['ADMINISTRADOR','EVALUADOR','USUARIO'].indexOf(a.dataset.rol)
          - ['ADMINISTRADOR','EVALUADOR','USUARIO'].indexOf(b.dataset.rol)
          || nombre(a).localeCompare(nombre(b), 'es') : nombre(a).localeCompare(nombre(b), 'es'));
    }
    function agrupar() {
      if (!tablaBase || !gruposVista || !opcionesVista.length) return;
      const cuerpo = tablaBase.querySelector('tbody');
      filas.forEach(f => cuerpo.append(f));
      gruposVista.replaceChildren();
      opcionesVista.forEach(b => b.setAttribute('aria-pressed', String(b.dataset.verPor === vista)));
      const agrupada = !['AZ','PROXIMA'].includes(vista);
      tablaBase.hidden = agrupada;
      gruposVista.hidden = !agrupada;
      if (!agrupada) return;
      const personas = new Map([...lista.querySelectorAll('[data-persona-id]')]
        .map(p => [p.dataset.personaId, p.dataset]));
      const empresas = new Map([...lista.querySelectorAll('[data-empresa-usuarios] option')]
        .map(o => [o.value, o.textContent]));
      const grupos = new Map(), usados = new Set();
      filas.forEach(f => AlineoListas39.claves(f.dataset, vista).forEach(clave => {
        if (!grupos.has(clave)) grupos.set(clave, []); grupos.get(clave).push(f);
      }));
      const roles = {ADMINISTRADOR:'Administrador', EVALUADOR:'Evaluador', USUARIO:'Usuario'};
      const titulo = clave => vista === 'ROL' ? roles[clave]
        : vista === 'EVALUADOR' ? personas.get(clave)?.nombre || 'Sin evaluador'
        : vista === 'EMPRESA' ? clave === 'TODAS' ? 'Todas las empresas'
          : empresas.get(clave) || 'Sin empresas asignadas' : clave;
      const orden = [...grupos.keys()].sort((a,b) => vista === 'ROL'
        ? Object.keys(roles).indexOf(a) - Object.keys(roles).indexOf(b)
        : a === 'SIN_ASIGNAR' ? 1 : b === 'SIN_ASIGNAR' ? -1 : titulo(a).localeCompare(titulo(b),'es'));
      orden.forEach(clave => {
        const registros = grupos.get(clave);
        if (!registros.some(f => !f.hidden)) return;
        const tarjeta = document.createElement('section'); tarjeta.className = 'grupo-lectura';
        tarjeta.dataset.grupoVisual = clave;
        const cabecera = document.createElement('header');cabecera.className = 'grupo-lectura-cabecera';
        if (vista === 'EVALUADOR' && personas.has(clave)) {
          const avatar = document.createElement('span');avatar.className = 'grupo-iniciales';
          avatar.style.setProperty('--persona', personas.get(clave).color);
          avatar.textContent = titulo(clave).replace(/^(Ing\.|Rep\.)\s*/i,'').split(/\s+/).slice(0,2)
            .map(p => p[0]).join('').toUpperCase();cabecera.append(avatar);
        }
        const h = document.createElement('h2');h.textContent = titulo(clave);
        if (vista === 'ROL') h.className = 'rol-pill rol-pill-' + clave.toLowerCase();
        cabecera.append(h);
        const cantidad = registros.filter(f => f.dataset.cumple === 'true').length;
        const n = document.createElement('span');const unidad = lista.dataset.seccion === 'EMPRESAS' ? 'empresa' : 'cuenta';
        n.textContent = cantidad + ' ' + unidad + (cantidad === 1 ? '' : 's');cabecera.append(n);
        const scroll = document.createElement('div');scroll.className = 'table-scroll';
        const tabla = document.createElement('table');tabla.className = tablaBase.querySelector('table').className;
        tabla.append(tablaBase.querySelector('thead').cloneNode(true));
        const tbody = document.createElement('tbody');
        registros.forEach(f => {
          const fila = usados.has(f) ? f.cloneNode(true) : f;
          if (fila !== f) fila.dataset.copiaGrupo = 'true';
          usados.add(f);tbody.append(fila);
        });
        tabla.append(tbody);scroll.append(tabla);tarjeta.append(cabecera,scroll);gruposVista.append(tarjeta);
      });
    }
    gruposVista?.addEventListener('click', e => {
      if (e.target.closest('a,button,input,select')) return;
      e.target.closest('tr[data-copia-grupo]')?.querySelector('a[href]')?.click();
    });
    opcionesVista.forEach(b => b.addEventListener('click', () => {
      vista = b.dataset.verPor;try {localStorage.setItem(claveVista,vista);} catch (_) {}
      actualizar();
    }));
    meses.forEach(mes => mes.querySelector('[data-ver-mes]')?.addEventListener('click', () => {
      mesesAbiertos.add(mes.dataset.mesVisitas);actualizar();
    }));
    lista.querySelector('[data-ver-curso]')?.addEventListener('click', e => {
      cursoCompleto = true;e.currentTarget.hidden = true;actualizar();
    });
    const gruposChips = [...lista.querySelectorAll(':scope > nav.chips, :scope > div.chips, :scope > .leyenda-personas')];
    if (!lista.querySelector('.fila-chips') && gruposChips.length) {
      const filaChips = document.createElement('div'); filaChips.className = 'fila-chips';
      lista.querySelector('.lista-herramientas')?.after(filaChips);
      gruposChips.forEach(g => filaChips.append(g));
    }
    const selectores = [...lista.querySelectorAll('[data-filtro-local]')];
    function actualizar() {
      const texto = normalizar(buscar?.value || '');
      const coincideTexto = fila => fila.dataset.filtroExtra !== 'false'
        && (!persona || (fila.dataset.evaluadores || fila.dataset.evaluador || '').split(/[ ,]+/).includes(persona))
        && (!incluir || incluir.checked || fila.dataset.empresaActiva !== 'false')
        && (!estadoEmpresa || estadoEmpresa.value === 'TODOS' ||
          (estadoEmpresa.value === 'INACTIVAS' ? fila.dataset.empresaActiva === 'false' : fila.dataset.empresaActiva !== 'false'))
        && selectores.every(c => !c.value || (c.value === '__SIN__' ? !fila.dataset[c.dataset.filtroLocal]
          : (fila.dataset[c.dataset.filtroLocal] || '') === c.value))
        && normalizar((fila.dataset.busqueda || '') + ' ' + fila.textContent).includes(texto);
      ordenarVista();
      const enCurso = [...lista.querySelectorAll('[data-visita-curso]')];
      let visibles = 0, total = 0;
      filas.forEach(fila => {
        const responder = lista.dataset.seccion === 'RESERVAS' &&
          fila.closest?.('.respuestas-reservas') && filtro === 'PROXIMAS' && fila.dataset.estado === 'ESPERANDO';
        const cumple = coincideTexto(fila) && (coincide(fila, filtro) || responder);
        if (cumple) total++;
        const destacada = fila.dataset.destacada === 'true';
        fila.dataset.cumple = String(cumple);
        fila.hidden = destacada || !cumple || (!meses.length && visibles >= limite);
        if (cumple && !destacada) visibles++;
      });
      chips.forEach(chip => {
        chip.setAttribute('aria-pressed', String(chip.dataset.listaFiltro === filtro));
        const cantidad = [...filas,...enCurso].filter(fila =>
          coincideTexto(fila) && coincide(fila, chip.dataset.listaFiltro)).length;
        const etiqueta = chip.dataset.etiqueta || chip.textContent.replace(/(?:\s*·)?\s*\d+(?:\s*·\s*\d+)?$/, '').trim();
        chip.dataset.etiqueta = etiqueta;
        chip.textContent = `${etiqueta} · ${cantidad}`;
      });
      lista.querySelectorAll('[data-lista-grupo]').forEach(grupo => {
        const cantidad = [...grupo.querySelectorAll('[data-registro]')].filter(fila => !fila.hidden).length;
        grupo.hidden = cantidad === 0;
        const contador = grupo.querySelector('.lista-grupo > span');
        if (contador) contador.textContent = cantidad;
        const dia = grupo.querySelector('[data-dia-contador]');
        if (dia) dia.textContent = cantidad + (cantidad === 1 ? ' reserva' : ' reservas');
      });
      meses.forEach(mes => {
        const registros = [...mes.querySelectorAll('[data-registro]')];
        const cumplen = registros.filter(f => f.dataset.cumple === 'true');
        const expandido = mesesAbiertos.has(mes.dataset.mesVisitas);
        const resumen = AlineoListas39.mes(cumplen.map(f => f.dataset), expandido);
        mes.hidden = !resumen.total;
        registros.forEach(f => { f.hidden = !cumplen.includes(f) || (!expandido && cumplen.indexOf(f) >= 5); });
        mes.querySelector('[data-mes-contador]').textContent = resumen.total +
          (resumen.total === 1 ? ' visita ·' : ' visitas ·');
        mes.querySelectorAll('[data-mes-riesgo]').forEach(e => {
          e.querySelector('span').textContent = resumen[e.dataset.mesRiesgo];
        });
        const sin = mes.querySelector('[data-mes-sin]');sin.hidden = !resumen.sin;
        sin.textContent = resumen.sin + ' sin resultado';
        const masMes = mes.querySelector('[data-ver-mes]');masMes.hidden = !resumen.restantes;
        masMes.textContent = resumen.restantes === 1 ? 'Ver la visita restante de ' + mes.dataset.nombreMes
          : 'Ver las ' + resumen.restantes + ' visitas restantes de ' + mes.dataset.nombreMes;
      });
      if (meses.length && !meses.some(m => !m.hidden && m.open)) {
        const reciente = meses.find(m => !m.hidden);if (reciente) reciente.open = true;
      }
      if (lista.dataset.seccion === 'VISITAS') {
        const curso = enCurso.filter(f => coincideTexto(f) && coincide(f,filtro));
        total += curso.length;visibles += curso.length;
        enCurso.forEach(f => {
          f.hidden = !curso.includes(f) || (!cursoCompleto && curso.indexOf(f) >= 3);
        });
        const elegidas = filas.filter(f => f.dataset.cumple === 'true');
        const todas = [...elegidas,...curso];
        const valores = AlineoListas39.conteos(todas.map(f => f.dataset),hoy);
        lista.querySelectorAll('[data-visitas-conteo]').forEach(e => e.textContent = valores[e.dataset.visitasConteo]);
      }
      agrupar();
      const respuestas = lista.querySelector('[data-respuestas-contador]');
      if (respuestas) respuestas.textContent = filas.filter(f => !f.hidden && f.dataset.estado === 'ESPERANDO').length;
      const contador = lista.querySelector('[data-lista-contador]');
      if (contador) {
        const unidad = lista.dataset.unidad || 'resultado';
        const plural = unidad.endsWith('ón') ? unidad.replace(/ón$/, 'ones') : unidad + 's';
        contador.textContent = `${total} ${total === 1 ? unidad : plural}`;
      }
      if (mas) { mas.hidden = meses.length > 0 || visibles <= limite; mas.textContent = `Ver 10 más · quedan ${Math.max(0,visibles-limite)}`; }
      const tituloVisitas = lista.querySelector('[data-visitas-titulo]');
      if (tituloVisitas) {
        const periodo = filtro === 'MES_ANTERIOR' ? anterior : hoy.slice(0,7);
        const nombreMes = new Intl.DateTimeFormat('es-CO',{month:'long',year:'numeric',timeZone:'America/Bogota'})
          .format(new Date(periodo+'-02T12:00:00Z'));
        tituloVisitas.textContent = `Finalizadas · ${['TODOS','ROJO'].includes(filtro) ? 'Todas' : nombreMes}`;
        const total = lista.querySelector('[data-visitas-total]');
        if (total) total.textContent = `${visibles} ${visibles === 1 ? 'visita' : 'visitas'}${filtro === 'ESTE_MES' ? ' este mes' : filtro === 'MES_ANTERIOR' ? ' el mes anterior' : ''}`;
      }
      const limpiar = lista.querySelector('[data-lista-limpiar]');
      if (limpiar) limpiar.hidden = !texto && !persona && !selectores.some(c => c.value)
        && filtro === (defecto[lista.dataset.seccion] || chips[0]?.dataset.listaFiltro || 'TODOS')
        && !estadoEmpresa?.value && !location.search && !filas.some(f => f.dataset.filtroExtra === 'false');
      const vacia = lista.querySelector('[data-lista-vacia]');
      if (vacia) vacia.hidden = visibles !== 0;
    }
    function reiniciar() { limite = 10; actualizar(); }
    mas?.addEventListener('click', () => { limite += 10; actualizar(); });
    chips.forEach(chip => chip.addEventListener('click', () => { filtro = chip.dataset.listaFiltro; reiniciar(); }));
    buscar?.addEventListener('input', reiniciar);
    personas.forEach(chip => chip.addEventListener('click', () => {
      persona = chip.dataset.listaPersona;
      personas.forEach(c => c.setAttribute('aria-pressed', String(c.dataset.listaPersona === persona)));
      reiniciar();
    }));
    incluir?.addEventListener('change', reiniciar);
    estadoEmpresa?.addEventListener('change', reiniciar);
    selectores.forEach(c => c.addEventListener('change', reiniciar));
    lista.querySelectorAll('[data-abrir-historial]').forEach(b => b.addEventListener('click', () => {
      filtro = b.dataset.abrirHistorial; reiniciar();
    }));
    lista.addEventListener('filtros-cambiados', reiniciar);
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
      const ordenadas = filas.sort((a,b) => e.target.value === 'recientes'
        ? Number(b.dataset.registro) - Number(a.dataset.registro)
        : normalizar(a.dataset.busqueda || '').localeCompare(normalizar(b.dataset.busqueda || ''),'es'));
      ordenadas.forEach(f => f.parentElement.append(f)); reiniciar();
    });
    actualizar();
  });
  document.querySelectorAll('.fila-enlace small,.persona-lista,.alineo-table td[data-label="Empresa"]').forEach(e => {
    if (!e.title) e.title = e.textContent.trim();
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
