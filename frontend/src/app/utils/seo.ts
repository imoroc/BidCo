/** Dirección pública del sitio, base de las URL canónicas y de Open Graph. */
export const SITIO = 'https://bid-co.vercel.app';

export const DESCRIPCION_POR_DEFECTO = 'BidCo: plataforma de subastas normales y a ciegas y sorteos.';

/** Devuelve la ruta sin parámetros de consulta, sin fragmento y sin barra final (salvo la raíz). */
export function rutaCanonica(url: string): string {
  const limpia = url.split('#')[0].split('?')[0];
  const ruta = limpia.startsWith('/') ? limpia : `/${limpia}`;
  return ruta.length > 1 ? ruta.replace(/\/+$/, '') || '/' : ruta;
}

/** URL absoluta canónica de una ruta del sitio. */
export function urlCanonica(url: string): string {
  const ruta = rutaCanonica(url);
  return ruta === '/' ? `${SITIO}/` : `${SITIO}${ruta}`;
}
