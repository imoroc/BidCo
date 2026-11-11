// Servidor estático mínimo para las pruebas e2e: sirve dist/frontend/browser y, como Vercel,
// devuelve index.html en las rutas de la aplicación (las que no tienen extensión).
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { extname, join, normalize, resolve } from 'node:path';

const RAIZ = resolve('dist/frontend/browser');
const PUERTO = Number(process.argv[2] ?? 4300);
const TIPOS = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json',
  '.webmanifest': 'application/manifest+json',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.txt': 'text/plain; charset=utf-8'
};

async function leer(ruta) {
  try {
    return await readFile(ruta);
  } catch {
    return null;
  }
}

createServer(async (peticion, respuesta) => {
  let ruta;
  try {
    const { pathname } = new URL(peticion.url ?? '/', 'http://localhost');
    ruta = join(RAIZ, normalize(decodeURIComponent(pathname)));
  } catch {
    respuesta.writeHead(400).end();
    return;
  }
  if (!ruta.startsWith(RAIZ)) {
    respuesta.writeHead(403).end();
    return;
  }
  const extension = extname(ruta);
  const contenido = extension ? await leer(ruta) : await leer(join(RAIZ, 'index.html'));
  if (!contenido) {
    respuesta.writeHead(404).end();
    return;
  }
  respuesta.writeHead(200, { 'Content-Type': TIPOS[extension] ?? 'text/html; charset=utf-8' }).end(contenido);
}).listen(PUERTO, () => console.log(`Sirviendo ${RAIZ} en http://localhost:${PUERTO}`));
