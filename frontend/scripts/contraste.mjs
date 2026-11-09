// Utilidades de contraste WCAG 2.x sobre los tokens de src/styles.css.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const ruta = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'styles.css');

function bloque(css, inicio) {
  const i = css.indexOf(inicio);
  if (i < 0) throw new Error(`No se encontró el bloque: ${inicio}`);
  const abre = css.indexOf('{', i);
  let nivel = 0;
  for (let j = abre; j < css.length; j++) {
    if (css[j] === '{') nivel++;
    if (css[j] === '}' && --nivel === 0) return css.slice(abre + 1, j);
  }
  throw new Error(`Bloque sin cerrar: ${inicio}`);
}

function variables(texto) {
  const mapa = {};
  for (const m of texto.matchAll(/--([\w-]+):\s*([^;]+);/g)) mapa[m[1]] = m[2].trim();
  return mapa;
}

export function cargarTemas() {
  const css = readFileSync(ruta, 'utf8');
  const claro = variables(bloque(css, ':root {'));
  const oscuroBloque = bloque(css, '@media (prefers-color-scheme: dark)');
  const oscuro = { ...claro, ...variables(oscuroBloque) };
  return { claro, oscuro };
}

function aRgb(valor) {
  const hex = /^#([0-9a-f]{6})$/i.exec(valor);
  if (!hex) return null;
  const n = parseInt(hex[1], 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

function luminancia([r, g, b]) {
  const f = (c) => {
    const s = c / 255;
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
}

export function contraste(a, b) {
  const ca = aRgb(a);
  const cb = aRgb(b);
  if (!ca || !cb) return null;
  const [l1, l2] = [luminancia(ca), luminancia(cb)].sort((x, y) => y - x);
  return (l1 + 0.05) / (l2 + 0.05);
}

// [texto, fondo, mínimo, descripción]. 4,5 para texto normal; 3 para elementos de interfaz.
export const PARES = [
  ['color-texto', 'color-fondo', 4.5, 'texto sobre fondo'],
  ['color-texto', 'color-superficie', 4.5, 'texto sobre superficie'],
  ['color-texto-fuerte', 'color-superficie', 4.5, 'texto fuerte sobre superficie'],
  ['color-texto-medio', 'color-superficie', 4.5, 'texto medio sobre superficie'],
  ['color-texto-suave', 'color-superficie', 4.5, 'texto suave sobre superficie'],
  ['color-texto-suave', 'color-fondo', 4.5, 'texto suave sobre fondo'],
  ['color-texto-suave', 'color-superficie-alt', 4.5, 'texto suave sobre superficie alternativa'],
  ['color-marca', 'color-superficie', 4.5, 'enlaces y marca sobre superficie'],
  ['color-marca', 'color-fondo', 4.5, 'enlaces y marca sobre fondo'],
  ['color-sobre-marca', 'color-marca', 4.5, 'texto sobre botón de marca'],
  ['color-sobre-marca', 'color-marca-hover', 4.5, 'texto sobre botón de marca (hover)'],
  ['color-error', 'color-superficie', 4.5, 'error sobre superficie'],
  ['color-error-fuerte', 'color-error-fondo', 4.5, 'alerta de error'],
  ['color-exito', 'color-superficie', 3, 'borde de aviso de éxito sobre superficie'],
  ['color-exito-fuerte', 'color-exito-fondo', 4.5, 'alerta de éxito'],
  ['color-aviso', 'color-superficie', 4.5, 'aviso sobre superficie'],
  ['color-destacado-texto', 'color-superficie', 4.5, 'texto destacado sobre superficie'],
  ['color-destacado-texto', 'color-fondo', 4.5, 'texto destacado sobre fondo'],
  ['color-sobre-destacado', 'color-destacado', 4.5, 'texto sobre fondo destacado'],
  ['color-texto', 'color-info-fondo', 4.5, 'texto sobre fondo informativo'],
  ['color-texto', 'color-realce', 4.5, 'texto sobre fondo resaltado'],
  ['color-borde-fuerte', 'color-superficie', 3, 'borde de campo sobre superficie'],
];
