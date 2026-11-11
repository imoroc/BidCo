import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

/** Aviso fijo cuando el navegador pierde la conexión; desaparece solo al recuperarla. */
@Component({
  selector: 'app-aviso-sin-conexion',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  host: {
    '(window:online)': 'sinConexion.set(false)',
    '(window:offline)': 'sinConexion.set(true)'
  },
  template: `
    @if (sinConexion()) {
      <div class="aviso-sin-conexion" role="status">
        Sin conexión. Puedes seguir navegando, pero las pujas y los precios se actualizarán al volver a conectarte.
      </div>
    }
  `,
  styles: `
    .aviso-sin-conexion {
      padding: 10px 16px;
      text-align: center;
      background: var(--color-superficie-alt);
      border-bottom: 1px solid var(--color-borde-suave);
      color: var(--color-texto-fuerte);
      font-size: 0.9rem;
      font-weight: 600;
    }
  `
})
export class AvisoSinConexionComponent {
  readonly sinConexion = signal(typeof navigator !== 'undefined' && navigator.onLine === false);
}
