/* Coordinación de posturas y persistencia. No mide ni calcula puntajes RULA. */
(function(raiz,fabrica){const api=fabrica();if(typeof module==='object'&&module.exports)module.exports=api;else raiz.AplomoPosturasCampo=api;})(typeof window==='undefined'?globalThis:window,()=>{
 function plan(){return [{criterio:'MAS_INADECUADA',descripcion:'postura más incómoda',titulo:'La más incómoda',motivo:'La postura más incómoda.'},{criterio:'MAYOR_EXPOSICION',descripcion:'postura de mayor duración',titulo:'La que más tiempo mantiene',motivo:'La postura que más tiempo mantiene.'}];}
 function descripcion(puesto,momento,lado){const sufijo=' · '+momento+' · '+(lado==='DERECHO'?'lado derecho':lado==='IZQUIERDO'?'lado izquierdo':'sin fotos');return Array.from(puesto).slice(0,150-Array.from(sufijo).length).join('')+sufijo;}
 function aviso(i,total,p){return 'Postura '+(i+1)+' de '+total+' · '+p.titulo;}
 function ajustes(marcado){return Object.fromEntries(['hombroElevado','brazoSeparado','brazoApoyado','antebrazoCruza','munecaDesviada'].map(k=>[k,!!marcado(k)]));}
 function retirarFoto(fotos,tareas,actual){const misma=f=>actual.id?f.id===actual.id:f.file===actual.file;const restantes=fotos.filter(f=>!misma(f)),pendientes=tareas.filter(f=>!misma(f));pendientes.forEach(f=>f.indiceFoto=restantes.findIndex(mismaFoto=>f.id?mismaFoto.id===f.id:mismaFoto.file===f.file));return {fotos:restantes,tareas:pendientes};}
 function puntosBrazo(raw,lado){if(!raw||raw.length<33)return null;const ids=lado==='DERECHO'?[24,12,14,16,20,18]:[23,11,13,15,19,17];if(ids.some(i=>!Number.isFinite(raw[i]?.x)||!Number.isFinite(raw[i]?.y)))return null;const p=i=>({x:Math.max(0,Math.min(1,raw[i].x)),y:Math.max(0,Math.min(1,raw[i].y))});return {cadera:p(ids[0]),hombro:p(ids[1]),codo:p(ids[2]),muneca:p(ids[3]),nudillos:{x:(p(ids[4]).x+p(ids[5]).x)/2,y:(p(ids[4]).y+p(ids[5]).y)/2}};}
 function estadoSeguir(falta,ocupada,paso){return {visible:true,desactivado:!!falta||ocupada,texto:ocupada?'Cargando…':paso===5?'Guardar y terminar':'Seguir →'};}
 function sugerenciaAutomatica(segmento,grados,rango,ia){return segmento==='CUELLO'&&ia&&grados<0&&grados>=-10?{rango:5,revisar:true,ajustadaIA:true}:{rango,revisar:false,ajustadaIA:false};}
 function critica(posturas){const max=Math.max(...posturas.map(p=>p.puntaje||0)),ganadoras=posturas.filter(p=>(p.puntaje||0)===max);return {max,indice:max&&ganadoras.length===1?posturas.indexOf(ganadoras[0]):-1,empate:posturas.length>1&&ganadoras.length>1};}
 // Solo elige la orientación de la foto; los ángulos se siguen midiendo en Java.
 function ladoFoto(mira,puntos,elegido='DERECHO'){
  if(mira==='Derecho'||mira==='DERECHO')return 'DERECHO';
  if(mira==='Izquierdo'||mira==='IZQUIERDO')return 'IZQUIERDO';
  const c=puntos?.cadera,h=puntos?.coronilla||puntos?.cabeza,m=puntos?.muneca;
  if(c&&h&&m&&[c.x,h.x,m.x].every(Number.isFinite)){
   const signos=[h.x-c.x,m.x-c.x].filter(v=>Math.abs(v)>=.025).map(Math.sign);
   if(signos.length&&signos.every(v=>v===signos[0]))return signos[0]>0?'DERECHO':'IZQUIERDO';
  }
  return elegido==='IZQUIERDO'?'IZQUIERDO':'DERECHO';
 }
 function avisoLado(anterior,actual){return anterior&&anterior!==actual?'Esta foto muestra el lado '+(actual==='DERECHO'?'derecho':'izquierdo')+'; lo ajustamos.':'';}
 function mismoLado(tareas,modo){return modo==='AMBOS'&&tareas.length>1&&tareas[0].analisis?.lado===tareas[1].analisis?.lado?'Esta foto muestra el mismo lado que la primera. Sube una del otro lado, o sigue solo con este brazo.':'';}
 function modoPostura(postura){return postura.modoLado||'AUTO';}
 function nuevoApoyoPies(anterior={}){return {marcada:!!anterior.marcada,tocada:!!anterior.tocada,sugerida:false};}
 function proponerApoyoPies(estado,sugerencia,origen){const siguiente={...estado,sugerida:false};return origen==='ia'&&!estado.tocada&&sugerencia===true?{...siguiente,marcada:true,sugerida:true}:siguiente;}
 function tocarApoyoPies(estado,marcada){return {...estado,marcada:!!marcada,tocada:true,sugerida:false};}
 function ordenGuardado(posturas){return [...posturas].sort((a,b)=>(a.puntaje||0)-(b.puntaje||0));}
 async function guardarPendientes(posturas,enviar){for(let i=0;i<posturas.length;i++){const p=posturas[i];if(p.guardada)continue;try{const ruta=await enviar(p,i);if(!/^\/evaluaciones\/\d+(?:\?|$)/.test(ruta||''))throw new Error('No pudimos confirmar el guardado.');p.guardada=ruta;}catch(e){const cantidad=posturas.filter(p=>p.guardada).length;throw new Error((cantidad?'Ya se guardaron '+cantidad+' de '+posturas.length+' posturas. ':'')+e.message);}}}
 return {nuevoApoyoPies,proponerApoyoPies,tocarApoyoPies,ladoFoto,avisoLado,mismoLado,modoPostura,plan,descripcion,aviso,ajustes,sugerenciaAutomatica,critica,retirarFoto,puntosBrazo,estadoSeguir,ordenGuardado,guardarPendientes};
});
