/** Datos de una subasta necesarios para describirla como producto con oferta (schema.org). */
export interface ProductoEstructurado {
  nombre: string;
  descripcion: string;
  /** URL absoluta de la imagen. */
  imagen: string;
  /** URL canónica de la ficha. */
  url: string;
  precio: number;
  /** Instante de cierre en ISO-8601; se omite si la fecha no es válida. */
  validaHasta?: string;
  vencida: boolean;
}

const MAX_DESCRIPCION = 500;

/** Descripción de una sola línea y de longitud acotada para los resultados enriquecidos. */
export function resumirDescripcion(texto: string): string {
  const limpia = (texto ?? '').replace(/\s+/g, ' ').trim();
  return limpia.length > MAX_DESCRIPCION ? `${limpia.slice(0, MAX_DESCRIPCION - 1).trimEnd()}…` : limpia;
}

/** Objeto JSON-LD Product/Offer de una subasta, listo para serializar. */
export function jsonLdProducto(p: ProductoEstructurado): Record<string, unknown> {
  return {
    '@context': 'https://schema.org',
    '@type': 'Product',
    name: p.nombre,
    description: resumirDescripcion(p.descripcion),
    image: p.imagen,
    url: p.url,
    offers: {
      '@type': 'Offer',
      url: p.url,
      price: p.precio.toFixed(2),
      priceCurrency: 'EUR',
      ...(p.validaHasta ? { priceValidUntil: p.validaHasta } : {}),
      availability: p.vencida ? 'https://schema.org/SoldOut' : 'https://schema.org/InStock',
      itemCondition: 'https://schema.org/UsedCondition'
    }
  };
}

/**
 * Serializa a JSON seguro para incrustar dentro de <script type="application/ld+json">: el texto lo
 * escriben los usuarios, así que se escapan los caracteres que permitirían cerrar la etiqueta.
 */
export function serializarJsonLd(datos: unknown): string {
  return JSON.stringify(datos)
    .replace(/</g, '\\u003c')
    .replace(/>/g, '\\u003e')
    .replace(/&/g, '\\u0026')
    .replace(/\u2028/g, '\\u2028')
    .replace(/\u2029/g, '\\u2029');
}
