/* RNF-002/004/007: fecha colombiana visible; el contrato del servidor conserva ISO. */
(function (global) {
  'use strict';
  const dos = valor => String(valor).padStart(2, '0');
  function fechaISO(dia, mes, anio) {
    if (!dia || !mes || String(anio).length !== 4) return '';
    const fecha = new Date(Date.UTC(Number(anio), Number(mes) - 1, Number(dia)));
    if (fecha.getUTCFullYear() !== Number(anio) || fecha.getUTCMonth() + 1 !== Number(mes)
        || fecha.getUTCDate() !== Number(dia)) return '';
    return `${anio}-${dos(mes)}-${dos(dia)}`;
  }
  function horaISO(hora, minuto) {
    if (!/^\d{1,2}$/.test(String(hora)) || !/^\d{1,2}$/.test(String(minuto))
        || Number(hora) > 23 || Number(minuto) > 59) return '';
    return `${dos(hora)}:${dos(minuto)}`;
  }
  function salta(valor, tipo) {
    if (tipo === 'anio') return false;
    if (valor.length === 2) return true;
    const numero = Number(valor);
    return tipo === 'dia' ? numero >= 4 : tipo === 'mes' ? numero >= 2
      : tipo === 'hora' ? numero >= 3 : numero >= 6;
  }
  function convertir(original) {
    if (original.dataset.tiempoListo) return;
    const fecha = original.type === 'date';
    original.dataset.tiempoListo = 'si';
    const tipos = fecha ? ['dia', 'mes', 'anio'] : ['hora', 'minuto'];
    const etiquetas = {dia: 'Día', mes: 'Mes', anio: 'Año', hora: 'Hora', minuto: 'Minutos'};
    const limite = {dia: 31, mes: 12, anio: 9999, hora: 23, minuto: 59};
    const grupo = document.createElement('div');
    grupo.className = 'tiempo-colombia';
    grupo.setAttribute('role', 'group');
    grupo.setAttribute('aria-label', original.closest('.field')?.querySelector('label')?.textContent || 'Fecha y hora');
    original.before(grupo);
    grupo.append(original);
    const partesIniciales = fecha ? original.value.split('-').reverse() : original.value.split(':');
    const requerido = original.required;
    original.type = 'hidden';
    original.required = false;
    const partes = tipos.map((tipo, indice) => {
      const campo = document.createElement('input');
      campo.type = 'text';
      campo.inputMode = 'numeric';
      campo.maxLength = tipo === 'anio' ? 4 : 2;
      campo.placeholder = {dia: 'DD', mes: 'MM', anio: 'AAAA', hora: 'HH', minuto: 'MM'}[tipo];
      campo.setAttribute('aria-label', etiquetas[tipo]);
      campo.required = requerido;
      campo.autocomplete = 'off';
      campo.dataset.parte = tipo;
      campo.value = partesIniciales[indice] || '';
      if (indice) grupo.append(document.createTextNode(fecha ? ' / ' : ' : '));
      grupo.append(campo);
      return campo;
    });
    const etiqueta = document.querySelector(`label[for="${original.id}"]`);
    partes[0].id = `${original.id || original.name}-visible`;
    if (etiqueta) etiqueta.htmlFor = partes[0].id;
    function actualizar() {
      const valores = partes.map(campo => campo.value);
      const anterior = original.value;
      original.value = fecha ? fechaISO(...valores) : horaISO(...valores);
      const incompleto = valores.some(Boolean) && !original.value;
      partes[0].setCustomValidity(incompleto ? (fecha ? 'Revisa la fecha.' : 'Revisa la hora.') : '');
      if (original.value === anterior) return;
      original.dispatchEvent(new Event('input', {bubbles: true}));
      original.dispatchEvent(new Event('change', {bubbles: true}));
    }
    original.addEventListener('aplomo:valor', () => {
      const valores = fecha ? original.value.split('-').reverse() : original.value.split(':');
      partes.forEach((campo, indice) => { campo.value = valores[indice] || ''; });
      actualizar();
    });
    partes.forEach((campo, indice) => {
      campo.addEventListener('input', () => {
        campo.value = campo.value.replace(/\D/g, '').slice(0, campo.maxLength);
        if (salta(campo.value, tipos[indice]) && partes[indice + 1]) {
          campo.value = dos(campo.value);
          partes[indice + 1].focus();
          partes[indice + 1].select();
        }
        actualizar();
      });
      campo.addEventListener('blur', () => {
        if (campo.value && tipos[indice] !== 'anio') campo.value = dos(campo.value);
        actualizar();
      });
      campo.addEventListener('keydown', evento => {
        if (evento.key === 'Backspace' && !campo.value && partes[indice - 1]) partes[indice - 1].focus();
        if (evento.key === 'ArrowUp' || evento.key === 'ArrowDown') {
          evento.preventDefault();
          const minimo = ['hora', 'minuto'].includes(tipos[indice]) ? 0 : tipos[indice] === 'anio' ? 1900 : 1;
          const actual = Number(campo.value || minimo);
          const valor = Math.max(minimo, Math.min(limite[tipos[indice]], actual + (evento.key === 'ArrowUp' ? 1 : -1)));
          campo.value = tipos[indice] === 'anio' ? String(valor) : dos(valor);
          actualizar();
        }
      });
      campo.addEventListener('paste', evento => {
        const texto = evento.clipboardData.getData('text').trim();
        const valores = texto.split(fecha ? '/' : ':');
        if (valores.length !== partes.length) return;
        evento.preventDefault();
        valores.forEach((valor, posicion) => { partes[posicion].value = valor.replace(/\D/g, ''); });
        actualizar();
      });
    });
    if (fecha) calendario(grupo, partes, actualizar);
  }
  function calendario(grupo, partes, actualizar) {
    const abrir = document.createElement('button');
    abrir.type = 'button';
    abrir.textContent = '📅';
    abrir.setAttribute('aria-label', 'Abrir calendario');
    abrir.setAttribute('aria-expanded', 'false');
    grupo.append(abrir);
    const panel = document.createElement('div');
    panel.className = 'calendario-campo';
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', 'Elegir fecha');
    panel.hidden = true;
    grupo.append(panel);
    let mes;
    function cerrar() { panel.hidden = true; abrir.setAttribute('aria-expanded', 'false'); abrir.focus(); }
    function dibujar() {
      panel.replaceChildren();
      const nav = document.createElement('nav');
      const titulo = document.createElement('strong');
      titulo.textContent = mes.toLocaleDateString('es-CO', {month: 'long', year: 'numeric'});
      function mover(etiqueta, cantidad) {
        const boton = document.createElement('button');
        boton.type = 'button'; boton.textContent = etiqueta;
        boton.setAttribute('aria-label', cantidad < 0 ? 'Mes anterior' : 'Mes siguiente');
        boton.onclick = () => { mes.setMonth(mes.getMonth() + cantidad); dibujar(); };
        return boton;
      }
      nav.append(mover('‹', -1), titulo, mover('›', 1));
      const dias = document.createElement('div');
      dias.setAttribute('role', 'grid');
      const inicio = new Date(mes.getFullYear(), mes.getMonth(), 1);
      inicio.setDate(inicio.getDate() - (inicio.getDay() + 6) % 7);
      for (let indice = 0; indice < 42; indice++) {
        const dia = new Date(inicio); dia.setDate(inicio.getDate() + indice);
        const boton = document.createElement('button');
        boton.type = 'button'; boton.textContent = dia.getDate();
        boton.setAttribute('aria-label', `${dos(dia.getDate())}/${dos(dia.getMonth() + 1)}/${dia.getFullYear()}`);
        boton.onclick = () => {
          [dia.getDate(), dia.getMonth() + 1, dia.getFullYear()].forEach((valor, indice) => {
            partes[indice].value = indice === 2 ? String(valor) : dos(valor);
          });
          actualizar(); cerrar();
        };
        dias.append(boton);
      }
      panel.append(nav, dias);
    }
    abrir.onclick = () => {
      if (!panel.hidden) { cerrar(); return; }
      const hoy = new Date();
      mes = new Date(Number(partes[2].value) || hoy.getFullYear(), (Number(partes[1].value) || hoy.getMonth() + 1) - 1, 1);
      dibujar(); panel.hidden = false; abrir.setAttribute('aria-expanded', 'true');
      panel.querySelector('[role="grid"] button').focus();
    };
    panel.addEventListener('keydown', evento => { if (evento.key === 'Escape') cerrar(); });
  }
  if (typeof module !== 'undefined') module.exports = {fechaISO, horaISO, salta};
  if (global.document) {
    const montar = () => document.querySelectorAll('input[type="date"],input[type="time"]').forEach(convertir);
    montar();
    new MutationObserver(montar).observe(document.body, {childList: true, subtree: true});
  }
})(typeof window !== 'undefined' ? window : globalThis);
