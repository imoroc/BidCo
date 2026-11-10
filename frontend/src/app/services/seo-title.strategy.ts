import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRouteSnapshot, RouterStateSnapshot, TitleStrategy } from '@angular/router';

import { SeoService } from './seo.service';

const TITULO_POR_DEFECTO = 'BidCo · Subastas y sorteos';

/** Además del título, actualiza descripción, Open Graph y canonical con los datos de la ruta. */
@Injectable({ providedIn: 'root' })
export class SeoTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly seo = inject(SeoService);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const titulo = this.buildTitle(snapshot) ?? TITULO_POR_DEFECTO;
    this.title.setTitle(titulo);

    let descripcion: string | undefined;
    let noindex = false;
    let ruta: ActivatedRouteSnapshot | undefined = snapshot.root;
    while (ruta) {
      descripcion = (ruta.data['descripcion'] as string | undefined) ?? descripcion;
      noindex = noindex || ruta.data['noindex'] === true;
      ruta = ruta.children.find(hijo => hijo.outlet === 'primary');
    }

    this.seo.actualizar({ titulo, descripcion, url: snapshot.url, noindex });
  }
}
