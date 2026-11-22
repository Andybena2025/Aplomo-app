/* RF-011/002: calendario y pasos de presentación; permisos, cruces y guardado siempre en el servidor. */
(() => {
 'use strict';
 const iso = (y,m,d) => new Date(Date.UTC(y,m,d)).toISOString().slice(0,10);
 function diasMes(y,m,hoy){
  const primero = new Date(Date.UTC(y,m,1)), dias = new Date(Date.UTC(y,m+1,0)).getUTCDate();
  return [...Array((primero.getUTCDay()+6)%7).fill(null), ...Array.from({length:dias},(_,i)=>{
   const fecha=iso(y,m,i+1);return {fecha,dia:i+1,pasado:fecha<hoy,hoy:fecha===hoy};
  })];
 }
 const finHora=(inicio,duracion)=>{const [h,m]=inicio.split(':').map(Number);return `${String(h+Number(duracion)).padStart(2,'0')}:${String(m).padStart(2,'0')}`;};
 if (typeof module !== 'undefined') module.exports={diasMes,finHora,iso};
 if (typeof document === 'undefined') return;
 const form=document.querySelector('[data-solicitar-visita]'); if(!form)return;
 const q=s=>form.querySelector(s), qa=s=>[...form.querySelectorAll(s)], campo=n=>form.elements.namedItem(n);
 const valor=n=>campo(n)?.value||'', seleccionado=n=>q(`input[name="${n}"]:checked`);
 const hoy=form.dataset.hoy, etapas=qa('[data-etapa]'), estado=q('[data-solicitud-error]');
 let paso=Number(form.dataset.paso)||1, revision=0, peticion;
 const [y,m]= (valor('fecha')||hoy).split('-').map(Number);let mes=new Date(Date.UTC(y,m-1,1));
 form.noValidate=true;
 for(const n of ['fecha','inicio','fin'])campo(n).disabled=false;
 for(const s of ['[data-calendario]','[data-duracion]','[data-horas-panel]','[data-solicitud-controles]'])q(s).hidden=false;
 if(paso===2){campo('inicio').value='';campo('fin').value='';}
 function calendario(){
  q('[data-mes-titulo]').textContent=new Intl.DateTimeFormat('es-CO',{month:'long',year:'numeric',timeZone:'UTC'}).format(mes);
  const caja=q('[data-dias]');caja.replaceChildren();
  for(const dia of diasMes(mes.getUTCFullYear(),mes.getUTCMonth(),hoy)){
   const e=document.createElement(dia?'button':'span');if(dia){e.type='button';e.className='dia-solicitud';e.textContent=dia.dia;
    e.dataset.dia=dia.fecha;e.disabled=dia.pasado;e.setAttribute('aria-label',dia.fecha);
    e.setAttribute('aria-pressed',String(dia.fecha===valor('fecha')));if(dia.hoy)e.setAttribute('aria-current','date');}
   caja.append(e);
  }
 }
 function resumen(){
  const caja=q('[data-solicitud-resumen]');caja.replaceChildren();caja.hidden=false;
  const poner=(texto,clase)=>{const p=document.createElement('p');p.textContent=texto;if(clase)p.className=clase;caja.append(p);return p;};
  const sede=campo('sede');const lugar=sede.tagName==='SELECT'?sede.selectedOptions[0].textContent:q('[data-sede-nombre]').textContent;
  poner(lugar+' · '+(seleccionado('salon')?.parentElement.querySelector('span').textContent||''));
  const persona=seleccionado('ingeniero')?.parentElement;
  if(persona){const p=poner('● '+persona.querySelector('span').textContent);p.style.color=getComputedStyle(persona).getPropertyValue('--persona');}
  if(valor('fecha')){const d=new Date(valor('fecha')+'T12:00:00Z');let f=new Intl.DateTimeFormat('es-CO',{weekday:'long',day:'2-digit',month:'2-digit',year:'numeric',timeZone:'UTC'}).format(d);f=f.charAt(0).toUpperCase()+f.slice(1);poner(f+' · '+valor('inicio')+' a '+valor('fin'));}
  poner(valor('motivo')||'Sin motivo adicional');
 }
 function mostrar(){
  etapas.forEach(e=>e.hidden=Number(e.dataset.etapa)!==paso);
  qa('[data-indicador]').forEach(e=>{if(Number(e.dataset.indicador)===paso)e.setAttribute('aria-current','step');else e.removeAttribute('aria-current');});
  q('[data-anterior]').hidden=paso===1;q('[data-siguiente]').hidden=paso===3;
  if(paso===3)resumen();if(paso===2)cargarHoras();
 }
 function falta(n){return !valor(n);}
 function validar(){
  const incompleto=paso===1?falta('salon'):paso===2?['ingeniero','fecha','inicio','fin'].some(falta):false;
  estado.textContent=incompleto?(paso===1?'Elige un salón para continuar.':'Elige el evaluador, el día y una hora libre para continuar.') : '';
  return !incompleto;
 }
 async function cargarHoras(){
  const turno=++revision;peticion?.abort();peticion=new AbortController();
  const caja=q('[data-horas]'),aviso=q('[data-horas-estado]');caja.replaceChildren();
  if(['ingeniero','fecha','salon'].some(falta)){aviso.textContent='Elige el evaluador y el día para ver las horas libres.';return;}
  aviso.textContent='Cargando horas libres…';
  try{
   const params=new URLSearchParams({ingeniero:valor('ingeniero'),fecha:valor('fecha'),duracion:valor('duracion'),salon:valor('salon')});
   const r=await fetch('/solicitar-visita/horas?'+params,{signal:peticion.signal,headers:{Accept:'application/json'}});
   if(!r.ok)throw Error();const horas=await r.json();if(turno!==revision)return;
   if(!Array.isArray(horas)||horas.some(h=>!/^\d{2}:\d{2}$/.test(h)))throw Error();
   if(!horas.includes(valor('inicio'))){campo('inicio').value='';campo('fin').value='';}
   for(const hora of horas){const b=document.createElement('button');b.type='button';b.className='chip';b.textContent=hora;b.dataset.hora=hora;b.setAttribute('aria-pressed',String(hora===valor('inicio')));caja.append(b);}
   aviso.textContent=horas.length?'':'No hay horas libres este día. Prueba otro día u otro evaluador.';
  }catch(e){if(turno!==revision||e.name==='AbortError')return;aviso.textContent='No pudimos consultar las horas. Vuelve a elegir el día para intentar de nuevo.';campo('inicio').value='';campo('fin').value='';}
 }
 function limpiarHora(){campo('inicio').value='';campo('fin').value='';}
 form.addEventListener('click',e=>{
  const t=e.target.closest('button');if(!t)return;
  if(t.matches('[data-siguiente]')){if(validar()){paso++;mostrar();}}
  if(t.matches('[data-anterior]')){paso--;estado.textContent='';mostrar();}
  if(t.dataset.mes){mes=new Date(Date.UTC(mes.getUTCFullYear(),mes.getUTCMonth()+Number(t.dataset.mes),1));calendario();}
  if(t.dataset.dia){campo('fecha').value=t.dataset.dia;limpiarHora();calendario();cargarHoras();}
  if(t.dataset.hora){campo('inicio').value=t.dataset.hora;campo('fin').value=finHora(t.dataset.hora,valor('duracion'));qa('[data-hora]').forEach(b=>b.setAttribute('aria-pressed',String(b===t)));}
  if(t.dataset.motivo){campo('motivo').value=t.dataset.motivo;resumen();}
 });
 form.addEventListener('change',e=>{
  if(e.target.name==='sede'){location.href='/solicitar-visita?sede='+encodeURIComponent(valor('sede'));return;}
  if(['ingeniero','duracion'].includes(e.target.name)){limpiarHora();cargarHoras();}
 });
 campo('motivo').addEventListener('input',()=>{if(paso===3)resumen();});
 form.addEventListener('submit',e=>{
  if(falta('salon'))paso=1;else if(['ingeniero','fecha','inicio','fin'].some(falta))paso=2;
  if(paso!==3){e.preventDefault();e.stopImmediatePropagation();mostrar();validar();}
 },true);
 calendario();mostrar();
})();
