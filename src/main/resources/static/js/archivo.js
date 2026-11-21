"use strict";
document.querySelector("[data-buscar-archivo]")?.addEventListener("input", evento => {
  const texto = evento.target.value.toLocaleLowerCase("es");
  document.querySelectorAll("[data-empresa-archivo]").forEach(enlace => {
    enlace.hidden = !enlace.textContent.toLocaleLowerCase("es").includes(texto);
  });
});
