import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cargarTemas, contraste, PARES } from './contraste.mjs';

const temas = cargarTemas();

for (const [nombre, tokens] of Object.entries(temas)) {
  for (const [texto, fondo, minimo, descripcion] of PARES) {
    test(`tema ${nombre}: ${descripcion} (${texto} sobre ${fondo})`, () => {
      assert.ok(tokens[texto], `Falta el token ${texto}`);
      assert.ok(tokens[fondo], `Falta el token ${fondo}`);
      const relacion = contraste(tokens[texto], tokens[fondo]);
      assert.ok(relacion !== null, `Los tokens deben ser colores #rrggbb (${tokens[texto]}, ${tokens[fondo]})`);
      assert.ok(relacion >= minimo, `Contraste ${relacion.toFixed(2)}:1, se exige al menos ${minimo}:1`);
    });
  }
}
