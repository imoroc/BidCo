import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PaginaInformativaComponent } from '../../components/pagina-informativa/pagina-informativa.component';

@Component({
  selector: 'app-no-encontrada',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [PaginaInformativaComponent, RouterLink],
  template: `
    <app-pagina-informativa titulo="Página no encontrada">
      <p>La dirección que has abierto no existe o ha cambiado de sitio.</p>
      <p><a routerLink="/auction">Volver a las subastas</a></p>
    </app-pagina-informativa>
  `
})
export class NoEncontradaComponent {}
