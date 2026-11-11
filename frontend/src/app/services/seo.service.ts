import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

import { serializarJsonLd } from '../utils/datos-estructurados';
import { DESCRIPCION_POR_DEFECTO, IMAGEN_POR_DEFECTO, urlCanonica } from '../utils/seo';

export interface DatosSeo {
  titulo: string;
  descripcion?: string;
  /** Ruta actual (con o sin consulta); se normaliza para el canonical. */
  url: string;
  /** Si es true, se pide a los buscadores que no indexen la página. */
  noindex?: boolean;
}

/** Datos propios de una ficha de producto, que sustituyen a los genéricos de la ruta. */
export interface DatosProducto {
  titulo: string;
  descripcion: string;
  /** URL absoluta de la imagen para Open Graph. */
  imagen: string;
  /** Objeto JSON-LD ya construido (Product/Offer). */
  datosEstructurados: Record<string, unknown>;
}

/** Mantiene sincronizadas las etiquetas meta de la cabecera con la ruta activa. */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly meta = inject(Meta);
  private readonly title = inject(Title);
  private readonly doc = inject(DOCUMENT);

  actualizar(datos: DatosSeo): void {
    const descripcion = datos.descripcion ?? DESCRIPCION_POR_DEFECTO;
    const url = urlCanonica(datos.url);

    this.meta.updateTag({ name: 'description', content: descripcion });
    this.meta.updateTag({ property: 'og:title', content: datos.titulo });
    this.meta.updateTag({ property: 'og:description', content: descripcion });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ name: 'twitter:title', content: datos.titulo });
    this.meta.updateTag({ name: 'twitter:description', content: descripcion });

    if (datos.noindex) {
      this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
    } else {
      this.meta.removeTag("name='robots'");
    }

    // Cada navegación parte de los datos genéricos: la ficha vuelve a fijar los suyos al cargar.
    this.fijarImagen(IMAGEN_POR_DEFECTO, 'summary');
    this.fijarDatosEstructurados(null);

    this.fijarCanonical(url);
  }

  /** Sustituye título, descripción, imagen y datos estructurados por los del producto mostrado. */
  fijarProducto(producto: DatosProducto): void {
    this.title.setTitle(producto.titulo);
    this.meta.updateTag({ name: 'description', content: producto.descripcion });
    this.meta.updateTag({ property: 'og:title', content: producto.titulo });
    this.meta.updateTag({ property: 'og:description', content: producto.descripcion });
    this.meta.updateTag({ name: 'twitter:title', content: producto.titulo });
    this.meta.updateTag({ name: 'twitter:description', content: producto.descripcion });
    this.fijarImagen(producto.imagen, 'summary_large_image');
    this.fijarDatosEstructurados(producto.datosEstructurados);
  }

  private fijarImagen(imagen: string, tarjeta: 'summary' | 'summary_large_image'): void {
    this.meta.updateTag({ property: 'og:image', content: imagen });
    this.meta.updateTag({ name: 'twitter:image', content: imagen });
    this.meta.updateTag({ name: 'twitter:card', content: tarjeta });
  }

  private fijarDatosEstructurados(datos: Record<string, unknown> | null): void {
    let script = this.doc.head.querySelector<HTMLScriptElement>('script[type="application/ld+json"]');
    if (!datos) {
      script?.remove();
      return;
    }
    if (!script) {
      script = this.doc.createElement('script');
      script.setAttribute('type', 'application/ld+json');
      this.doc.head.appendChild(script);
    }
    script.textContent = serializarJsonLd(datos);
  }

  private fijarCanonical(url: string): void {
    let enlace = this.doc.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!enlace) {
      enlace = this.doc.createElement('link');
      enlace.setAttribute('rel', 'canonical');
      this.doc.head.appendChild(enlace);
    }
    enlace.setAttribute('href', url);
  }
}
