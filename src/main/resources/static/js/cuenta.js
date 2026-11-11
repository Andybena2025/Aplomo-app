/* RNF-002, RNF-007: avisos accesibles y envío con indicador; las reglas se validan en Java. */
document.querySelectorAll('[data-cerrar]').forEach(b => b.addEventListener('click', () => b.closest('.aviso').remove()));
document.querySelectorAll('[data-confirmacion]').forEach(a => setTimeout(() => a.remove(), 5000));
document.querySelectorAll('form').forEach(f => f.addEventListener('submit', () => {
  const b = f.querySelector('button[type="submit"],button:not([type])');
  if (b) { b.disabled = true; b.textContent = 'Procesando…'; }
}));
