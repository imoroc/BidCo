// Genera el SQL que puebla la base con el catálogo de demostración, respetando las reglas de la aplicación:
//  - quien lidera una subasta tiene su importe retenido del saldo (a los superados se les devuelve);
//  - en una subasta a ciegas solo se retiene la puja más alta y el precio no se actualiza hasta el cierre;
//  - crear una subasta da 50 puntos y participar en un sorteo gasta los puntos aportados.
import { generadorAleatorio, literal, slug } from './utiles.mjs';

const PUNTOS_POR_CREAR = 50;
const SALDO_BASE = 3000;
const PUNTOS_BASE = 1500;
const ZONA = 'Europe/Madrid';

/** Importe con dos decimales exactos (el saldo es DECIMAL(10,2)). */
const euros = (n) => Math.round(n * 100) / 100;

function elegir(azar, lista) {
  return lista[Math.floor(azar() * lista.length)];
}

/** Reparte las fechas de las pujas entre el inicio de la subasta y hoy, sin ir hacia atrás. */
function diasDePuja(diasDesdeInicio, cantidad, indice) {
  return Math.max(0, diasDesdeInicio - Math.floor(((indice + 1) * diasDesdeInicio) / (cantidad + 1)));
}

function pujasNormal(azar, usuarios, creador, subasta, diasDesdeInicio) {
  const cantidad = azar() < 0.2 ? 0 : 1 + Math.floor(azar() * 7);
  const pujas = [];
  let actual = subasta.precio;
  let anterior = null;
  for (let i = 0; i < cantidad; i++) {
    const candidatos = usuarios.filter((u) => u !== creador && u !== anterior);
    const pujador = elegir(azar, candidatos);
    actual = Math.max(actual + 1, Math.ceil(actual * (1.04 + azar() * 0.12)));
    pujas.push({ pujador, importe: euros(actual), dias: diasDePuja(diasDesdeInicio, cantidad, i) });
    anterior = pujador;
  }
  return pujas;
}

function pujasCiega(azar, usuarios, creador, subasta, diasDesdeInicio) {
  const cantidad = azar() < 0.15 ? 0 : 1 + Math.floor(azar() * 5);
  const candidatos = usuarios.filter((u) => u !== creador);
  const pujas = [];
  const usados = new Set();
  const importes = new Set();
  for (let i = 0; i < cantidad; i++) {
    const libres = candidatos.filter((u) => !usados.has(u));
    const pujador = elegir(azar, libres);
    usados.add(pujador);
    let importe;
    do {
      importe = Math.ceil(subasta.precio * (1.1 + azar() * 0.9));
    } while (importes.has(importe));
    importes.add(importe);
    pujas.push({ pujador, importe: euros(importe), dias: diasDePuja(diasDesdeInicio, cantidad, i) });
  }
  return pujas;
}

