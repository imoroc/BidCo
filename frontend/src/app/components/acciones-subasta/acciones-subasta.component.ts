import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SubastaResponseDTO, SubastaService } from '../../services/auction.service';
import { estaVencida } from '../../utils/fechas';

type Modo = 'ninguno' | 'editando' | 'confirmando';

/**
 * Editar o cancelar una subasta propia desde «Tus subastas». La API solo lo permite mientras no tenga
 * pujas ni haya cerrado: el listado no sabe si hay pujas, así que el servidor decide y aquí se explica el motivo.
 */
@Component({
  selector: 'app-acciones-subasta',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule],
  template: `
    @if (!vencida) {
      @switch (modo) {
        @case ('ninguno') {
          <div class="acciones">
            <button type="button" class="btn-secondary" (click)="abrirEdicion()">Editar</button>
            <button type="button" class="btn-secondary peligro" (click)="modo = 'confirmando'">Cancelar subasta</button>
          </div>
        }
        @case ('confirmando') {
          <div class="panel" role="group" [attr.aria-label]="'Cancelar ' + subasta.nombreArticulo">
            <p>¿Cancelar esta subasta? Se retirará y no se puede deshacer. Solo es posible si aún no tiene pujas.</p>
            <div class="acciones">
              <button type="button" class="btn-primary peligro" [disabled]="enviando" (click)="cancelar()">Sí, cancelar</button>
              <button type="button" class="btn-secondary" [disabled]="enviando" (click)="cerrar()">No, mantener</button>
            </div>
          </div>
        }
        @case ('editando') {
          <form class="panel" (ngSubmit)="guardar()" [attr.aria-label]="'Editar ' + subasta.nombreArticulo">
            <div class="input-group">
              <label [for]="'nombre-' + subasta.subastaID">Nombre del producto</label>
              <input type="text" [id]="'nombre-' + subasta.subastaID" name="nombre" [(ngModel)]="nombre" maxlength="255" required>
            </div>
            <div class="input-group">
              <label [for]="'descripcion-' + subasta.subastaID">Descripción</label>
              <textarea [id]="'descripcion-' + subasta.subastaID" name="descripcion" rows="3" [(ngModel)]="descripcion" maxlength="5000" required></textarea>
            </div>
            <div class="input-group">
              <label [for]="'categoria-' + subasta.subastaID">Categoría</label>
              <select [id]="'categoria-' + subasta.subastaID" name="categoria" [(ngModel)]="categoria" required>
                @for (c of categorias; track c.valor) {
                  <option [value]="c.valor">{{ c.nombre }}</option>
                }
              </select>
            </div>
            <div class="acciones">
              <button type="submit" class="btn-primary" [disabled]="enviando || !valido()">{{ enviando ? 'Guardando…' : 'Guardar cambios' }}</button>
              <button type="button" class="btn-secondary" [disabled]="enviando" (click)="cerrar()">Cancelar edición</button>
            </div>
          </form>
        }
      }
      @if (error) {
        <p class="error" role="alert">{{ error }}</p>
      }
    }
  `,
  styles: `
    :host { display: block; margin-top: var(--espacio-3); }
    .acciones { display: flex; flex-wrap: wrap; gap: var(--espacio-2); }
    .panel { display: grid; gap: var(--espacio-3); padding: var(--espacio-3); border: 1px solid var(--color-borde-suave); border-radius: var(--radio-md); background: var(--color-superficie); }
    .panel p { margin: 0; }
    .peligro { --btn-bg: var(--color-error-fondo); --btn-bg-hover: var(--color-error-fondo-hover); --btn-fg: var(--color-error-fuerte); }
    .error { margin: var(--espacio-2) 0 0; color: var(--color-error-fuerte); }
  `
})
export class AccionesSubastaComponent {
  private readonly subastaService = inject(SubastaService);

  @Input({ required: true }) subasta!: SubastaResponseDTO;
  /** La subasta se ha retirado: el listado debe quitarla. */
  @Output() cancelada = new EventEmitter<number>();
  /** Se han guardado cambios: el listado debe actualizar la tarjeta. */
  @Output() actualizada = new EventEmitter<SubastaResponseDTO>();

  readonly categorias = [
    { valor: 'tecnologia', nombre: 'Tecnología' },
    { valor: 'hogar', nombre: 'Hogar' },
    { valor: 'moda', nombre: 'Moda' },
    { valor: 'deportes', nombre: 'Deportes' },
    { valor: 'juguetes', nombre: 'Juguetes' },
    { valor: 'otros', nombre: 'Otros' }
  ];

  modo: Modo = 'ninguno';
  enviando = false;
  error = '';
  nombre = '';
  descripcion = '';
  categoria = '';

  get vencida(): boolean {
    return estaVencida(this.subasta.fechaFinal);
  }

  abrirEdicion() {
    this.error = '';
    this.enviando = true;
    // El listado no trae la descripción: se pide la ficha completa para rellenar el formulario.
    this.subastaService.obtenerSubasta(this.subasta.subastaID).subscribe({
      next: ficha => {
        this.nombre = ficha.nombreArticulo;
        this.descripcion = ficha.descripcion ?? '';
        this.categoria = ficha.categoria;
        this.enviando = false;
        this.modo = 'editando';
      },
      error: err => this.fallar(err)
    });
  }

  valido(): boolean {
    return !!this.nombre.trim() && !!this.descripcion.trim() && !!this.categoria;
  }

  guardar() {
    if (this.enviando || !this.valido()) {
      return;
    }
    this.error = '';
    this.enviando = true;
    this.subastaService.actualizarSubasta(this.subasta.subastaID, {
      nombreArticulo: this.nombre.trim(),
      descripcion: this.descripcion.trim(),
      categoria: this.categoria
    }).subscribe({
      next: nueva => {
        this.enviando = false;
        this.modo = 'ninguno';
        this.actualizada.emit({ ...this.subasta, ...nueva });
      },
      error: err => this.fallar(err)
    });
  }

  cancelar() {
    if (this.enviando) {
      return;
    }
    this.error = '';
    this.enviando = true;
    this.subastaService.cancelarSubasta(this.subasta.subastaID).subscribe({
      next: () => {
        this.enviando = false;
        this.cancelada.emit(this.subasta.subastaID);
      },
      error: err => this.fallar(err)
    });
  }

  cerrar() {
    this.modo = 'ninguno';
    this.error = '';
  }

  private fallar(err: { status?: number }) {
    this.enviando = false;
    this.modo = this.modo === 'editando' ? 'editando' : 'ninguno';
    this.error = this.mensajeDeError(err);
  }

  private mensajeDeError(err: { status?: number }): string {
    switch (err?.status) {
      case 0: return 'No se ha podido conectar con el servidor. Inténtalo de nuevo en unos minutos.';
      case 403: return 'Solo el creador de la subasta puede modificarla.';
      case 404: return 'La subasta ya no existe.';
      case 409: return 'Esta subasta ya tiene pujas o ha cerrado, y no se puede modificar.';
      case 400: return 'Revisa los datos: ningún campo puede estar vacío.';
      default: return 'No se ha podido completar la operación. Inténtalo de nuevo.';
    }
  }
}
