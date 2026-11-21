/* RNF-007: navegación entre artículos autorizados, con enlaces utilizables sin JavaScript. */
(() => {
  const articulos = [...document.querySelectorAll('[data-articulo]')];
  const enlaces = [...document.querySelectorAll('[data-articulo-enlace]')];
  if (!articulos.length) return;
  function mostrar() {
    const elegida = articulos.find(articulo => `#${articulo.id}` === location.hash) || articulos[0];
    articulos.forEach(articulo => { articulo.hidden = articulo !== elegida; });
    enlaces.forEach(enlace => {
      if (enlace.hash === `#${elegida.id}`) enlace.setAttribute('aria-current', 'page');
      else enlace.removeAttribute('aria-current');
    });
  }
  addEventListener('hashchange', mostrar);
  document.querySelectorAll('[data-util]').forEach(boton => {
    boton.onclick = () => {
      boton.parentElement.querySelectorAll('[data-util]').forEach(otro => {
        otro.setAttribute('aria-pressed', String(otro === boton));
      });
    };
  });
  mostrar();
})();
