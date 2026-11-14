/* Coordinación de posturas y persistencia. No mide ni calcula puntajes RULA. */
(function(raiz,fabrica){const api=fabrica();if(typeof module==='object'&&module.exports)module.exports=api;else raiz.AplomoPosturasCampo=api;})(typeof window==='undefined'?globalThis:window,()=>{
 function plan(){return [{criterio:'MAS_INADECUADA',descripcion:'postura más incómoda',titulo:'La más incómoda',motivo:'La postura más incómoda.'},{criterio:'MAYOR_EXPOSICION',descripcion:'postura de mayor duración',titulo:'La que más tiempo mantiene',motivo:'La postura que más tiempo mantiene.'}];}
 function descripcion(puesto,momento,lado){const sufijo=' · '+momento+' · '+(lado==='DERECHO'?'lado derecho':lado==='IZQUIERDO'?'lado izquierdo':'sin fotos');return Array.from(puesto).slice(0,150-Array.from(sufijo).length).join('')+sufijo;}
 function aviso(i,total,p){return 'Postura '+(i+1)+' de '+total+' · '+p.titulo;}
 function ajustes(marcado){return Object.fromEntries(['hombroElevado','brazoSeparado','brazoApoyado','antebrazoCruza','munecaDesviada'].map(k=>[k,!!marcado(k)]));}
 function ordenGuardado(posturas){return [...posturas].sort((a,b)=>(a.puntaje||0)-(b.puntaje||0));}
 async function guardarPendientes(posturas,enviar){for(let i=0;i<posturas.length;i++){const p=posturas[i];if(p.guardada)continue;try{const ruta=await enviar(p,i);if(!/^\/evaluaciones\/\d+(?:\?|$)/.test(ruta||''))throw new Error('No pudimos confirmar el guardado.');p.guardada=ruta;}catch(e){const cantidad=posturas.filter(p=>p.guardada).length;throw new Error((cantidad?'Ya se guardaron '+cantidad+' de '+posturas.length+' posturas. ':'')+e.message);}}}
 return {plan,descripcion,aviso,ajustes,ordenGuardado,guardarPendientes};
});
