/* RF-026: impresión nativa; el navegador permite imprimir o guardar como PDF. */
document.querySelector('[data-imprimir]')?.addEventListener('click', () => window.print());
