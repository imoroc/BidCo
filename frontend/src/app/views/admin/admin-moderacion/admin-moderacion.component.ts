import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import { FormsModule } from '@angular/forms';

import { AdminService } from '../../../services/admin.service';
import { SubastaResponseDTO, SubastaService } from '../../../services/auction.service';

const RESULTADOS = 12;
const MOTIVO_MAX = 500;

/**
 * Moderación: la administración busca una subasta por nombre y la retira indicando el motivo.
 * Al retirarla se devuelve el saldo a quien lideraba y se avisa por correo al creador.
 */
@Component({
  selector: 'app-admin-moderacion',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [FormsModule, CurrencyPipe],
  template: `
    <section class="admin-form-card wide-card">
      <div class="card-header">
        <h3 class="form-title">Moderar subastas</h3>
        <p>Busca una subasta por nombre y retírala si incumple las normas. No se puede deshacer.</p>
      </div>

      <div class="form-body">
        <form class="buscar" (ngSubmit)="buscar()">
          <div class="input-group">
            <label for="mod-busqueda">Nombre del artículo</label>
            <input id="mod-busqueda" type="search" name="termino" [(ngModel)]="termino" maxlength="255" autocomplete="off">
          </div>
          <button class="btn-admin" type="submit" [disabled]="buscando || !termino.trim()">{{ buscando ? 'Buscando…' : 'Buscar' }}</button>
        </form>

        @if (mensaje) {
          <div class="alert-success" role="status">{{ mensaje }}</div>
        }
        @if (error) {
          <div class="alert-error" role="alert">{{ error }}</div>
        }

        @if (buscado && !buscando && !resultados.length && !error) {
          <p class="vacio">No hay subastas con ese nombre.</p>
        }

        @if (resultados.length) {
          <ul class="resultados" aria-label="Resultados de la búsqueda">
            @for (s of resultados; track s.subastaID) {
              <li class="fila">
                <div class="datos">
                  <strong>{{ s.nombreArticulo }}</strong>
                  <span>#{{ s.subastaID }} · {{ s.subastaNormal ? 'Normal' : 'A ciegas' }} · {{ s.categoria }} · {{ s.precioFinal | currency:'EUR':'symbol':'1.2-2':'es' }}</span>
                </div>
                @if (seleccionada?.subastaID !== s.subastaID) {
                  <button type="button" class="btn-secondary peligro" (click)="elegir(s)">Retirar</button>
                } @else {
                  <form class="retirada" (ngSubmit)="retirar()" [attr.aria-label]="'Retirar ' + s.nombreArticulo">
                    <div class="input-group">
                      <label [for]="'motivo-' + s.subastaID">Motivo (se envía al creador)</label>
                      <textarea [id]="'motivo-' + s.subastaID" name="motivo" rows="3" [maxlength]="motivoMax" [(ngModel)]="motivo" required></textarea>
                    </div>
                    <div class="acciones">
                      <button type="submit" class="btn-primary peligro" [disabled]="enviando || !motivo.trim()">{{ enviando ? 'Retirando…' : 'Confirmar retirada' }}</button>
                      <button type="button" class="btn-secondary" [disabled]="enviando" (click)="descartar()">Cancelar</button>
                    </div>
                  </form>
                }
              </li>
            }
          </ul>
          @if (resultados.length >= resultadosMax) {
            <p class="vacio">Se muestran los primeros {{ resultadosMax }}. Afina la búsqueda para ver otros.</p>
          }
        }
      </div>
    </section>
  `,
  styles: `
    .buscar { display: flex; flex-wrap: wrap; align-items: flex-end; gap: var(--espacio-3); }
    .buscar .input-group { flex: 1 1 14rem; }
    .resultados { display: grid; gap: var(--espacio-3); margin: 0; padding: 0; list-style: none; }
    .fila { display: grid; gap: var(--espacio-3); padding: var(--espacio-3); border: 1px solid var(--color-borde-suave); border-radius: var(--radio-md); background: var(--color-superficie); }
    .datos { display: grid; gap: var(--espacio-1); }
    .datos span, .vacio { font-size: var(--texto-sm); color: var(--color-texto-suave); }
    .retirada { display: grid; gap: var(--espacio-3); }
    .acciones { display: flex; flex-wrap: wrap; gap: var(--espacio-2); }
    .peligro { --btn-bg: var(--color-error-fondo); --btn-bg-hover: var(--color-error-fondo-hover); --btn-fg: var(--color-error-fuerte); }
  `
})
export class AdminModeracionComponent {
  private readonly subastas = inject(SubastaService);
  private readonly admin = inject(AdminService);

  readonly resultadosMax = RESULTADOS;
  readonly motivoMax = MOTIVO_MAX;

  termino = '';
  resultados: SubastaResponseDTO[] = [];
  seleccionada: SubastaResponseDTO | null = null;
  motivo = '';
  buscando = false;
  buscado = false;
  enviando = false;
  mensaje = '';
  error = '';

  buscar() {
    const termino = this.termino.trim();
    if (!termino || this.buscando) {
      return;
    }
    this.buscando = true;
    this.error = '';
    this.mensaje = '';
    this.seleccionada = null;
    const params = new HttpParams()
      .append('minPrice', 0).append('maxPrice', 0).append('dateOrder', '')
      .append('page', 0).append('size', RESULTADOS);
    this.subastas.getSubastasFiltradasPorNombre(params, termino).subscribe({
      next: lista => {
        this.resultados = lista;
        this.buscado = true;
        this.buscando = false;
      },
      error: () => {
        this.resultados = [];
        this.buscando = false;
        this.error = 'No se ha podido buscar. Inténtalo de nuevo.';
      }
    });
  }

  elegir(subasta: SubastaResponseDTO) {
    this.seleccionada = subasta;
    this.motivo = '';
    this.error = '';
    this.mensaje = '';
  }

  descartar() {
    this.seleccionada = null;
    this.error = '';
  }

  retirar() {
    const subasta = this.seleccionada;
    const motivo = this.motivo.trim();
    if (!subasta || !motivo || this.enviando) {
      return;
    }
    this.enviando = true;
    this.error = '';
    this.admin.retirarSubasta(subasta.subastaID, motivo).subscribe({
      next: () => {
        this.enviando = false;
        this.resultados = this.resultados.filter(s => s.subastaID !== subasta.subastaID);
        this.seleccionada = null;
        this.mensaje = `Subasta «${subasta.nombreArticulo}» retirada.`;
      },
      error: err => {
        this.enviando = false;
        this.error = this.mensajeDeError(err);
      }
    });
  }

  private mensajeDeError(err: { status?: number }): string {
    switch (err?.status) {
      case 0: return 'No se ha podido conectar con el servidor. Inténtalo de nuevo en unos minutos.';
      case 400: return 'Indica el motivo (hasta 500 caracteres).';
      case 403: return 'Solo la administración puede retirar subastas; las cuentas de experto no.';
      case 404: return 'La subasta ya no existe.';
      case 409: return 'La subasta ya se cerró con ganador y no se puede retirar.';
      default: return 'No se ha podido retirar la subasta. Inténtalo de nuevo.';
    }
  }
}
