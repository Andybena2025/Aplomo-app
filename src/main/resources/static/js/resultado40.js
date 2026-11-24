/* Paso 40 · RF-020/023, RNF-007: presenta datos del servidor; no calcula puntajes RULA. */
(function(root){
 'use strict';
 const e=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const color=v=>['VERDE','AMARILLO','ROJO'].includes(v)?v:'VERDE';
 const etiqueta=v=>({VERDE:'Verde',AMARILLO:'Amarillo',ROJO:'Rojo'}[v]||'Sin resultado');
 // (propuesta) RNF-007: efectos posibles, en un único mapa de textos fácil de cambiar.
 const EFECTOS=Object.freeze({
  MUNECA:['Molestia en muñeca y antebrazo','Cansancio, hormigueo o dolor al final del día.'],
  ANTEBRAZO:['Molestia en antebrazo y codo','Fatiga o molestia al repetir el movimiento.'],
  TRONCO:['Carga en espalda','Tensión que se acumula con las horas.'],
  CUELLO:['Carga en cuello','Tensión que se acumula con las horas.'],
  BRAZO:['Carga en brazo y hombro','Fatiga o molestia al mantener el brazo levantado.'],
  PIERNAS:['Cansancio en piernas','Fatiga al mantener el peso sin un apoyo estable.']
 });
 const significados={bajo:'El riesgo es bajo. La postura es aceptable si no se mantiene mucho tiempo.',
  medio:'El riesgo es moderado. Conviene revisar el puesto y hacer cambios pronto.',
  alto:'El riesgo es alto. Esta postura puede causar dolor o lesiones si se sigue repitiendo. Hay que cambiar el puesto antes de seguir trabajando así.'};
 const anillo=(d,tipo='pequeno')=>`<span class="r40-anillo ${tipo} ${color(d.color)}" aria-label="Puntaje ${e(d.puntaje)}">${e(d.puntaje)}</span>`;
 const icono=tipo=>`<span class="r40-icono" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">${tipo===2?'<path d="M12 3v9l6 3M12 3a9 9 0 1 0 9 9"/>':tipo===1?'<path d="M5 19l4-9 3 4 3-9 4 14M4 5h16"/>':'<path d="M5 19l4-6 3 2 5-10M4 8h4M16 19h4"/>'}</svg></span>`;
 let secuencia=0;
 // RNF-007: Catmull-Rom cerrada convertida a curvas C; puntos y proporciones indicados por Andy.
 const derecho=[[85,46],[86,55],[97,59],[110,62],[119,70],[123,84],[125,102],[126,120],[127,136],[128,152],[129,166],[132,176],[131,188],[125,192],[120,184],[119,170],[117,154],[115,138],[113,122],[110,104],[107,90],[106,108],[103,130],[105,152],[108,176],[107,204],[104,228],[104,250],[101,272],[100,284],[107,293],[98,296],[89,293],[88,280],[88,255],[87,228],[85,200],[82,178],[80,174]];
 function contorno(){
  const puntos=[...derecho,...derecho.slice(0,-1).reverse().map(([x,y])=>[160-x,y])],n=puntos.length;
  const fmt=p=>p.map(x=>Number(x.toFixed(2))).join(',');
  let d='M'+fmt(puntos[0]);
  puntos.forEach((b,i)=>{const a=puntos[(i+n-1)%n],c=puntos[(i+1)%n],f=puntos[(i+2)%n];
   d+=' C'+fmt([b[0]+(c[0]-a[0])/6,b[1]+(c[1]-a[1])/6])+' '+fmt([c[0]-(f[0]-b[0])/6,c[1]-(f[1]-b[1])/6])+' '+fmt(c);
  });return d+' Z';
 }
 function figura(segmentos){
  const pref='cuerpo40-'+(++secuencia),d=contorno(),por=new Map(segmentos.map(s=>[s.clave,s]));
  const centros={CUELLO:[80,54,12,12],TRONCO:[80,112,26,42],BRAZO:[117,96,11,24],ANTEBRAZO:[122,146,9,18],MUNECA:[126,178,9,12],PIERNAS:[95,232,13,55]};
  let defs='',zonas='';
  Object.entries(centros).forEach(([k,[x,y,rx,ry]])=>{
   const s=por.get(k),clase=color(s?.color),id=pref+'-'+k;
   defs+=`<radialGradient id="${id}"><stop stop-color="var(--${clase.toLowerCase()})" stop-opacity=".85"/><stop offset="1" stop-color="var(--${clase.toLowerCase()})" stop-opacity="0"/></radialGradient>`;
   for(const cx of x===80?[x]:[x,160-x])zonas+=`<ellipse cx="${cx}" cy="${y}" rx="${rx}" ry="${ry}" fill="url(#${id})"/>`;
   if(k==='CUELLO')zonas+=`<ellipse cx="80" cy="28" rx="14" ry="17" fill="url(#${id})"/>`;
  });
  return `<figure class="r40-cuerpo"><svg viewBox="0 0 160 300" role="img" aria-label="Mapa del riesgo por partes del cuerpo"><defs><linearGradient id="${pref}-marfil" x2="0" y2="1"><stop stop-color="var(--ivory)" stop-opacity=".07"/><stop offset="1" stop-color="var(--ivory)" stop-opacity=".02"/></linearGradient><clipPath id="${pref}-clip"><path d="${d}"/><ellipse cx="80" cy="28" rx="14" ry="17"/></clipPath>${defs}</defs><g fill="url(#${pref}-marfil)" stroke="var(--marca)" stroke-width="1.1" stroke-opacity=".75"><path d="${d}"/><ellipse cx="80" cy="28" rx="14" ry="17"/></g><g clip-path="url(#${pref}-clip)">${zonas}</g><path d="M80 5V298" stroke="var(--marca)" stroke-width=".7" stroke-dasharray="2 4" opacity=".55"/></svg><figcaption><span class="VERDE">● Bien</span><span class="AMARILLO">● Revisar</span><span class="ROJO">● Pesa más</span></figcaption></figure>`;
 }
 function trabajador(t){
  if(!t||!Object.keys(t).length)return '';
  const generos={FEMENINO:'Femenino',MASCULINO:'Masculino',OTRO:'Otro',NO_DICE:'Prefiere no decir'},antiguedades={MENOS_1:'Menos de 1 año',DE_1_A_5:'1 a 5 años',DE_6_A_10:'6 a 10 años',MAS_10:'Más de 10 años'};
  const datos=[];if(t.genero)datos.push(['Género',generos[t.genero]]);if(t.edad)datos.push(['Edad',t.edad+' años']);if(t.antiguedad)datos.push(['En la empresa',antiguedades[t.antiguedad]]);if(t.minutos)datos.push(['Jornada',(t.minutos/60).toLocaleString('es-CO',{maximumFractionDigits:1})+' h al día']);
  return datos.length?`<section class="r40-trabajador"><strong>Trabajador</strong><div>${datos.map(([k,v])=>`<span class="tag">${e(k)}: ${e(v)}</span>`).join('')}</div><small>Solo contexto · no cambia el puntaje RULA</small></section>`:'';
 }
 function estado(a,datos,csrf,indice=0,multiples=false){
  if(!a)return '<span class="r40-estado">'+(datos.seguimientoDisponible===false?'Solo consulta':datos.guardado?'Sin sugerencia registrada':'Pendiente')+'</span>';
  const texto={PENDIENTE:'Pendiente',EN_PROCESO:'En proceso',HECHA:'Hecha'}[a.estado]||'Pendiente';
  if(!datos.editable)return `<span class="r40-estado ${a.estado==='HECHA'?'hecha':''}">${texto}</span>`;
  return `<form data-sugerencia-form class="r40-avance ${a.estado==='HECHA'?'hecha':''}" method="post" action="/sugerencias/${Number(a.id)}/estado"><input type="hidden" name="csrf" value="${e(csrf)}"><label><span class="${multiples?'r40-cambio-estado':'sr-only'}">${multiples?'Cambio '+(indice+1):'Estado: '+e(a.texto)}</span><select class="input" name="estado" aria-label="Estado: ${e(a.texto)}">${Object.entries({PENDIENTE:'Pendiente',EN_PROCESO:'En proceso',HECHA:'Hecha'}).map(([v,t])=>`<option value="${v}"${a.estado===v?' selected':''}>${t}</option>`).join('')}</select></label><button class="btn sm" type="submit">Guardar estado</button><div data-error-sugerencia role="alert" hidden><span></span><button type="button" class="link" data-cerrar-error>Cerrar</button></div></form>`;
 }
 function render(posturas,csrf=''){
  const tarjetas=posturas.map(p=>({titulo:p.titulo,datos:[...(p.analisis||[])].sort((a,b)=>b.puntaje-a.puntaje)[0]})).filter(p=>p.datos);
  if(!tarjetas.length)return '<p class="hint">Esta evaluación se guardará sin fotos ni puntaje RULA.</p>';
  const critica=tarjetas.reduce((mejor,p)=>p.datos.puntaje>mejor.datos.puntaje?p:mejor),d=critica.datos,segmentos=d.segmentos||[];
  const significado=significados[d.puntaje<=2?'bajo':d.puntaje<=4?'medio':'alto'];
  const tramos=(d.niveles||[]).map(n=>`<div class="r40-tramo ${color(n.color)} ${d.puntaje>=n.min&&d.puntaje<=n.max?'actual':''}"><strong>${n.min===n.max?n.min:n.min+'–'+n.max}</strong><span>${e(n.accion)}</span><small>${d.puntaje>=n.min&&d.puntaje<=n.max?'▼ Este puesto':'Nivel '+n.nivel}</small></div>`).join('');
  const post=tarjetas.map(p=>`<article class="r40-postura"><div class="r40-postura-cabecera">${anillo(p.datos)}<div><h3>${e(p.titulo)}</h3><span>${etiqueta(p.datos.color)} · Nivel ${e(p.datos.nivel)} de 4</span>${p===critica?'<strong class="r40-critica">MÁS CRÍTICA</strong>':''}</div></div><details class="r40-desglose"><summary>Ver desglose ›</summary><div class="table-scroll">${p.datos.desglose||''}</div></details></article>`).join('');
  const antes=d.anterior==null?'':`<aside class="r40-antes">Evaluación anterior <strong class="${color((d.niveles||[]).find(n=>d.anterior>=n.min&&d.anterior<=n.max)?.color)}">${e(d.anterior)}</strong> → Ahora <strong class="${color(d.color)}">${e(d.puntaje)}</strong></aside>`;
  const filas=segmentos.map(s=>`<article class="r40-segmento ${color(s.color)}"><div><strong>${e(s.nombre)}</strong>${s.color==='ROJO'?'<span class="r40-pesa">PESA MÁS</span>':''}<p>${e(s.medido)}</p><span class="r40-barra"><i style="width:${Math.max(0,Math.min(100,s.peso*100))}%"></i></span></div><strong>${e(s.puntos)} de ${e(s.maximo)}</strong></article>`).join('');
  const efectos=segmentos.slice(0,2).map((s,i)=>{const f=EFECTOS[s.clave];return f?`<article>${icono(i)}<h3>${e(f[0])}</h3><p>${e(f[1])}</p></article>`:'';}).join('');
  const plan=segmentos.filter(s=>s.puntos>s.minimo);
  const rows=plan.map((s,i)=>`<tr class="${color(s.color)}" data-plan-parte="${e(s.clave)}"><td data-label="#"><b class="r40-numero">${i+1}</b></td><td data-label="Parte"><strong>${e(s.nombre)}</strong></td><td data-label="Hoy → Meta"><s>${e(s.medido)}</s><span class="r40-meta">→ ${e(s.meta)}</span></td><td data-label="Cambio en el puesto">${s.acciones?.length>1?'<ol class="r40-cambios">'+s.acciones.map(a=>'<li>'+e(a.texto)+'</li>').join('')+'</ol>':e(s.cambio)}</td><td data-label="Puntos"><s>${e(s.puntos)}</s> → <span class="r40-meta-minima">${e(s.minimo)}</span></td><td data-label="Estado">${s.acciones?.length?s.acciones.map((a,i)=>estado(a,d,csrf,i,s.acciones.length>1)).join(''):estado(null,d,csrf)}</td></tr>`).join('');
  const otras=[...new Map(tarjetas.flatMap(p=>p.datos.otras||[]).map(a=>[a.id,a])).values()];
  const extras=otras.map((a,i)=>`<tr><td data-label="#">${plan.length+i+1}</td><td data-label="Parte">Otra</td><td data-label="Hoy → Meta">—</td><td data-label="Cambio en el puesto">${e(a.texto)}</td><td data-label="Puntos">No cambia el puntaje</td><td data-label="Estado">${estado(a,d,csrf)}</td></tr>`).join('');
  const faltantes=tarjetas.filter(p=>p.datos.sinSugerencias).map(p=>`<p class="r40-sin-sugerencias">El análisis de ${e(p.titulo)} está guardado. Aún necesita una sugerencia.${p.datos.editable?` <a class="link" href="/sugerencias/nueva?evaluacion=${Number(p.datos.evaluacion)}">Escribir sugerencia ›</a>`:''}</p>`).join('');
  const pro=d.proyeccion,proyectado=pro?`<div class="r40-proyeccion">${anillo(d,'proyeccion')}<span aria-hidden="true">→</span>${anillo(pro,'proyeccion')}<div><strong>${pro.puntaje<d.puntaje?'Si se cumple el plan, el puesto bajaría a':'Con estos cambios, el puntaje se mantiene en'} ${e(pro.puntaje)} · ${etiqueta(pro.color)}</strong><p>Estimación con la misma actividad y carga; debe confirmarse en una reevaluación.</p></div></div>`:'';
  const id='resultado41-'+(++secuencia);
  const titulos=['¿Por qué salió '+d.puntaje+'?','¿Por qué importa?','Plan de atención'];
  const pestanas=`<div class="r40-pestanas" role="tablist" aria-label="Entender el resultado" hidden>${titulos.map((t,i)=>`<button type="button" role="tab" id="${id}-tab-${i}" aria-controls="${id}-panel-${i}" aria-selected="${i===0}" tabindex="${i===0?0:-1}"><span aria-hidden="true">${i+1}</span>${e(t)}</button>`).join('')}</div>`;
  const panel=i=>`role="tabpanel" id="${id}-panel-${i}" aria-labelledby="${id}-tab-${i}" tabindex="0"`;
  return `<div class="resultado40"><section class="r40-card r40-principal ${color(d.color)}"><div class="r40-cabecera">${anillo(d,'grande')}<div><span class="r40-nivel">${etiqueta(d.color)} · Nivel ${e(d.nivel)} de 4</span><h2>${e(d.accion)}</h2><p>${e(significado)}</p></div></div><div class="r40-escala" aria-label="Escala RULA de 1 a 7">${tramos}</div><div class="r40-posturas">${post}</div><p class="hint">El puesto toma el puntaje de la postura más crítica.</p></section>${trabajador(d.trabajador)}${antes}${pestanas}<section class="r40-card" ${panel(0)}><header class="r40-seccion"><span>1</span><div><h2>¿Por qué salió ${e(d.puntaje)}?</h2><p>${e(critica.titulo)} · Lado ${d.lado==='IZQUIERDO'?'izquierdo':'derecho'}</p></div></header><div class="r40-desglose-cuerpo">${figura(segmentos)}<div>${filas}<p class="hint">${d.actividad||d.carga?'Se suma: '+(d.actividad?'actividad muscular +'+e(d.actividad):'')+(d.actividad&&d.carga?' · ':'')+(d.carga?'carga o fuerza +'+e(d.carga):'')+' en ambos grupos.':'Sin incremento por actividad muscular ni carga.'}</p></div></div></section><section class="r40-card" ${panel(1)}><header class="r40-seccion"><span>2</span><h2>¿Por qué importa?</h2></header><div class="r40-efectos">${efectos}<article>${icono(2)}<h3>Con el tiempo</h3><p>Más riesgo de lesiones de músculos y tendones (trastornos musculoesqueléticos).</p></article></div><p class="hint">RULA mide el riesgo de la postura. No es un diagnóstico médico.</p></section><section class="r40-card" ${panel(2)}><header class="r40-seccion"><span>3</span><h2>Plan de atención</h2></header><p class="r40-plazo ${color(d.color)}">Nivel ${etiqueta(d.color).toLowerCase()}: ${e(d.accion)} · ${d.puntaje>=5?'Hacer estos cambios, en orden, antes de seguir trabajando en esta postura.':d.puntaje>=3?'Revisar el puesto y organizar los cambios en este orden.':'Mantener las condiciones y revisar la postura durante la jornada.'}</p>${plan.length||otras.length?`<div class="r40-plan-scroll"><table class="r40-plan"><thead><tr>${['#','Parte','Hoy → Meta','Cambio en el puesto','Puntos','Estado'].map(t=>'<th>'+t+'</th>').join('')}</tr></thead><tbody>${rows}${extras}</tbody></table></div>`:`<p>${d.puntaje>2?'Los segmentos ya tienen su puntaje mínimo. Revisa la actividad muscular y la carga con el evaluador.':'Los segmentos ya tienen su puntaje mínimo. Mantén las condiciones del puesto.'}</p>`}${faltantes}${proyectado}${d.reserva?`<a class="btn" href="${e(d.reserva)}">Agendar reevaluación ›</a>`:''}</section></div>`;
 }
 // RNF-007/008: mejora progresiva; el HTML base y la impresión mantienen las tres secciones.
 function iniciar(contenedor,entorno=typeof window==='undefined'?{}:window){
  contenedor.querySelectorAll('.resultado40').forEach(resultado=>{
   const barra=resultado.querySelector('.r40-pestanas');
   if(!barra||barra.dataset.lista)return;
   const botones=[...barra.querySelectorAll('[role="tab"]')];
   const paneles=[...resultado.querySelectorAll('[role="tabpanel"]')];
   if(botones.length!==3||paneles.length!==3)return;
   const parametros=new URLSearchParams(entorno.location?.search||'');parametros.delete('pestana');
   const clave='alineo:resultado:pestana:'+(entorno.location?.pathname||'')+(parametros.size?'?'+parametros:'');
   const recordar=indice=>{try{entorno.sessionStorage?.setItem(clave,String(indice));}catch(_){}};
   const seleccionar=(indice,enfocar=false)=>{
    botones.forEach((b,i)=>{b.setAttribute('aria-selected',String(i===indice));b.tabIndex=i===indice?0:-1;paneles[i].hidden=i!==indice;});
    if(enfocar)botones[indice].focus();
    recordar(indice);
   };
   let inicial=0;
   try{const guardada=Number(entorno.sessionStorage?.getItem(clave));if(Number.isInteger(guardada)&&guardada>=0&&guardada<3)inicial=guardada;}catch(_){}
   if(new URLSearchParams(entorno.location?.search||'').get('pestana')==='plan')inicial=2;
   botones.forEach((b,i)=>{
    b.addEventListener('click',()=>seleccionar(i));
    b.addEventListener('keydown',evento=>{
     const desplazamiento={ArrowRight:1,ArrowLeft:-1}[evento.key];
     if(desplazamiento){evento.preventDefault();seleccionar((i+desplazamiento+3)%3,true);}
     else if(evento.key==='Home'||evento.key==='End'){evento.preventDefault();seleccionar(evento.key==='Home'?0:2,true);}
    });
   });
   resultado.addEventListener('submit',evento=>{
    if(evento.target.matches('[data-sugerencia-form]'))recordar(2);
   },true);
   barra.dataset.lista='si';resultado.classList.add('r40-con-pestanas');
   seleccionar(inicial);barra.hidden=false;
  });
 }
 const api={render,trabajador,contorno,EFECTOS,iniciar};
 if(typeof module!=='undefined'&&module.exports)module.exports=api;else root.AlineoResultado40=api;
 if(typeof document!=='undefined')document.querySelectorAll('[data-resultado40-json]').forEach(script=>{
  const destino=script.previousElementSibling;destino.innerHTML=render(JSON.parse(script.textContent),script.dataset.csrf||'');iniciar(destino);
 });
})(typeof window==='undefined'?{}:window);
