/* Filtros de presentación sobre las cuentas que el servidor ya autorizó consultar. */
(() => {
  const form = document.querySelector('[data-filtros-usuarios]');
  if (!form) return;
  const filas = [...document.querySelectorAll('[data-cuenta]')];
  const buscar = document.querySelector('#usuarios-buscar');
  const rol = document.querySelector('#usuarios-rol');
  const empresa = document.querySelector('#usuarios-empresa');
  const normalizar = valor => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es').trim();
  const actualizar = () => {
    const q = normalizar(buscar.value);
    let cantidad = 0;
    filas.forEach(fila => {
      fila.hidden = !(normalizar(fila.querySelector('[data-nombre-correo]').textContent).includes(q)
        && (!rol.value || fila.dataset.rol === rol.value)
        && (!empresa.value || fila.dataset.empresa === empresa.value));
      if (!fila.hidden) cantidad++;
    });
    document.querySelector('[data-contador-usuarios]').textContent = `${cantidad} ${cantidad === 1 ? 'cuenta registrada' : 'cuentas registradas'}`;
    document.querySelector('[data-usuarios-vacios]').hidden = cantidad !== 0;
  };
  form.addEventListener('submit', evento => { evento.preventDefault(); actualizar(); });
  buscar.addEventListener('input', actualizar);
  rol.addEventListener('change', actualizar);
  empresa.addEventListener('change', actualizar);
  form.addEventListener('reset', () => { buscar.value = ''; rol.value = ''; empresa.value = ''; actualizar(); });
})();
