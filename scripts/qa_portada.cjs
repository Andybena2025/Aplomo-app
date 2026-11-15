const {test}=require('node:test'),assert=require('node:assert/strict');
const {encuadre}=require('../src/main/resources/static/js/portada.js');
for(const [w,h]of [[360,740],[390,844],[430,932]])test(`portada ${w}x${h}: cabeza y punta completas, sin bordes vacíos`,()=>{
 const H=h*.46,c=encuadre(w,H,true,true),k=c.escala;
 assert.ok(c.izquierda<=0&&c.izquierda+c.ancho>=w);
 assert.ok(c.arriba<=0&&c.arriba+c.alto>=H);
 assert.ok(c.arriba+16*k>=0);assert.ok(c.arriba+816*k<=H);
 assert.ok(Math.abs(c.arriba+816*k-(H-4))<.01);
 assert.ok(c.izquierda+1020*k>=w*.5-.01&&c.izquierda+1020*k<=w*.6);
});
for(const [w,h,compacto]of [[820,489,true],[1366,950,false],[844,504,false]])test(`encuadre anterior conservado ${w}x${h}`,()=>{
 const c=encuadre(w,h,false,compacto),k=Math.max(w/1400,h/933);
 assert.equal(c.ancho,1400*k);assert.equal(c.alto,933*k);
 assert.equal(c.izquierda,(w-c.ancho)*(compacto?.92:.76));assert.equal(c.arriba,(h-c.alto)*.5);
});
