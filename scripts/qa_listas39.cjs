const {test}=require('node:test');
const assert=require('node:assert/strict');
const listas=require('../src/main/resources/static/js/sistema-visual.js');
const revision=require('../src/main/resources/static/js/revision-reglas.js');
test('el ángulo visible no muestra decimales ni transforma el valor original',()=>{
 for(const [entrada,salida] of [[23.52,'24°'],[18.1,'18°'],[-6.2,'-6°'],[null,'Sin medir']])
  assert.equal(revision.anguloTexto(entrada),salida);
 const fuente={grados:23.52};revision.anguloTexto(fuente.grados);assert.equal(fuente.grados,23.52);
});
test('un mes limita a cinco filas y conserva los conteos por nivel y los restantes',()=>{
 const filas=Array.from({length:9},(_,i)=>({puntaje:String(i<3?7:i<6?4:2),nivel:String(i<3?4:i<6?2:1)}));
 assert.deepEqual(listas.mes(filas,false),{total:9,restantes:4,alto:3,medio:3,bajo:3,sin:0});
 assert.equal(listas.mes(filas,true).restantes,0);
 assert.equal(listas.mes(filas.slice(0,2),false).total,2);
});
test('sin resultado no se inventa un riesgo bajo',()=>{
 assert.equal(listas.riesgo({puntaje:'',nivel:''}),'sin');
 assert.equal(listas.mes([{}],false).sin,1);
});
test('Ver por empresa incluye cada asignación y separa Administradores sin duplicar el registro original',()=>{
 const fila={rol:'EVALUADOR',empresa:'12 24'};
 assert.deepEqual(listas.claves(fila,'EMPRESA'),['12','24']);
 assert.deepEqual(listas.claves({rol:'ADMINISTRADOR'},'EMPRESA'),['TODAS']);
 assert.deepEqual(listas.claves({rol:'USUARIO',empresa:''},'EMPRESA'),['SIN_EMPRESA']);
 assert.equal(fila.empresa,'12 24');
});
test('Ver por evaluador y ciudad conserva múltiples asignaciones y el grupo sin evaluador',()=>{
 assert.deepEqual(listas.claves({evaluadores:'8,9'},'EVALUADOR'),['8','9']);
 assert.deepEqual(listas.claves({evaluadores:''},'EVALUADOR'),['SIN_ASIGNAR']);
 assert.deepEqual(listas.claves({ciudad:'Armenia, Calarcá'},'CIUDAD'),['Armenia','Calarcá']);
});

test('los indicadores cuentan también visitas en curso y respetan el conjunto filtrado',()=>{
 const filas=[{estado:'EN_CURSO',fecha:'2026-11-24',puntaje:'7',nivel:'4'},
  {estado:'FINALIZADA',fecha:'2026-11-20',puntaje:'4',nivel:'2'},
  {estado:'FINALIZADA',fecha:'2026-10-12',puntaje:'6',nivel:'3'}];
 assert.deepEqual(listas.conteos(filas,'2026-11-24'),{curso:1,mes:2,finalizadas:2,alto:2});
 assert.deepEqual(listas.conteos(filas.filter(f=>f.estado==='FINALIZADA'),'2026-11-24'),
  {curso:0,mes:1,finalizadas:2,alto:1});
});
