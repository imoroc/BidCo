import { cloudinaryLoader } from './cloudinary-loader';

describe('cloudinaryLoader', () => {
  const base = 'https://res.cloudinary.com/demo/image/upload/v1/producto.jpg';

  it('añade formato, calidad y ancho a las URL de Cloudinary', () => {
    expect(cloudinaryLoader({ src: base, width: 400 }))
      .toBe('https://res.cloudinary.com/demo/image/upload/f_auto,q_auto,w_400/v1/producto.jpg');
  });

  it('omite el ancho cuando no se indica', () => {
    expect(cloudinaryLoader({ src: base }))
      .toBe('https://res.cloudinary.com/demo/image/upload/f_auto,q_auto/v1/producto.jpg');
  });

  it('no modifica las URL que no son de Cloudinary', () => {
    expect(cloudinaryLoader({ src: 'assets/imagen.png', width: 400 })).toBe('assets/imagen.png');
    expect(cloudinaryLoader({ src: 'blob:http://localhost/abc', width: 400 })).toBe('blob:http://localhost/abc');
  });
});
