import { Component, Input } from '@angular/core';

/** Mensaje de estado de un listado: cargando, error o sin resultados. No muestra nada si hay datos. */
@Component({
  selector: 'app-estado-listado',
  standalone: true,
  template: `
    @if (cargando) {
      <p class="estado-listado" role="status">Cargando… Si es la primera visita, el servidor puede tardar hasta un minuto en arrancar.</p>
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
      color: #5f6368;
      font-size: 1rem;
    }

    .estado-error {
      color: #b3261e;
    }
  `
})
export class EstadoListadoComponent {
  @Input() cargando = false;
  @Input() errorCarga = false;
  @Input() total = 0;
  @Input() mensajeVacio = 'No hay resultados.';
}
