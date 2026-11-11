import { ImageLoaderConfig } from '@angular/common';

const HOST_CLOUDINARY = 'res.cloudinary.com';
const MARCA_SUBIDA = '/image/upload/';

/**
 * Loader de NgOptimizedImage para las imágenes alojadas en Cloudinary: pide el formato y la calidad
 * más adecuados (f_auto, q_auto) y el ancho necesario (w_N). Con `c_limit` solo se reduce: una foto
 * pequeña no se amplía (saldría borrosa y más pesada). Las URL que no son de Cloudinary
 * (recursos locales, vistas previas con blob:) se devuelven sin tocar.
 */
export function cloudinaryLoader(config: ImageLoaderConfig): string {
  const { src, width } = config;
  if (!esDeCloudinary(src)) {
    return src;
  }
  const inicio = src.indexOf(MARCA_SUBIDA) + MARCA_SUBIDA.length;
  const transformaciones = ['f_auto', 'q_auto', ...(width ? ['c_limit', `w_${width}`] : [])].join(',');
  return `${src.slice(0, inicio)}${transformaciones}/${src.slice(inicio)}`;
}

/** Comprueba el host exacto de la URL y que la ruta sea de una imagen subida. */
function esDeCloudinary(src: string): boolean {
  try {
    const url = new URL(src);
    return url.hostname === HOST_CLOUDINARY && url.pathname.includes(MARCA_SUBIDA);
  } catch {
    return false;
  }
}
