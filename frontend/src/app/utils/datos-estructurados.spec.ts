import { jsonLdProducto, resumirDescripcion, serializarJsonLd } from './datos-estructurados';

const BASE = {
  nombre: 'Reloj de bolsillo',
  descripcion: 'Funciona bien.',
  imagen: 'https://res.cloudinary.com/demo/image/upload/reloj.jpg',
  url: 'https://bid-co.vercel.app/auction/product/7',
  precio: 25,
  validaHasta: '2026-12-01T22:59:00.000Z',
  vencida: false
};

describe('datos-estructurados', () => {
  it('describe la subasta abierta como producto en stock con su precio en euros', () => {
    const datos = jsonLdProducto(BASE) as any;
    expect(datos['@type']).toBe('Product');
    expect(datos.offers.price).toBe('25.00');
    expect(datos.offers.priceCurrency).toBe('EUR');
    expect(datos.offers.availability).toBe('https://schema.org/InStock');
    expect(datos.offers.priceValidUntil).toBe(BASE.validaHasta);
  });

  it('omite la validez de la oferta si no hay fecha', () => {
    const datos = jsonLdProducto({ ...BASE, validaHasta: undefined }) as any;
    expect('priceValidUntil' in datos.offers).toBeFalse();
  });

  it('marca como agotada la subasta vencida', () => {
    const datos = jsonLdProducto({ ...BASE, vencida: true }) as any;
    expect(datos.offers.availability).toBe('https://schema.org/SoldOut');
  });

  it('resume la descripción a una línea y la acota', () => {
    expect(resumirDescripcion('  uno\n\n dos   tres ')).toBe('uno dos tres');
    const larga = resumirDescripcion('a'.repeat(900));
    expect(larga.length).toBe(500);
    expect(larga.endsWith('…')).toBeTrue();
  });

  it('escapa lo que permitiría cerrar la etiqueta script', () => {
    const texto = serializarJsonLd({ nombre: '</script><img src=x onerror=alert(1)> & más\u2028' });
    expect(texto).not.toContain('<');
    expect(texto).not.toContain('>');
    expect(texto).not.toContain('&');
    expect(texto).not.toContain('\u2028');
    expect(JSON.parse(texto).nombre).toBe('</script><img src=x onerror=alert(1)> & más\u2028');
  });
});