function bloqueSubasta(ctx, subasta, indice) {
  const { azar, usuarios, imagenes, diasDeVida, saldos, puntos } = ctx;
  const normal = subasta.tipo === 'normal';
  const creador = usuarios[indice % usuarios.length];
  const diasDesdeInicio = 3 + Math.floor(azar() * 55);
  const pujas = normal
    ? pujasNormal(azar, usuarios, creador, subasta, diasDesdeInicio)
    : pujasCiega(azar, usuarios, creador, subasta, diasDesdeInicio);

  const lider = pujas.length ? pujas.reduce((a, b) => (b.importe > a.importe ? b : a)) : null;
  const precioFinal = normal && lider ? lider.importe : subasta.precio;
  if (lider) {
    saldos.set(lider.pujador, saldos.get(lider.pujador) - lider.importe);
  }
  puntos.set(creador, puntos.get(creador) + PUNTOS_POR_CREAR);

  const idUsuario = (u) => `(SELECT usuario_id FROM usuario WHERE nombre_usuario = ${literal(u)})`;
  const lineas = [];
  lineas.push(`-- ${normal ? 'Subasta' : 'Subasta a ciegas'}: ${subasta.nombre}`);
  lineas.push('DO $$');
  lineas.push('DECLARE');
  lineas.push(`  v_hoy date := (now() AT TIME ZONE ${literal(ZONA)})::date;`);
  lineas.push(`  v_creador bigint := ${idUsuario(creador)};`);
  lineas.push('  v_subasta bigint;');
  lineas.push('BEGIN');
  lineas.push(`  IF EXISTS (SELECT 1 FROM subasta WHERE creador_id = v_creador AND nombre_articulo = ${literal(subasta.nombre)}) THEN`);
  lineas.push('    RETURN;');
  lineas.push('  END IF;');
  lineas.push('  INSERT INTO subasta (fecha_inicial, fecha_final, precio_inicial, precio_final, subasta_normal,');
  lineas.push('                       nombre_articulo, descripcion, imagen, creador_id, ganador_id, categoria)');
  lineas.push(`  VALUES (v_hoy - ${diasDesdeInicio}, now() + ${diasDeVida(azar)} * interval '1 day', ${subasta.precio}, ${precioFinal}, ${normal},`);
  lineas.push(`          ${literal(subasta.nombre)}, ${literal(subasta.descripcion)}, ${literal(imagenes[slug(subasta.nombre)].url)},`);
  lineas.push(`          v_creador, NULL, ${literal(subasta.categoria)})`);
  lineas.push('  RETURNING subasta_id INTO v_subasta;');
  if (pujas.length) {
    lineas.push('  INSERT INTO puja (fecha, importe, ganadora, usuario_id, subasta_id) VALUES');
    lineas.push(
      pujas.map((p) => `    (v_hoy - ${p.dias}, ${p.importe}, false, ${idUsuario(p.pujador)}, v_subasta)`).join(',\n') + ';'
    );
  }
  lineas.push(`  UPDATE usuario SET puntos = puntos + ${PUNTOS_POR_CREAR} WHERE usuario_id = v_creador;`);
  if (lider) {
    lineas.push(`  UPDATE usuario SET saldo = saldo - ${lider.importe} WHERE nombre_usuario = ${literal(lider.pujador)};`);
  }
  for (const c of subasta.comentarios ?? []) {
    lineas.push('  INSERT INTO comentario (comentario, precio_estimado, trabajador_id, subasta_id)');
    lineas.push(
      `  VALUES (${literal(c.texto)}, ${c.estimado}, (SELECT trabajador_id FROM trabajador WHERE nombre_usuario = ${literal(c.experto)}), v_subasta);`
    );
  }
  lineas.push('END');
  lineas.push('$$;');
  return lineas.join('\n');
}

