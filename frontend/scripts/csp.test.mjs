import { test } from 'node:test';
import assert from 'node:assert/strict';

import { analizarIndex, comprobarCsp, hashSha256, leerCsp } from './csp.mjs';

const SCRIPT = "document.body.classList.add('listo')";

function vercel(scriptSrc, estilos = "'self' 'unsafe-inline'") {
  return {
    headers: [{
      source: '/(.*)',
      headers: [{
        key: 'Content-Security-Policy-Report-Only',
        value: `default-src 'self'; script-src ${scriptSrc}; style-src ${estilos}`
      }]
    }]
  };
}

test('el hash coincide con el formato de la CSP', () => {
  assert.match(hashSha256(SCRIPT), /^'sha256-[A-Za-z0-9+/]+=*'$/);
});

test('ignora los scripts externos y los bloques JSON-LD', () => {
  const html = `<script src="main.js" type="module"></script>
    <script type="application/ld+json">{"@type":"Product"}</script>
    <script>${SCRIPT}</script>`;
  assert.deepEqual(analizarIndex(html).scripts, [SCRIPT]);
});

test('reconoce el cierre del script con espacios antes de >', () => {
  assert.deepEqual(analizarIndex(`<script>${SCRIPT}</script >`).scripts, [SCRIPT]);
});

test('detecta los manejadores en línea y los estilos en línea', () => {
  const html = '<link rel="stylesheet" href="a.css" onload="this.media=\'all\'"><div style="color:red"></div>';
  const r = analizarIndex(html);
  assert.deepEqual(r.manejadores, ['onload']);
  assert.equal(r.estilosEnLinea, true);
});

test('acepta un script en línea cuyo hash está en script-src', () => {
  const html = `<script>${SCRIPT}</script>`;
  assert.deepEqual(comprobarCsp(html, vercel(`'self' ${hashSha256(SCRIPT)}`)), []);
});

test('avisa si falta el hash del script en línea', () => {
  const html = `<script>${SCRIPT}</script>`;
  const problemas = comprobarCsp(html, vercel("'self'"));
  assert.equal(problemas.length, 1);
  assert.match(problemas[0], /Falta 'sha256-/);
});

test('avisa si un hash ya no corresponde a ningún script', () => {
  const html = '<script src="main.js"></script>';
  const problemas = comprobarCsp(html, vercel(`'self' ${hashSha256(SCRIPT)}`));
  assert.equal(problemas.length, 1);
  assert.match(problemas[0], /ya no corresponde/);
});

test('avisa de manejadores en línea', () => {
  const html = '<link rel="stylesheet" href="a.css" onload="x()">';
  assert.match(comprobarCsp(html, vercel("'self'"))[0], /manejadores en línea/);
});

test('avisa de estilos en línea si style-src no los permite', () => {
  const html = '<style>body{margin:0}</style>';
  assert.match(comprobarCsp(html, vercel("'self'", "'self'"))[0], /estilos en línea/);
  assert.deepEqual(comprobarCsp(html, vercel("'self'")), []);
});

test('falla si no hay cabecera de CSP', () => {
  assert.throws(() => leerCsp({ headers: [] }), /ninguna cabecera/);
});
