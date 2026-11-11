import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';

import { IMAGEN_POR_DEFECTO } from '../utils/seo';
import { SeoService } from './seo.service';

describe('SeoService', () => {
  let seo: SeoService;
  let doc: Document;

  const contenido = (selector: string) => doc.head.querySelector(selector)?.getAttribute('content');
  const script = () => doc.head.querySelector<HTMLScriptElement>('script[type="application/ld+json"]');

  beforeEach(() => {
    seo = TestBed.inject(SeoService);
    doc = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    script()?.remove();
    seo.actualizar({ titulo: 'BidCo', url: '/' });
  });

  it('fija la imagen y el JSON-LD de la ficha y los retira al cambiar de ruta', () => {
    seo.fijarProducto({
      titulo: 'Reloj · BidCo',
      descripcion: 'Un reloj.',
      imagen: 'https://res.cloudinary.com/demo/image/upload/w_1200/reloj.jpg',
      datosEstructurados: { '@type': 'Product', name: 'Reloj' }
    });
    expect(contenido('meta[property="og:image"]')).toContain('reloj.jpg');
    expect(contenido('meta[name="twitter:card"]')).toBe('summary_large_image');
    expect(JSON.parse(script()!.textContent!).name).toBe('Reloj');

    seo.actualizar({ titulo: 'Subastas · BidCo', url: '/auction' });
    expect(contenido('meta[property="og:image"]')).toBe(IMAGEN_POR_DEFECTO);
    expect(contenido('meta[name="twitter:card"]')).toBe('summary');
    expect(script()).toBeNull();
  });

  it('reutiliza la misma etiqueta de JSON-LD al cambiar de ficha', () => {
    const datos = (nombre: string) => ({ titulo: nombre, descripcion: nombre, imagen: 'https://x.test/a.jpg', datosEstructurados: { name: nombre } });
    seo.fijarProducto(datos('A'));
    seo.fijarProducto(datos('B'));
    expect(doc.head.querySelectorAll('script[type="application/ld+json"]').length).toBe(1);
    expect(JSON.parse(script()!.textContent!).name).toBe('B');
  });
});
