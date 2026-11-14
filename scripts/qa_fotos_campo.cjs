// node --test scripts/qa_fotos_campo.cjs · mismas validaciones para subida y ejemplo.
const {test}=require('node:test'),assert=require('node:assert/strict');
const {File}=require('node:buffer'),fotos=require('../src/main/resources/static/js/fotos-campo.js');
const url='/static/img/galeria/S6_levantando_pieza_pesada_perfil.jpg';
const opciones=blob=>({Archivo:File,descargar:async()=>({ok:true,blob:async()=>blob})});
test('una subida válida conserva el mismo File',()=>{const f=new File(['foto'],'perfil.png',{type:'image/png'});assert.equal(fotos.validar(f,true,0),f);});
test('el ejemplo se convierte en File y entra por la validación común con sus bytes y tipo',async()=>{const blob=new Blob(['foto de ejemplo'],{type:'image/jpeg'});let recibido;await fotos.ejemplo(url,file=>{recibido=fotos.validar(file,true,0);},opciones(blob));assert.equal(recibido.name,'S6_levantando_pieza_pesada_perfil.jpg');assert.equal(recibido.type,'image/jpeg');assert.equal(recibido.size,blob.size);assert.equal(await recibido.text(),await blob.text());});
test('un ejemplo de 6 MB tampoco puede saltarse el tamaño',async()=>{await assert.rejects(fotos.ejemplo(url,file=>fotos.validar(file,true,0),opciones(new Blob([new Uint8Array(6*1024*1024)],{type:'image/jpeg'}))),/hasta 5 MB/);});
test('un PDF llamado jpg tampoco puede saltarse el tipo',async()=>{await assert.rejects(fotos.ejemplo(url,file=>fotos.validar(file,true,0),opciones(new Blob(['%PDF'],{type:'application/pdf'}))),/JPG o PNG/);});
test('el ejemplo exige permiso y respeta diez fotos',async()=>{const config=opciones(new Blob(['foto'],{type:'image/jpeg'}));await assert.rejects(fotos.ejemplo(url,file=>fotos.validar(file,false,0),config),/permiso/);await assert.rejects(fotos.ejemplo(url,file=>fotos.validar(file,true,10),config),/hasta 10/);});
test('si falla la descarga no añade foto',async()=>{let llamadas=0;await assert.rejects(fotos.ejemplo(url,()=>llamadas++,{Archivo:File,descargar:async()=>({ok:false})}));assert.equal(llamadas,0);});
test('solo acepta rutas del catálogo local',async()=>{let llamadas=0;await assert.rejects(fotos.ejemplo('https://otra.example/foto.jpg',()=>{}, {descargar:async()=>llamadas++}));assert.equal(llamadas,0);});
