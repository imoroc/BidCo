// Informes de las fotos elegidas: una página para revisarlas de un vistazo y la lista de créditos.
import { slug } from './utiles.mjs';

const escapar = (texto) =>
  String(texto ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

function conFoto(articulos, imagenes) {
  return articulos.filter((a) => imagenes[slug(a.nombre)]?.url).map((a) => ({ articulo: a, foto: imagenes[slug(a.nombre)] }));
}

/** Página con todas las fotos y su origen. Para cambiar una: seed/fotos/<slug>.jpg o seed/fotos.json. */
export function paginaRevision(articulos, imagenes) {
  const tarjetas = conFoto(articulos, imagenes)
    .map(({ articulo, foto }) => `
    <figure>
      <img src="${escapar(foto.url)}" alt="${escapar(articulo.nombre)}" loading="lazy">
      <figcaption>
        <strong>${escapar(articulo.nombre)}</strong><br>
        <code>${escapar(slug(articulo.nombre))}</code><br>
        ${escapar(foto.origen)}${foto.licencia ? ` · ${escapar(foto.licencia)}` : ''}
        ${foto.pagina ? `<br><a href="${escapar(foto.pagina)}">origen</a>` : ''}
      </figcaption>
    </figure>`)
    .join('\n');
  return `<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>Fotos del catálogo de demostración</title>
<style>
  body { font-family: system-ui, sans-serif; margin: 24px; }
  .rejilla { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
  figure { margin: 0; }
  img { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 8px; background: #eee; }
  figcaption { font-size: 13px; margin-top: 6px; }
</style></head>
<body><h1>Fotos del catálogo de demostración</h1>
<p>Para cambiar una foto: guarda tu archivo como <code>seed/fotos/&lt;slug&gt;.jpg</code> (o fija una URL en <code>seed/fotos.json</code>), borra su entrada de <code>imagenes.json</code> y vuelve a lanzar <code>fotos</code>.</p>
<div class="rejilla">${tarjetas}
</div></body></html>
`;
}

/** Créditos de las fotos de Commons (las licencias CC BY exigen atribución). */
export function creditos(articulos, imagenes) {
  const lineas = conFoto(articulos, imagenes)
    .filter(({ foto }) => foto.origen === 'Wikimedia Commons')
    .map(({ articulo, foto }) => `- ${articulo.nombre}: «${foto.titulo}», de ${foto.autor} (${foto.licencia}). ${foto.pagina}`);
  return `# Créditos de las fotos de demostración\n\nFotos de Wikimedia Commons con licencias libres:\n\n${lineas.join('\n')}\n`;
}
