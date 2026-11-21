'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
const {alternar} = require('../src/main/resources/static/js/mostrar-clave.js');
test('Mostrar y Ocultar cambian solo la visibilidad, con etiqueta y estado accesibles', () => {
  const campo = {type:'password',value:'EjemploSoloPrueba8'};
  const atributos = {};
  const boton = {setAttribute:(k,v)=>{atributos[k]=v;}};
  alternar(campo, boton);
  assert.equal(campo.type,'text'); assert.equal(atributos['aria-pressed'],'true');
  assert.equal(atributos['aria-label'],'Ocultar contraseña');
  alternar(campo, boton);
  assert.equal(campo.type,'password'); assert.equal(atributos['aria-pressed'],'false');
  assert.equal(campo.value,'EjemploSoloPrueba8');
});
