const {test}=require('node:test'),assert=require('node:assert/strict');
const guia=require('../src/main/resources/static/js/marcado-manual.js');
test('la primera instrucción destaca paso y hombro',()=>assert.equal(guia.instruccion(0),'Paso 1 de 5: toca el hombro'));
test('cada instrucción conserva el orden de las cinco marcas',()=>{for(let i=0;i<5;i++){assert.ok(guia.instruccion(i).startsWith(`Paso ${i+1} de 5: `));assert.ok(guia.instruccion(i).endsWith(guia.TEXTOS[i].toLowerCase()));}});
test('foto con su inicio visible no provoca desplazamiento',()=>{assert.equal(guia.fueraDeVista({top:100,bottom:500},800,80),false);assert.equal(guia.fueraDeVista({top:80,bottom:900},800,80),false);});
test('foto fuera por arriba o por abajo permite enfocarla',()=>{assert.equal(guia.fueraDeVista({top:-200,bottom:70},800,80),true);assert.equal(guia.fueraDeVista({top:-100,bottom:300},800,80),true);assert.equal(guia.fueraDeVista({top:900,bottom:1200},800,80),true);});
