/* Solo contexto y esfuerzo del último puesto de la misma visita/cuenta, en esta pestaña. */
window.crearMemoriaEvaluacion=form=>{
  const habilitada=!form.dataset.continuar&&!!form.elements.cuenta_recuerdo;
  const clave=habilitada?`aplomo:recorrido:${form.elements.cuenta_recuerdo.value}:${form.elements.visita.value}`:null;
  let anterior=null;
  try{if(clave)anterior=JSON.parse(sessionStorage.getItem(clave));}catch(_){}
  const contextos={2:['postura_opcion','postura'],3:['criterio'],4:['minutos']};
  function marcarRecordada(controles,valor){
    controles.forEach(r=>{const etiqueta=r.closest('label');etiqueta.querySelector('[data-pastilla-anterior]')?.remove();if(r.value===valor){const pastilla=document.createElement('span');pastilla.className='tag tour-pastilla';pastilla.dataset.pastillaAnterior='';pastilla.textContent='Del puesto anterior';etiqueta.append(pastilla);}});
  }
  function restaurar(){
    if(!anterior||typeof anterior!=='object')return;
    for(const nombre of ['postura_opcion','criterio','minutos']){
      const radio=[...form.querySelectorAll(`input[name="${nombre}"]`)].find(r=>r.value===anterior[nombre]);if(radio){radio.checked=true;marcarRecordada([...form.querySelectorAll(`input[name="${nombre}"]`)],radio.value);}
    }
    if(form.querySelector('[name="postura_opcion"]:checked')&&typeof anterior.postura==='string'&&anterior.postura.length<=150)form.elements.postura.value=anterior.postura;
  }
  function aviso(n){
    const panel=form.querySelector(`[data-paso="${n}"]`);if(!panel||!contextos[n])return;
    let nota=panel.querySelector('[data-recuerdo-contexto]');
    if(!nota){nota=document.createElement('p');nota.className='hint';nota.dataset.recuerdoContexto='';nota.textContent='Respuesta del puesto anterior: revísala y pulsa Continuar para confirmarla.';panel.insertBefore(nota,panel.querySelector('h2').nextSibling);}
    nota.hidden=!anterior||!contextos[n].some(k=>anterior[k]);
  }
  return {
    restaurar,aviso,marcarRecordada,
    esfuerzo(foto,lado){return Array.isArray(anterior?.esfuerzos)?anterior.esfuerzos.find(e=>e.foto===foto&&e.lado===lado):null;},
    guardar(tareas){
      if(!clave)return;
      const datos={};for(const nombre of ['postura_opcion','criterio','minutos'])datos[nombre]=form.querySelector(`[name="${nombre}"]:checked`)?.value;
      datos.postura=form.elements.postura.value;
      datos.esfuerzos=tareas.filter(t=>t.analisis?.actividadConfirmada&&t.analisis?.cargaConfirmada).map(t=>({foto:t.indiceFoto,lado:t.analisis.lado,actividad:t.analisis.actividadEleccion,carga:t.analisis.carga}));
      try{sessionStorage.setItem(clave,JSON.stringify(datos));}catch(_){}
    }
  };
};
