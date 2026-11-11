#!/usr/bin/env node
// Puebla BidCo con un catálogo de demostración. Uso:
//   node seed/sembrar.mjs fotos    busca las fotos, las sube a Cloudinary y guarda salida/imagenes.json
//   node seed/sembrar.mjs sql      genera seed/salida/sembrar.sql a partir de catalogo.json y salida/imagenes.json
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { leerCredenciales, pausa, resolverFoto } from './fotos.mjs';
import { creditos, paginaRevision } from './informes.mjs';
import { generarSql } from './sql.mjs';
import { slug } from './utiles.mjs';

const AQUI = dirname(fileURLToPath(import.meta.url));
const SALIDA = join(AQUI, 'salida');

async function leerJson(ruta, porDefecto) {
  try {
    return JSON.parse(await readFile(ruta, 'utf8'));
  } catch (error) {
    if (error.code === 'ENOENT' && porDefecto !== undefined) {
      return porDefecto;
    }
    throw error;
  }
}

async function comandoSql() {
  const catalogo = await leerJson(join(AQUI, 'catalogo.json'));
  const imagenes = await leerJson(join(SALIDA, 'imagenes.json'), {});
  const sql = generarSql(catalogo, imagenes);
  await mkdir(SALIDA, { recursive: true });
  const destino = join(SALIDA, 'sembrar.sql');
  await writeFile(destino, sql, 'utf8');
  console.log(`SQL generado en ${destino}`);
}

async function comandoFotos() {
  const credenciales = leerCredenciales();
  const catalogo = await leerJson(join(AQUI, 'catalogo.json'));
  const articulos = [...catalogo.subastas, ...catalogo.sorteos];
  const fijadas = await leerJson(join(AQUI, 'fotos.json'), {});
  const rutaImagenes = join(SALIDA, 'imagenes.json');
  const imagenes = await leerJson(rutaImagenes, {});
  await mkdir(SALIDA, { recursive: true });

  const pendientes = articulos.filter((a) => !imagenes[slug(a.nombre)]);
  console.log(`${articulos.length - pendientes.length} fotos ya subidas, ${pendientes.length} pendientes`);
  let fallos = 0;
  for (const articulo of pendientes) {
    try {
      imagenes[slug(articulo.nombre)] = await resolverFoto(articulo, { credenciales, carpetaFotos: join(AQUI, 'fotos'), fijadas });
      await writeFile(rutaImagenes, JSON.stringify(imagenes, null, 2), 'utf8'); // se guarda cada una: se puede reanudar
      console.log(`✓ ${articulo.nombre}`);
    } catch (error) {
      fallos++;
      console.warn(`✗ ${articulo.nombre}: ${error.message}`);
    }
    await pausa(400);
  }
  await writeFile(join(SALIDA, 'revision.html'), paginaRevision(articulos, imagenes), 'utf8');
  await writeFile(join(SALIDA, 'creditos.md'), creditos(articulos, imagenes), 'utf8');
  console.log(`Revisa las fotos en ${join(SALIDA, 'revision.html')}${fallos ? `\nHan fallado ${fallos}: repite el comando o pon tu propia foto en seed/fotos/` : ''}`);
  if (fallos) process.exitCode = 1;
}

const comandos = { fotos: comandoFotos, sql: comandoSql };
const comando = comandos[process.argv[2]];
if (!comando) {
  console.error(`Uso: node seed/sembrar.mjs <${Object.keys(comandos).join('|')}>`);
  process.exit(1);
}
comando().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
