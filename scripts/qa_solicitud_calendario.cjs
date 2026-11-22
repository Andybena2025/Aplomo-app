'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict');
const {diasMes,finHora,iso}=require('../src/main/resources/static/js/solicitar-visita.js');
test('calendario lunes a domingo, mes correcto y días anteriores apagados',()=>{const d=diasMes(2026,10,'2026-11-21');assert.equal(d.length,36);assert.equal(d[0],null);assert.equal(d[6].dia,1);assert.equal(d.at(-1).fecha,'2026-11-30');assert.equal(d.find(x=>x?.dia===20).pasado,true);assert.equal(d.find(x=>x?.dia===21).pasado,false);assert.equal(d.find(x=>x?.dia===21).hoy,true);});
test('cambio de año y febrero bisiesto sin depender del reloj del navegador',()=>{assert.equal(iso(2026,12,1),'2027-01-01');assert.equal(diasMes(2028,1,'2028-02-01').filter(Boolean).length,29);assert.equal(diasMes(2027,1,'2027-02-01').filter(Boolean).length,28);});
test('chips de duración calculan fin sin perder media hora',()=>{assert.equal(finHora('08:30',2),'10:30');assert.equal(finHora('15:00',3),'18:00');});
