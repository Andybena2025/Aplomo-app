import vm from 'node:vm';
import fs from 'node:fs';
import assert from 'node:assert/strict';
const source=fs.readFileSync(process.argv[2],'utf8');
function entorno(){
 const control={falloRed:false,modeloLento:false,deteccionLenta:false,sinPersona:false,activos:0,maximo:0,cerrados:0,plazos:[],requests:[]};
 const L=Array.from({length:33},(_,i)=>({x:.2+i*.01,y:.1+i*.015,visibility:0.2}));
 for(const i of [14,16,20])L[i].visibility=.9;L[0].x=.8;L[8].x=.6;control.L=L;
 class Pose{setOptions(o){control.options=o;}onResults(fn){this.fn=fn;}initialize(){return control.modeloLento?new Promise(()=>{}):Promise.resolve();}async send({image}){assert.equal(image.src,'blob:foto-local');control.activos++;control.maximo=Math.max(control.maximo,control.activos);if(control.deteccionLenta)return new Promise(()=>{});await Promise.resolve();this.fn({poseLandmarks:control.sinPersona?undefined:control.L});control.activos--;}close(){control.cerrados++;return Promise.resolve();}}
 const window={navigator:{onLine:true}};const document={createElement(){return {remove(){}};},head:{append(script){control.requests.push(script.src);queueMicrotask(()=>{if(control.falloRed)script.onerror();else{window.Pose=Pose;script.onload();}});}}};
 class Image{constructor(){this.width=1000;this.height=800;}decode(){return Promise.resolve();}}
 vm.runInNewContext(source,{window,document,Image,Promise,Math,Number,Error,setTimeout(fn,ms){control.plazos.push(ms);return setTimeout(fn,ms/1000);},clearTimeout});
 return {control,window,api:window.AplomoDeteccion};
}
let e=entorno(),r=await e.api.detectarPuntos('blob:foto-local');
assert.deepEqual(Object.keys(r.L).sort(),['cabeza','cadera','codo','hombro','muneca','nudillos']);assert.equal(r.mira,'Derecho');assert.equal(r.L.cadera.x,e.control.L[24].x);assert.equal(r.L.nudillos.x,(e.control.L[20].x+e.control.L[18].x)/2);assert.equal(r.raw.length,33);assert.ok(Object.values(r.L).every(p=>p.x>=0&&p.x<=1&&p.y>=0&&p.y<=1));
for(const i of [13,15,19])e.control.L[i].visibility=1;e.control.L[0].x=.1;
r=await e.api.detectarPuntos('blob:foto-local');assert.equal(r.L.cadera.x,e.control.L[23].x);assert.equal(r.mira,'Izquierdo');
await Promise.all([e.api.detectarPuntos('blob:foto-local'),e.api.detectarPuntos('blob:foto-local')]);assert.equal(e.control.maximo,1);assert.equal(e.control.requests.length,1);
e.control.sinPersona=true;assert.equal(await e.api.detectarPuntos('blob:foto-local'),null);
e=entorno();e.control.falloRed=true;await assert.rejects(e.api.detectarPuntos('blob:foto-local'));e.control.falloRed=false;assert.ok(await e.api.detectarPuntos('blob:foto-local'));
e=entorno();e.control.modeloLento=true;await assert.rejects(e.api.detectarPuntos('blob:foto-local'));assert.ok(e.control.cerrados);e.control.modeloLento=false;assert.ok(await e.api.detectarPuntos('blob:foto-local'));assert.ok([15000,45000,60000].every(t=>e.control.plazos.includes(t)));
e=entorno();e.control.deteccionLenta=true;await assert.rejects(e.api.detectarPuntos('blob:foto-local'));e.control.deteccionLenta=false;assert.ok(await e.api.detectarPuntos('blob:foto-local'));
e=entorno();e.window.navigator.onLine=false;await assert.rejects(e.api.detectarPuntos('blob:foto-local'));assert.equal(e.control.requests.length,0);
console.log('8 escenarios correctos: puntos/coronilla, lado visible, cola y caché, sin persona, red y reintento, plazo de modelo y reintento, plazo de detección y reintento. offline declarado sin solicitudes. Sin red real ni fotos externas.');
