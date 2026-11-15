/* La foto y el SVG comparten el mismo encuadre; no cambia la medición. */
(function(raiz,fabrica){
  const portada=fabrica();
  if(typeof module==='object'&&module.exports)module.exports=portada;
  if(raiz?.document)portada.montar(raiz.document,raiz);
})(typeof window==='undefined'?null:window,()=>{
  const FOTO={ancho:1400,alto:933},PLOMADA={x:1020,final:800,cabeza:16},PUNTA=PLOMADA.final+16;
  function encuadre(W,H,movil,compacto){
    let k,izquierda,arriba;
    if(movil){
      // Conserva cabeza/punta; cover impide bordes vacíos y permite centrar hasta donde alcanza la foto.
      k=Math.max(W/FOTO.ancho,(H-4)/PUNTA,(H-12)/(PUNTA-PLOMADA.cabeza));
      izquierda=Math.min(0,Math.max(W-FOTO.ancho*k,W*.5-PLOMADA.x*k));
      arriba=Math.min(0,H-4-PUNTA*k);
    }else{
      k=Math.max(W/FOTO.ancho,H/FOTO.alto);
      izquierda=(W-FOTO.ancho*k)*(compacto?.92:.76);
      arriba=(H-FOTO.alto*k)*.5;
    }
    return {ancho:FOTO.ancho*k,alto:FOTO.alto*k,izquierda,arriba,escala:k};
  }
  function montar(doc,win){
    const hero=doc.querySelector('.login .hero'),pz=doc.getElementById('pzOverlay');
    if(!hero||!pz)return;
    function fit(){
      const movil=win.innerWidth<=700,c=encuadre(hero.clientWidth,hero.clientHeight,movil,win.innerWidth<=820);
      Object.assign(pz.style,{width:c.ancho+'px',height:c.alto+'px',left:c.izquierda+'px',top:c.arriba+'px',clipPath:'none'});
      doc.querySelector('.login').style.setProperty('--plomada-x',(c.izquierda+PLOMADA.x*c.escala)+'px');
      hero.style.backgroundSize=movil?c.ancho+'px '+c.alto+'px':'';
      hero.style.backgroundPosition=movil?c.izquierda+'px '+c.arriba+'px':'';
    }
    fit();new win.ResizeObserver(fit).observe(hero);
  }
  return {encuadre,montar};
});
