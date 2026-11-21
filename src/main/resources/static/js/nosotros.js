/* Paso 25: enlaces de portada, foco explícito y movimiento reducido. */
(()=>{const volver=document.querySelector('[data-volver-login]'),conoce=document.querySelector('[data-conoce]');if(!volver||!conoce)return;
const ir=(elemento)=>elemento?.scrollIntoView({behavior:matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth',block:'start'});
conoce.addEventListener('click',e=>{e.preventDefault();history.replaceState(null,'','#nosotros');ir(document.querySelector('#nosotros'));});
volver.addEventListener('click',e=>{e.preventDefault();history.replaceState(null,'',location.pathname);ir(document.querySelector('.login'));document.querySelector('#correo')?.focus({preventScroll:true});});
})();
