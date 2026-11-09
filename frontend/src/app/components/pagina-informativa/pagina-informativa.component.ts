import { ChangeDetectionStrategy, Component, Input, ViewEncapsulation } from '@angular/core';

import { HeaderComponent } from '../header/header.component';
import { FooterComponent } from '../footer/footer.component';

/** Marco común de las páginas de texto (contacto, términos, privacidad): cabecera, contenido y pie. */
@Component({
  selector: 'app-pagina-informativa',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [HeaderComponent, FooterComponent],
  encapsulation: ViewEncapsulation.None,
  template: `
    <app-header></app-header>
    <main class="pagina-info">
      <article class="pagina-info-contenido">
        <h1>{{ titulo }}</h1>
        @if (actualizacion) {
          <p class="pagina-info-fecha">Última actualización: {{ actualizacion }}</p>
        }
        <ng-content></ng-content>
      </article>
    </main>
    <app-footer></app-footer>
  `,
  styles: `
    .pagina-info {
      min-height: 60vh;
      padding: 32px 16px 48px;
      font-family: 'Montserrat', sans-serif;
    }

    .pagina-info-contenido {
      max-width: 780px;
      margin: 0 auto;
      padding: 32px;
      background: var(--color-superficie);
      border-radius: 12px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
      line-height: 1.65;
      color: var(--color-texto-fuerte);
    }

    .pagina-info-contenido h1 {
      margin: 0 0 4px;
      color: var(--color-marca);
      font-size: 1.9rem;
    }

    .pagina-info-contenido h2 {
      margin: 28px 0 8px;
      color: var(--color-marca);
      font-size: 1.15rem;
    }

    .pagina-info-contenido p,
    .pagina-info-contenido li {
      font-size: 0.97rem;
    }

    .pagina-info-contenido a {
      color: var(--color-marca);
      font-weight: 600;
    }

    .pagina-info-fecha {
      margin: 0 0 8px;
      color: var(--color-texto-suave);
      font-size: 0.85rem;
    }

    @media (max-width: 600px) {
      .pagina-info-contenido {
        padding: 20px 16px;
      }
    }
  `
})
export class PaginaInformativaComponent {
  @Input({ required: true }) titulo = '';
  @Input() actualizacion = '';
}
