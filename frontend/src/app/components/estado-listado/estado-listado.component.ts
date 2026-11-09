import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/** Mensaje de estado de un listado: cargando, error o sin resultados. No muestra nada si hay datos. */
@Component({
  selector: 'app-estado-listado',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  template: `
    @if (cargando) {
      <p class="estado-listado" role="status">{{ mensajeCarga }}</p>
      <div class="esqueletos" aria-hidden="true">
        @for (n of esqueletos; track n) {
          <div class="esqueleto">
            <div class="esqueleto-foto"></div>
            <div class="esqueleto-linea"></div>
            <div class="esqueleto-linea corta"></div>
          </div>
        }
      </div>
    } @else if (errorCarga) {
      <p class="estado-listado estado-error" role="alert">No se han podido cargar los productos. Inténtalo de nuevo en unos instantes.</p>
    } @else if (total === 0) {
      <p class="estado-listado" role="status">{{ mensajeVacio }}</p>
    }
  `,
  styles: `
    .estado-listado {
      padding: 48px 16px;
      text-align: center;
      color: var(--color-texto-suave);
      font-size: 1rem;
    }

    .estado-error {
      color: var(--color-error-fuerte);
    }

    .esqueletos {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: var(--espacio-5);
      padding: 0 var(--espacio-4) var(--espacio-6);
    }

    .esqueleto {
      display: flex;
      flex-direction: column;
      gap: var(--espacio-3);
      padding-bottom: var(--espacio-4);
      overflow: hidden;
      background: var(--color-superficie);
      border: 1px solid var(--color-borde-suave);
      border-radius: var(--radio-md);
    }

    .esqueleto-foto,
    .esqueleto-linea {
      background: linear-gradient(90deg, var(--color-superficie-alt) 25%, var(--color-borde-suave) 50%, var(--color-superficie-alt) 75%);
      background-size: 200% 100%;
      animation: brillo 1.4s linear infinite;
    }

    .esqueleto-foto { aspect-ratio: 1 / 1; }
    .esqueleto-linea { height: 14px; margin: 0 var(--espacio-4); border-radius: var(--radio-sm); }
    .esqueleto-linea.corta { width: 40%; }

    @keyframes brillo { to { background-position: -200% 0; } }

    @media (prefers-reduced-motion: reduce) {
      .esqueleto-foto,
      .esqueleto-linea { animation: none; }
    }
  `
})
export class EstadoListadoComponent {
  @Input() cargando = false;
  @Input() mensajeCarga = 'Cargando… Si es la primera visita, el servidor puede tardar hasta un minuto en arrancar.';
  readonly esqueletos = [1, 2, 3, 4, 5, 6, 7, 8];
  @Input() errorCarga = false;
  @Input() total = 0;
  @Input() mensajeVacio = 'No hay resultados.';
}
