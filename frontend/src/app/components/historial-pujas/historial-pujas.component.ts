import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, Input, OnChanges, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { ProductoService, PujaHistorialDto } from '../../services/product.service';

/** Últimas pujas de una subasta normal, con el pujador enmascarado. Se recarga cuando cambia el precio. */
@Component({
  selector: 'app-historial-pujas',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  template: `
    <section class="historial" aria-labelledby="historial-titulo">
      <h3 id="historial-titulo">Historial de pujas</h3>
      @if (errorCarga) {
        <p class="historial-nota" role="status">No se ha podido cargar el historial.</p>
      } @else if (cargado && pujas.length === 0) {
        <p class="historial-nota">Todavía no hay pujas. Sé el primero.</p>
      } @else if (pujas.length > 0) {
        <ol class="historial-lista">
          @for (puja of pujas; track $index) {
            <li class="historial-fila">
              <span class="historial-pujador">{{ puja.pujador }}</span>
              <span class="historial-fecha">{{ puja.fecha | date:'d MMM y' }}</span>
              <span class="historial-importe">{{ puja.importe | currency:'EUR':'symbol':'1.0-2' }}</span>
            </li>
          }
        </ol>
      }
    </section>
  `,
  styles: `
    :host { display: block; }

    .historial {
      margin-top: var(--espacio-5);
    }

    .historial h3 {
      margin-bottom: var(--espacio-3);
      color: var(--color-texto-fuerte);
    }

    .historial-nota {
      color: var(--color-texto-suave);
    }

    .historial-lista {
      margin: 0;
      padding: 0;
      list-style: none;
      border: 1px solid var(--color-borde);
      border-radius: var(--radio-md);
      background: var(--color-superficie);
    }

    .historial-fila {
      display: flex;
      gap: var(--espacio-3);
      align-items: baseline;
      padding: var(--espacio-3) var(--espacio-4);
    }

    .historial-fila + .historial-fila {
      border-top: 1px solid var(--color-borde-suave);
    }

    .historial-pujador {
      flex: 1;
      color: var(--color-texto);
    }

    .historial-fecha {
      color: var(--color-texto-suave);
      font-size: 0.875rem;
    }

    .historial-importe {
      min-width: 5rem;
      text-align: right;
      font-weight: 600;
      color: var(--color-texto-fuerte);
    }
  `
})
export class HistorialPujasComponent implements OnChanges {
  @Input({ required: true }) subastaId!: number;
  /** Se usa solo como señal: cada cambio de precio indica que hay una puja nueva que mostrar. */
  @Input() precioActual = 0;

  pujas: PujaHistorialDto[] = [];
  cargado = false;
  errorCarga = false;

  private readonly productos = inject(ProductoService);
  private peticion?: Subscription;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.peticion?.unsubscribe());
  }

  ngOnChanges(): void {
    this.peticion?.unsubscribe();
    this.peticion = this.productos.obtenerHistorialPujas(this.subastaId).subscribe({
      next: pujas => {
        this.pujas = pujas;
        this.cargado = true;
        this.errorCarga = false;
      },
      error: () => {
        // Si ya había datos se conservan; solo se avisa cuando no hay nada que mostrar.
        this.errorCarga = this.pujas.length === 0;
        this.cargado = true;
      }
    });
  }
}
