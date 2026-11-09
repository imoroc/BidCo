import { ImageLoaderConfig } from '@angular/common';

const MARCA_SUBIDA = '/image/upload/';

/**
 * Loader de NgOptimizedImage para las imágenes alojadas en Cloudinary: pide el formato y la calidad
 * más adecuados (f_auto, q_auto) y el ancho necesario (w_N). Las URL que no son de Cloudinary
 * (recursos locales, vistas previas con blob:) se devuelven sin tocar.
 */
export function cloudinaryLoader(config: ImageLoaderConfig): string {
  const { src, width } = config;
  const posicion = src.indexOf(MARCA_SUBIDA);
  if (!src.includes('res.cloudinary.com') || posicion === -1) {
    return src;
  }
  const transformaciones = ['f_auto', 'q_auto', ...(width ? [`w_${width}`] : [])].join(',');
  const inicio = posicion + MARCA_SUBIDA.length;
  return `${src.slice(0, inicio)}${transformaciones}/${src.slice(inicio)}`;
}
