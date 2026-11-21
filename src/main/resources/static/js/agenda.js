"use strict";
document.querySelectorAll("[data-filtrar-evaluador]").forEach(control => {
  control.addEventListener("change", () => {
    document.querySelectorAll("[data-evaluador]").forEach(cita => {
      if (cita.dataset.evaluador === control.dataset.filtrarEvaluador) cita.hidden = !control.checked;
    });
  });
});
document.querySelectorAll(".cal-numero").forEach(boton => {
  boton.addEventListener("click", () => {
    if (!matchMedia("(max-width:700px)").matches) return;
    const destino = document.querySelector(".cal-dia-seleccionado");
    const dia = boton.closest("[data-dia]");
    destino.replaceChildren();
    const titulo = document.createElement("h2");
    titulo.textContent = dia.dataset.dia;
    destino.append(titulo);
    const citas = dia.querySelectorAll(".cal-cita:not([hidden])");
    citas.forEach(cita => destino.append(cita.cloneNode(true)));
    if (!citas.length) destino.append(document.createTextNode("Sin citas"));
    destino.hidden = false;
  });
});
