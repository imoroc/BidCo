// Busca una foto libre de cada artículo en Wikimedia Commons, la descarga y la sube a Cloudinary.
// Prioridad por artículo: 1) seed/fotos/<slug>.jpg|png|webp  2) URL fijada en seed/fotos.json  3) Commons.
import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { slug } from './utiles.mjs';

const COMMONS = process.env.COMMONS_API ?? 'https://commons.wikimedia.org/w/api.php';
const CLOUDINARY = process.env.CLOUDINARY_API ?? 'https://api.cloudinary.com/v1_1';
// Wikimedia pide identificar a quien consulta su API.
const AGENTE = 'BidCo-seed/1.0 (https://github.com/imoroc/BidCo; demo de portfolio)';
const LICENCIAS_LIBRES = /^(cc0|public domain|pd\b|cc[ -]by([ -]sa)?[ -][0-9.]+)/i;
const TITULOS_DESCARTADOS = /logo|icon|diagram|map\b|drawing|poster|screenshot|stamp|sticker|advert|vector|illustration|painting|engraving|scan|book|catalog/i;
const TIPOS = { '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.png': 'image/png', '.webp': 'image/webp' };

const sinEtiquetas = (html) => String(html ?? '').replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim();
const pausa = (ms) => new Promise((resolver) => setTimeout(resolver, ms));

export function leerCredenciales(entorno = process.env) {
  const credenciales = {
    nube: entorno.CLOUDINARY_CLOUD_NAME,
    clave: entorno.CLOUDINARY_API_KEY,
    secreto: entorno.CLOUDINARY_API_SECRET
  };
  const faltan = Object.entries({ CLOUDINARY_CLOUD_NAME: credenciales.nube, CLOUDINARY_API_KEY: credenciales.clave, CLOUDINARY_API_SECRET: credenciales.secreto })
    .filter(([, valor]) => !valor)
    .map(([nombre]) => nombre);
  if (faltan.length) {
    throw new Error(`Faltan variables de entorno: ${faltan.join(', ')}`);
  }
  return credenciales;
}

/** Candidatas de Commons para una búsqueda, de más a menos relevante, ya filtradas por tipo, tamaño y licencia. */
export async function buscarEnCommons(consulta, { solicitar = fetch } = {}) {
  const parametros = new URLSearchParams({
    action: 'query', format: 'json', origin: '*', generator: 'search', gsrnamespace: '6', gsrlimit: '25',
    gsrsearch: `${consulta} filetype:bitmap`, prop: 'imageinfo', iiprop: 'url|size|mime|extmetadata',
    iiurlwidth: '1600', iiextmetadatafilter: 'LicenseShortName|Artist'
  });
  const respuesta = await solicitar(`${COMMONS}?${parametros}`, { headers: { 'User-Agent': AGENTE } });
  if (!respuesta.ok) {
    throw new Error(`Commons respondió ${respuesta.status}`);
  }
  const datos = await respuesta.json();
  return Object.values(datos.query?.pages ?? {})
    .sort((a, b) => a.index - b.index)
    .map((pagina) => {
      const info = pagina.imageinfo?.[0];
      const meta = info?.extmetadata ?? {};
      return info && {
        titulo: pagina.title.replace(/^File:/, ''),
        pagina: info.descriptionurl,
        descarga: info.thumburl ?? info.url,
        mime: info.mime,
        ancho: info.width,
        alto: info.height,
        licencia: sinEtiquetas(meta.LicenseShortName?.value),
        autor: sinEtiquetas(meta.Artist?.value) || 'Autor desconocido'
      };
    })
    .filter((c) => c
      && ['image/jpeg', 'image/png'].includes(c.mime)
      && c.ancho >= 1000 && c.alto >= 700
      && c.ancho / c.alto >= 0.7 && c.ancho / c.alto <= 1.8
      && LICENCIAS_LIBRES.test(c.licencia)
      && !TITULOS_DESCARTADOS.test(c.titulo));
}

async function descargar(url, solicitar) {
  const respuesta = await solicitar(url, { headers: { 'User-Agent': AGENTE } });
  if (!respuesta.ok) {
    throw new Error(`No se pudo descargar la foto (${respuesta.status})`);
  }
  return { bytes: Buffer.from(await respuesta.arrayBuffer()), mime: respuesta.headers.get('content-type') ?? 'image/jpeg' };
}

/** Sube la foto a Cloudinary con firma. Se limita a 1600 px al recibirla; al servirla ya se pide f_auto,q_auto. */
export async function subirACloudinary(identificador, foto, credenciales, { solicitar = fetch } = {}) {
  const parametros = {
    invalidate: 'true',
    overwrite: 'true',
    public_id: `bidco-demo/${identificador}`,
    timestamp: String(Math.floor(Date.now() / 1000)),
    transformation: 'c_limit,h_1600,w_1600'
  };
  const cadena = Object.keys(parametros).sort().map((k) => `${k}=${parametros[k]}`).join('&');
  const firma = createHash('sha1').update(cadena + credenciales.secreto).digest('hex');

  const formulario = new FormData();
  Object.entries(parametros).forEach(([k, v]) => formulario.append(k, v));
  formulario.append('api_key', credenciales.clave);
  formulario.append('signature', firma);
  formulario.append('file', new Blob([foto.bytes], { type: foto.mime }), `${identificador}.jpg`);

  const respuesta = await solicitar(`${CLOUDINARY}/${credenciales.nube}/image/upload`, { method: 'POST', body: formulario });
  const json = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok || !json.secure_url) {
    throw new Error(`Cloudinary rechazó la foto: ${json.error?.message ?? respuesta.status}`);
  }
  return json.secure_url;
}

async function fotoLocal(carpeta, identificador) {
  for (const [extension, mime] of Object.entries(TIPOS)) {
    try {
      return { bytes: await readFile(join(carpeta, `${identificador}${extension}`)), mime };
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
  }
  return null;
}

/** Devuelve { url, titulo, autor, licencia, pagina, origen } de un artículo, subiendo su foto a Cloudinary. */
export async function resolverFoto(articulo, { credenciales, carpetaFotos, fijadas = {}, solicitar = fetch }) {
  const identificador = slug(articulo.nombre);

  const local = await fotoLocal(carpetaFotos, identificador);
  if (local) {
    const url = await subirACloudinary(identificador, local, credenciales, { solicitar });
    return { url, origen: 'archivo propio' };
  }
  if (fijadas[identificador]) {
    const url = await subirACloudinary(identificador, await descargar(fijadas[identificador], solicitar), credenciales, { solicitar });
    return { url, origen: 'URL fijada', pagina: fijadas[identificador] };
  }

  const candidatas = await buscarEnCommons(articulo.busqueda, { solicitar });
  if (!candidatas.length) {
    throw new Error(`Commons no tiene ninguna foto libre para "${articulo.busqueda}". Pon una en seed/fotos/${identificador}.jpg`);
  }
  const elegida = candidatas[0];
  const url = await subirACloudinary(identificador, await descargar(elegida.descarga, solicitar), credenciales, { solicitar });
  return { url, origen: 'Wikimedia Commons', titulo: elegida.titulo, autor: elegida.autor, licencia: elegida.licencia, pagina: elegida.pagina };
}

export { pausa };
