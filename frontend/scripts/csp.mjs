// Comprueba que la CSP de vercel.json permite lo que el build de producción mete en index.html.
// Angular (Beasties) inserta un <script> en línea para activar el CSS no crítico: con `script-src 'self'`
// quedaría bloqueado y los estilos principales no se aplicarían. Se permite por su hash, y este script
// avisa si el hash deja de coincidir (por ejemplo, tras subir Angular).
import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');

export function hashSha256(texto) {
  return `'sha256-${createHash('sha256').update(texto, 'utf8').digest('base64')}'`;
}

/** Scripts en línea que se ejecutan (no los bloques de datos como JSON-LD) y manejadores `on...=`. */
export function analizarIndex(html) {
  const scripts = [];
  for (const m of html.matchAll(/<script(?<atributos>[^>]*)>(?<cuerpo>[\s\S]*?)<\/script\b[^>]*>/gi)) {
    const { atributos, cuerpo } = m.groups;
    if (/\bsrc\s*=/i.test(atributos) || /type\s*=\s*["']application\/ld\+json["']/i.test(atributos)) continue;
    scripts.push(cuerpo);
  }
  const manejadores = [...html.matchAll(/<[a-z][^>]*\s(on[a-z]+)\s*=/gi)].map(m => m[1]);
  const estilosEnLinea = /<style[\s>]/i.test(html) || /\sstyle\s*=/i.test(html);
  return { scripts, manejadores, estilosEnLinea };
}

/** Devuelve la lista de directivas de la CSP (enforced o Report-Only) de vercel.json. */
export function leerCsp(vercel) {
  for (const regla of vercel.headers ?? []) {
    for (const cabecera of regla.headers ?? []) {
      if (/^content-security-policy(-report-only)?$/i.test(cabecera.key)) {
        const directivas = {};
        for (const parte of cabecera.value.split(';')) {
          const [nombre, ...valores] = parte.trim().split(/\s+/);
          if (nombre) directivas[nombre.toLowerCase()] = valores;
        }
        return directivas;
      }
    }
  }
  throw new Error('vercel.json no tiene ninguna cabecera Content-Security-Policy');
}

/** Lista de problemas (vacía si la CSP encaja con el index.html). */
export function comprobarCsp(html, vercel) {
  const csp = leerCsp(vercel);
  const { scripts, manejadores, estilosEnLinea } = analizarIndex(html);
  const permitidosScript = csp['script-src'] ?? csp['default-src'] ?? [];
  const problemas = [];

  for (const cuerpo of scripts) {
    const hash = hashSha256(cuerpo);
    if (!permitidosScript.includes(hash) && !permitidosScript.includes("'unsafe-inline'")) {
      problemas.push(`Falta ${hash} en script-src para el script en línea: ${cuerpo.trim().slice(0, 80)}…`);
    }
  }
  const hashesSobrantes = permitidosScript.filter(t => t.startsWith("'sha256-") && !scripts.some(s => hashSha256(s) === t));
  for (const hash of hashesSobrantes) {
    problemas.push(`El hash ${hash} de script-src ya no corresponde a ningún script del index.html`);
  }
  if (manejadores.length > 0) {
    problemas.push(`El index.html tiene manejadores en línea (${manejadores.join(', ')}), que script-src bloquea`);
  }
  if (estilosEnLinea) {
    const estilos = csp['style-src'] ?? csp['default-src'] ?? [];
    if (!estilos.includes("'unsafe-inline'")) {
      problemas.push("El index.html tiene estilos en línea y style-src no incluye 'unsafe-inline'");
    }
  }
  return problemas;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const html = readFileSync(join(raiz, 'dist', 'frontend', 'browser', 'index.html'), 'utf8');
  const vercel = JSON.parse(readFileSync(join(raiz, 'vercel.json'), 'utf8'));
  const problemas = comprobarCsp(html, vercel);
  if (problemas.length > 0) {
    console.error('La CSP de vercel.json no encaja con el index.html del build:');
    problemas.forEach(p => console.error(`  - ${p}`));
    process.exit(1);
  }
  console.log('CSP correcta: los scripts en línea del index.html están permitidos por hash.');
}
