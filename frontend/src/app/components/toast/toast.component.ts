import { Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast.service';

/** Contenedor de toasts: se monta una sola vez en la raíz de la aplicación. */
@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <div class="toasts">
      @for (t of toastService.toasts(); track t.id) {
        <div class="toast" [class]="'toast toast-' + t.tipo" [attr.role]="t.tipo === 'error' ? 'alert' : 'status'">
          <span>{{ t.mensaje }}</span>
          <button type="button" class="toast-cerrar" aria-label="Cerrar aviso" (click)="toastService.cerrar(t.id)">×</button>
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .toasts {
      position: fixed;
      left: 50%;
      bottom: var(--espacio-5);
      transform: translateX(-50%);
      z-index: 2000;
      display: flex;
      flex-direction: column;
      gap: var(--espacio-2);
      width: min(480px, calc(100vw - 2 * var(--espacio-4)));
    }

    .toast {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--espacio-3);
      padding: var(--espacio-3) var(--espacio-4);
      border-radius: var(--radio-md);
      background: var(--color-texto);
      color: var(--color-sobre-marca);
      font-size: var(--texto-sm);
      box-shadow: var(--sombra-2);
    }

    .toast-exito { border-left: 4px solid var(--color-exito); }
    .toast-error { border-left: 4px solid var(--color-error); }
    .toast-info { border-left: 4px solid var(--color-acento); }

    .toast-cerrar {
      min-width: var(--objetivo-tactil);
      min-height: var(--objetivo-tactil);
      border: none;
      background: transparent;
      color: inherit;
      font-size: var(--texto-lg);
      cursor: pointer;
    }
  `
})
export class ToastComponent {
  readonly toastService = inject(ToastService);
}
