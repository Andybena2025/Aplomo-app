/* RF-019: dos preguntas explícitas por foto y lado; ninguna fórmula RULA. */
window.crearEsfuerzoGuiado=(form,analizador,memoria,actualizar)=>{
  let fotos=[],indice=0,etapa=9,temporizador;
  const grupos={9:'[data-esfuerzo-opcion]',10:'[data-carga-opcion]'};
  function mostrar(n){
    clearTimeout(temporizador);etapa=n;analizador.seleccionar(indice);
    const d=analizador.datos();if(!d)return;
    const recordado=memoria.esfuerzo(d.foto.indiceFoto??indice,d.estado.lado);
    const valor=n===9?d.estado.actividadEleccion:d.estado.carga;
    const previo=n===9?recordado?.actividad:recordado?.carga;
    const opcion=valor||previo;
    for(const radio of form.querySelectorAll(grupos[n]))radio.checked=radio.value===opcion;
    const panel=form.querySelector(`[data-paso="${n}"]`);
    panel.querySelector('[data-tarea-esfuerzo]').textContent=`Foto ${(d.foto.indiceFoto??indice)+1} · Lado ${d.estado.lado==='DERECHO'?'derecho':'izquierdo'} · ${indice+1} de ${fotos.length}`;
    panel.querySelector('[data-recuerdo-esfuerzo]').hidden=!!valor||!previo;
    actualizar();
  }
  function respondido(){return !!form.querySelector(grupos[etapa]+':checked');}
  function confirmar(){
    const opcion=form.querySelector(grupos[etapa]+':checked');if(!opcion)return false;
    analizador.elegirEsfuerzo(etapa===9?'actividad':'carga',opcion.value);return true;
  }
  form.addEventListener('change',e=>{
    if(!e.target.matches('[data-esfuerzo-opcion],[data-carga-opcion]'))return;
    clearTimeout(temporizador);actualizar();
    if(!matchMedia('(prefers-reduced-motion: reduce)').matches)temporizador=setTimeout(()=>form.querySelector('[data-siguiente]').click(),250);
  });
  return {
    preparar(fs){fotos=fs;indice=0;},
    mostrar,
    respondido,
    continuar(){if(!confirmar())return null;if(etapa===9)return 10;if(indice<fotos.length-1){indice++;return 9;}return 11;},
    atras(){clearTimeout(temporizador);if(etapa===10)return 9;if(indice>0){indice--;return 10;}return 8;},
    ultimo(){indice=Math.max(0,fotos.length-1);},
    cancelar(){clearTimeout(temporizador);},
    progreso(){return {actual:indice*2+(etapa===10?1:0),total:Math.max(1,fotos.length)*2};}
  };
};
