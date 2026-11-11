// Utilidades compartidas por el generador de SQL y la descarga de fotos.

/** Identificador estable de un artículo a partir de su nombre (sirve de nombre de archivo y de public_id). */
export function slug(texto) {
  return texto
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 60);
}

/** Literal SQL: texto entre comillas simples (duplicando las internas), número tal cual y NULL. */
export function literal(valor) {
  if (valor === null || valor === undefined) {
    return 'NULL';
  }
  if (typeof valor === 'number' || typeof valor === 'boolean') {
    return String(valor);
  }
  return `'${String(valor).replace(/'/g, "''")}'`;
}

/** Generador pseudoaleatorio con semilla (mulberry32): el mismo catálogo da siempre el mismo SQL. */
export function generadorAleatorio(semilla) {
  let estado = semilla >>> 0;
  return () => {
    estado = (estado + 0x6d2b79f5) >>> 0;
    let t = estado;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
