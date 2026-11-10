import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

/** Botón para añadir la página siguiente a un listado. No se muestra si no hay más resultados. */
@Component({
  selector: 'app-cargar-mas',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  template: `
    @if (hayMas) {
      <div class="cargar-mas">
        <button type="button" class="btn-secondary" [disabled]="cargando" (click)="cargarMas.emit()">
          {{ cargando ? 'Cargando…' : 'Cargar más' }}
        </button>
      </div>
    }
  `,
  styles: `
    :host { display: block; }

    .cargar-mas {
      display: flex;
      justify-content: center;
      padding: var(--espacio-5) var(--espacio-4) var(--espacio-6);
    }

    .cargar-mas button:disabled {
      cursor: progress;
      opacity: 0.7;
    }
  `
})
export class CargarMasComponent {
  @Input() hayMas = false;
  @Input() cargando = false;
  @Output() cargarMas = new EventEmitter<void>();
}
