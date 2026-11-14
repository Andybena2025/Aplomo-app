// Pruebas sin dependencias: node --test scripts/qa_revision_eventos.cjs
const {test}=require('node:test');
const assert=require('node:assert/strict');
const crear=require('../src/main/resources/static/js/accion-evaluacion.js');
const reglas=require('../src/main/resources/static/js/revision-reglas.js');
test('dos toques durante cálculo solo ejecutan una petición y luego permite continuar',async()=>{
 let liberar,enviadas=0,cambios=[];
 const bloqueo=crear({alCambiar:v=>cambios.push(v)});
 const primera=bloqueo.ejecutar(async()=>{enviadas++;await new Promise(r=>liberar=r);});
 assert.equal(bloqueo.ocupada,true);
 assert.equal(await bloqueo.ejecutar(async()=>enviadas++),false);
 assert.equal(enviadas,1);liberar();assert.equal(await primera,true);
 assert.equal(bloqueo.ocupada,false);assert.deepEqual(cambios,[true,false]);
 assert.equal(await bloqueo.ejecutar(async()=>enviadas++),true);assert.equal(enviadas,2);
});
test('fallo de red libera Continuar y conserva la posibilidad de reintentar',async()=>{
 const errores=[],bloqueo=crear({alError:e=>errores.push(e.message)});
 assert.equal(await bloqueo.ejecutar(async()=>{throw new TypeError('Sin conexión');}),false);
 assert.equal(bloqueo.ocupada,false);assert.deepEqual(errores,['Sin conexión']);
 assert.equal(await bloqueo.ejecutar(async()=>{}),true);
});
test('el bloqueo también se libera si falla la actualización visual inicial',async()=>{
 const bloqueo=crear({alCambiar:v=>{if(v)throw new Error('Control no disponible');}});
 assert.equal(await bloqueo.ejecutar(async()=>{}),false);assert.equal(bloqueo.ocupada,false);
});
for(const [segmento,valor] of [['BRAZO',44],['ANTEBRAZO',61],['MUNECA',14],['CUELLO',19],['TRONCO',59]]){
 test(segmento+': menos de 3 grados del límite requiere revisión',()=>assert.equal(reglas.requiereRevision({segmento,grados:valor,encontrado:true}),true));
}
test('exactamente 3 grados no satisface la regla de menos de 3',()=>assert.equal(reglas.cercaDelBorde('BRAZO',42),false));
test('punto ausente y discrepancia requieren revisión; una decisión explícita resuelve',()=>{
 assert.equal(reglas.requiereRevision({segmento:'BRAZO',encontrado:false}),true);
 assert.equal(reglas.requiereRevision({segmento:'BRAZO',grados:70,encontrado:true,discrepa:true}),true);
 assert.equal(reglas.requiereRevision({segmento:'BRAZO',grados:70,encontrado:true,discrepa:true,resuelto:true}),false);
});
