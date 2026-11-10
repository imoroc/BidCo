import { rutaCanonica, urlCanonica } from './seo';

describe('seo', () => {
  it('quita consulta, fragmento y barra final de la ruta', () => {
    expect(rutaCanonica('/search?q=reloj#arriba')).toBe('/search');
    expect(rutaCanonica('/ayuda/')).toBe('/ayuda');
  });

  it('conserva la raíz y añade la barra inicial si falta', () => {
    expect(rutaCanonica('/')).toBe('/');
    expect(rutaCanonica('terminos')).toBe('/terminos');
  });

  it('compone la URL absoluta del sitio', () => {
    expect(urlCanonica('/')).toBe('https://bid-co.vercel.app/');
    expect(urlCanonica('/auction/product/7?x=1')).toBe('https://bid-co.vercel.app/auction/product/7');
  });
});
