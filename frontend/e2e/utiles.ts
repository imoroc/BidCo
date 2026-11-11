import type { Page } from '@playwright/test';

const enDias = (dias: number) => new Date(Date.now() + dias * 86_400_000).toISOString();

export const SUBASTAS = [
  { subastaID: 1, nombreArticulo: 'Reloj de bolsillo antiguo', precioFinal: '120.00', categoria: 'moda', subastaNormal: true, fechaFinal: enDias(3) },
  { subastaID: 2, nombreArticulo: 'Guitarra eléctrica de colección', precioFinal: '450.00', categoria: 'tecnologia', subastaNormal: true, fechaFinal: enDias(5) }
];

export const RUTAS_PUBLICAS: ReadonlyArray<{ ruta: string; titulo: string }> = [
  { ruta: '/auction', titulo: 'Subastas · BidCo' },
  { ruta: '/blind-auctions', titulo: 'Subastas a ciegas · BidCo' },
  { ruta: '/raffle', titulo: 'Sorteos · BidCo' },
  { ruta: '/search', titulo: 'Búsqueda · BidCo' },
  { ruta: '/login', titulo: 'Iniciar sesión · BidCo' },
  { ruta: '/register', titulo: 'Crear cuenta · BidCo' },
  { ruta: '/ayuda', titulo: 'Ayuda · BidCo' },
  { ruta: '/contacto', titulo: 'Contacto · BidCo' },
  { ruta: '/terminos', titulo: 'Términos de uso · BidCo' },
  { ruta: '/privacidad', titulo: 'Política de privacidad · BidCo' }
];

/** Responde a la API con JSON. La cabecera Date alimenta el reloj del servidor, como en producción. */
export async function responderApi(page: Page, coincide: (url: URL) => boolean, cuerpo: unknown, status = 200): Promise<void> {
  await page.route(coincide, ruta =>
    ruta.fulfill({ status, contentType: 'application/json', headers: { Date: new Date().toUTCString() }, body: JSON.stringify(cuerpo) })
  );
}

/**
 * Deja la página sin red real: lo externo se corta, la API devuelve listas vacías y los
 * listados devuelven dos subastas de ejemplo. Las rutas registradas después tienen prioridad.
 */
export async function simularApi(page: Page): Promise<void> {
  await page.route(url => url.hostname !== 'localhost', ruta => ruta.abort());
  await responderApi(page, url => url.pathname.startsWith('/api/'), []);
  await responderApi(page, url => url.pathname.startsWith('/api/subastas/filtro-'), SUBASTAS);
}