function bloqueSorteo(ctx, sorteo) {
  const { azar, usuarios, imagenes, diasDeVida, puntos } = ctx;
  const diasDesdeInicio = 3 + Math.floor(azar() * 40);
  const cantidad = 2 + Math.floor(azar() * 5);
  const participantes = [...usuarios].sort(() => azar() - 0.5).slice(0, cantidad);
  const participaciones = participantes.map((pujador, i) => ({
    pujador,
    puntos: sorteo.puntos + Math.floor(azar() * 5) * 10,
    dias: diasDePuja(diasDesdeInicio, cantidad, i)
  }));
  for (const p of participaciones) {
    puntos.set(p.pujador, puntos.get(p.pujador) - p.puntos);
  }

  const idUsuario = (u) => `(SELECT usuario_id FROM usuario WHERE nombre_usuario = ${literal(u)})`;
  const lineas = [];
  lineas.push(`-- Sorteo: ${sorteo.nombre}`);
  lineas.push('DO $$');
  lineas.push('DECLARE');
  lineas.push(`  v_hoy date := (now() AT TIME ZONE ${literal(ZONA)})::date;`);
  lineas.push('  -- Los sorteos los crea la administración: se usa la cuenta de administración y, si no hay, un experto.');
  lineas.push('  v_trabajador bigint := COALESCE(');
  lineas.push('    (SELECT trabajador_id FROM trabajador WHERE rol_trabajador = FALSE ORDER BY trabajador_id LIMIT 1),');
  lineas.push("    (SELECT trabajador_id FROM trabajador WHERE nombre_usuario = 'experto_tecnologia'));");
  lineas.push('  v_sorteo bigint;');
  lineas.push('BEGIN');
  lineas.push(`  IF EXISTS (SELECT 1 FROM sorteo WHERE trabajador_id = v_trabajador AND nombre_articulo = ${literal(sorteo.nombre)}) THEN`);
  lineas.push('    RETURN;');
  lineas.push('  END IF;');
  lineas.push('  INSERT INTO sorteo (nombre_articulo, descripcion, fecha_inicio, fecha_fin, puntos_necesarios, puntos_finales,');
  lineas.push('                      trabajador_id, ganador_id, imagen, categoria)');
  lineas.push(`  VALUES (${literal(sorteo.nombre)}, ${literal(sorteo.descripcion)}, v_hoy - ${diasDesdeInicio},`);
  lineas.push(`          now() + ${diasDeVida(azar)} * interval '1 day', ${sorteo.puntos}, 0, v_trabajador, NULL,`);
  lineas.push(`          ${literal(imagenes[slug(sorteo.nombre)].url)}, ${literal(sorteo.categoria)})`);
  lineas.push('  RETURNING sorteo_id INTO v_sorteo;');
  lineas.push('  INSERT INTO puja_sorteo (fecha, puntos, usuario_id, sorteo_id) VALUES');
  lineas.push(
    participaciones.map((p) => `    (v_hoy - ${p.dias}, ${p.puntos}, ${idUsuario(p.pujador)}, v_sorteo)`).join(',\n') + ';'
  );
  for (const p of participaciones) {
    lineas.push(`  UPDATE usuario SET puntos = puntos - ${p.puntos} WHERE nombre_usuario = ${literal(p.pujador)};`);
  }
  lineas.push('END');
  lineas.push('$$;');
  return lineas.join('\n');
}

function bloqueUsuarios(usuarios) {
  const filas = usuarios.map(
    (u) =>
      `    (${literal(u.usuario)}, ${literal(`${u.usuario}@example.com`)}, ${literal('España')}, ${literal(u.ciudad)}, ` +
      `${literal(u.codigoPostal)}, ${literal(u.calle)}, ${u.piso}, ${literal(u.letra)})`
  );
  return [
    '-- Usuarios de demostración (correos @example.com, un dominio reservado que nunca recibe correo).',
    'INSERT INTO usuario (nombre_usuario, correo_electronico, password, saldo, puntos, pais, ciudad, codigo_postal,',
    '                     calle, numero_piso, letra_piso, correo_verificado)',
    `SELECT v.usuario, v.correo, crypt(:'clave_demo', gen_salt('bf', 10)), ${SALDO_BASE}, ${PUNTOS_BASE},`,
    '       v.pais, v.ciudad, v.cp, v.calle, v.piso, v.letra, TRUE',
    '  FROM (VALUES',
    filas.join(',\n'),
    '  ) AS v (usuario, correo, pais, ciudad, cp, calle, piso, letra)',
    'ON CONFLICT DO NOTHING;'
  ].join('\n');
}

function bloqueExpertos(expertos) {
  const filas = expertos.map((e) => `    (${literal(e.usuario)}, ${literal(`${e.usuario}@example.com`)})`);
  return [
    '-- Expertos de demostración (rol_trabajador = TRUE).',
    'INSERT INTO trabajador (nombre_usuario, correo_electronico, password, rol_trabajador)',
    "SELECT v.usuario, v.correo, crypt(:'clave_demo', gen_salt('bf', 10)), TRUE",
    '  FROM (VALUES',
    filas.join(',\n'),
    '  ) AS v (usuario, correo)',
    'ON CONFLICT DO NOTHING;'
  ].join('\n');
}

