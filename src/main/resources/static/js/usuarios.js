/* RF-003: filtros sobre filas autorizadas; no cambian permisos ni cuentas. */
(() => {
  const lista = document.querySelector('[data-seccion="USUARIOS"]');
  if (!lista) return;
  const parametros = new URLSearchParams(location.search);
  let rol = parametros.get('rol') || '';
  const empresa = lista.querySelector('[data-empresa-usuarios]');
  empresa.value = parametros.get('empresa') || '';
  function preparar() {
    lista.querySelectorAll('[data-registro]').forEach(fila => {
      fila.dataset.filtroExtra = String((!rol || fila.dataset.rol === rol)
        && (!empresa.value || fila.dataset.empresa.split(' ').includes(empresa.value)));
    });
    lista.querySelectorAll('[data-rol-elegir]').forEach(boton => {
      boton.setAttribute('aria-pressed', String(boton.dataset.rolElegir === rol));
    });
    lista.dispatchEvent(new Event('filtros-cambiados'));
  }
  lista.querySelectorAll('[data-rol-elegir]').forEach(boton => {
    boton.onclick = () => { rol = rol === boton.dataset.rolElegir ? '' : boton.dataset.rolElegir; preparar(); };
  });
  empresa.onchange = preparar;
  preparar();
})();
