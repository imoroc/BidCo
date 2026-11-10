import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta } from '@angular/platform-browser';

import { DESCRIPCION_POR_DEFECTO, urlCanonica } from '../utils/seo';

export interface DatosSeo {
  titulo: string;
  descripcion?: string;
  /** Ruta actual (con o sin consulta); se normaliza para el canonical. */
  url: string;
  /** Si es true, se pide a los buscadores que no indexen la página. */
  noindex?: boolean;
}

/** Mantiene sincronizadas las etiquetas meta de la cabecera con la ruta activa. */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly meta = inject(Meta);
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

    this.fijarCanonical(url);
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
