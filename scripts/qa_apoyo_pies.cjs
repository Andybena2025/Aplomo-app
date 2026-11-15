const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const contexto={window:{addEventListener(){}},performance,setTimeout,clearTimeout};vm.runInNewContext(fs.readFileSync('src/main/resources/static/js/deteccion.js','utf8'),contexto);
const ia=contexto.window.AplomoDeteccion,r=require('../src/main/resources/static/js/posturas-campo.js');
function puntos(diferencia=0,altura=.5){const p=Array.from({length:33},()=>({x:.5,y:.5,visibility:1}));p[23].y=p[24].y=.35;p[27].y=.35+altura-diferencia/2;p[28].y=.35+altura+diferencia/2;return p;}
test('tobillos a la misma altura no proponen cambiar la casilla',()=>assert.equal(ia.sugerirPiesSinApoyo(puntos()),null));
test('desnivel del 20 por ciento propone pies sin buen apoyo',()=>assert.equal(ia.sugerirPiesSinApoyo(puntos(.1)),true));
test('el 15 por ciento exacto y un desnivel menor no sugieren',()=>{assert.equal(ia.sugerirPiesSinApoyo(puntos(.075)),null);assert.equal(ia.sugerirPiesSinApoyo(puntos(.07)),null);});
test('exige visibilidad de las dos caderas y los dos tobillos',()=>{for(const i of [23,24,27,28]){const p=puntos(.1);p[i].visibility=ia.PERFIL.visibilidad-.001;assert.equal(ia.sugerirPiesSinApoyo(p),null);p[i].visibility=ia.PERFIL.visibilidad;assert.equal(ia.sugerirPiesSinApoyo(p),true);}});
test('Oficina excluye la sugerencia aunque el desnivel sea grande',()=>assert.equal(ia.sugerirPiesSinApoyo(puntos(.1),{contexto:'OFICINA'}),null));
test('separación pequeña cadera-tobillos excluye fotos sentadas de Planta',()=>assert.equal(ia.sugerirPiesSinApoyo(puntos(.1,.2),{contexto:'PLANTA'}),null));
test('puntos ausentes, fuera de foto o no finitos no sugieren',()=>{assert.equal(ia.sugerirPiesSinApoyo(null),null);for(const cambio of [p=>delete p[28],p=>p[23].x=1.1,p=>p[27].y=NaN]){const p=puntos(.1);cambio(p);assert.equal(ia.sugerirPiesSinApoyo(p),null);}});
test('frente y detalle no proponen apoyo de pies',()=>{for(const vista of ['FRENTE','DETALLE'])assert.equal(ia.sugerirPiesSinApoyo(puntos(.1),{vista}),null);});
test('IA preselecciona la casilla y habilita Según la foto',()=>{const e=r.proponerApoyoPies(r.nuevoApoyoPies(),true,'ia');assert.deepEqual(e,{marcada:true,tocada:false,sugerida:true});});
test('la decisión humana se conserva tanto marcada como desmarcada',()=>{for(const marcada of [false,true]){const e=r.tocarApoyoPies(r.nuevoApoyoPies(),marcada),s=r.proponerApoyoPies(e,true,'ia');assert.equal(s.marcada,marcada);assert.equal(s.tocada,true);assert.equal(s.sugerida,false);}});
test('un toque retira la pastilla sin dejar de ser una elección humana',()=>{const e=r.proponerApoyoPies(r.nuevoApoyoPies(),true,'ia'),s=r.tocarApoyoPies(e,false);assert.equal(s.marcada,false);assert.equal(s.sugerida,false);assert.equal(s.tocada,true);});
test('detección sin sugerencia nunca marca ni desmarca',()=>{for(const marcada of [false,true]){const s=r.proponerApoyoPies({...r.nuevoApoyoPies(),marcada},null,'ia');assert.equal(s.marcada,marcada);assert.equal(s.sugerida,false);}});
test('Calcular manualmente nunca aplica una sugerencia',()=>{const s=r.proponerApoyoPies(r.nuevoApoyoPies(),true,'manual');assert.equal(s.marcada,false);assert.equal(s.sugerida,false);});
test('al cambiar foto conserva el toque; si no fue tocada puede volver a sugerir',()=>{const humana=r.nuevoApoyoPies(r.tocarApoyoPies(r.nuevoApoyoPies(),false));assert.equal(r.proponerApoyoPies(humana,true,'ia').marcada,false);const automatica=r.nuevoApoyoPies();assert.equal(r.proponerApoyoPies(automatica,true,'ia').marcada,true);assert.equal(r.nuevoApoyoPies().tocada,false);});
test('umbrales están nombrados y usan la visibilidad de PERFIL',()=>{assert.equal(ia.APOYO_PIES.DIFERENCIA_RELATIVA_MINIMA,.15);assert.equal(ia.APOYO_PIES.ALTURA_CADERA_TOBILLOS_MINIMA,.22);assert.equal(ia.PERFIL.visibilidad,.5);assert.ok(Object.isFrozen(ia.APOYO_PIES));});

test('S3 con los cuatro landmarks observados en MediaPipe no se confunde con una foto sentada',()=>{const p=puntos();for(const [i,y,v] of [[23,.4825,1],[24,.4834,1],[27,.6414,.86],[28,.7927,1]]){p[i].y=y;p[i].visibility=v;}assert.equal(ia.sugerirPiesSinApoyo(p,{contexto:'PLANTA'}),true);});
