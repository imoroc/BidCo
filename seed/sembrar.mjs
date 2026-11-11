#!/usr/bin/env node
// Puebla BidCo con un catálogo de demostración. Uso:
//   node seed/sembrar.mjs sql      genera seed/salida/sembrar.sql a partir de catalogo.json y salida/imagenes.json
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { generarSql } from './sql.mjs';

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

const comandos = { sql: comandoSql };
const comando = comandos[process.argv[2]];
if (!comando) {
  console.error(`Uso: node seed/sembrar.mjs <${Object.keys(comandos).join('|')}>`);
  process.exit(1);
}
comando().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