const CABECERA = `-- =====================================================================================================
-- Catálogo de demostración de BidCo. GENERADO por seed/sembrar.mjs: no lo edites a mano, regenéralo.
--
-- Uso (haz antes una copia de la base de datos):
--   Simulación (no guarda nada):  psql "$DATABASE_URL" -v clave_demo='<contraseña de al menos 10 caracteres>' -f sembrar.sql
--   Guardar los cambios:          añade -v confirmar=1
--
-- Es repetible: cada subasta o sorteo solo se crea si no existe ya (mismo creador y mismo nombre).
-- =====================================================================================================
\\set ON_ERROR_STOP on
\\if :{?clave_demo}
\\else
  \\echo 'Falta la contraseña de las cuentas de demostración: añade -v clave_demo=...'
  \\quit
\\endif

BEGIN;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
`;

const PIE = `
-- Resumen
SELECT 'subastas abiertas' AS concepto, count(*) AS total FROM subasta WHERE fecha_final > now()
UNION ALL SELECT 'sorteos abiertos', count(*) FROM sorteo WHERE fecha_fin > now()
UNION ALL SELECT 'usuarios', count(*) FROM usuario
UNION ALL SELECT 'pujas', count(*) FROM puja
UNION ALL SELECT 'comentarios de expertos', count(*) FROM comentario;

\\if :{?confirmar}
  COMMIT;
  \\echo 'Cambios guardados.'
\\else
  ROLLBACK;
  \\echo 'Simulación: no se ha guardado nada. Repite con -v confirmar=1 para guardar los cambios.'
\\endif
`;

/**
 * @param catalogo contenido de catalogo.json
 * @param imagenes mapa slug(nombre) -> { url } con la foto ya subida
 * @param opciones.aniosDeVida años que tardan en cerrar las subastas y sorteos (más un desfase de 0 a 364 días)
 */
export function generarSql(catalogo, imagenes, { aniosDeVida = 8 } = {}) {
  const faltan = [...catalogo.subastas, ...catalogo.sorteos].filter((a) => !imagenes[slug(a.nombre)]?.url);
  if (faltan.length) {
    throw new Error(`Faltan fotos de: ${faltan.map((a) => a.nombre).join(', ')}. Ejecuta antes "fotos".`);
  }
  const azar = generadorAleatorio(20261111);
  const nombres = catalogo.usuarios.map((u) => u.usuario);
  const ctx = {
    azar,
    usuarios: nombres,
    imagenes,
    diasDeVida: (rnd) => aniosDeVida * 365 + Math.floor(rnd() * 365),
    saldos: new Map(nombres.map((u) => [u, SALDO_BASE])),
    puntos: new Map(nombres.map((u) => [u, PUNTOS_BASE]))
  };

  const bloques = [bloqueUsuarios(catalogo.usuarios), bloqueExpertos(catalogo.expertos)];
  catalogo.subastas.forEach((s, i) => bloques.push(bloqueSubasta(ctx, s, i)));
  catalogo.sorteos.forEach((s) => bloques.push(bloqueSorteo(ctx, s)));

  // Comprobación previa: ningún usuario puede quedarse con saldo o puntos negativos.
  for (const [usuario, saldo] of ctx.saldos) {
    if (saldo < 0) throw new Error(`El saldo de ${usuario} quedaría negativo (${saldo}). Sube SALDO_BASE o baja los precios.`);
  }
  for (const [usuario, total] of ctx.puntos) {
    if (total < 0) throw new Error(`Los puntos de ${usuario} quedarían negativos (${total}). Sube PUNTOS_BASE.`);
  }
  return `${CABECERA}\n${bloques.join('\n\n')}\n${PIE}`;
}
