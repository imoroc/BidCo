import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { UserService } from '../../services/user.service';

/** Aviso fijo bajo la cabecera mientras la cuenta no tiene el correo verificado. */
@Component({
  selector: 'app-aviso-verificacion',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  template: `
    @if (visible()) {
      <div class="aviso-verificacion" role="status">
        <p>
          Confirma tu correo <strong>{{ correo() }}</strong> para pujar, participar en sorteos y crear subastas.
        </p>
        @if (enviado()) {
          <span class="aviso-enviado">Enlace enviado. Revisa tu bandeja de entrada.</span>
        } @else {
          <button type="button" class="btn-primary" [disabled]="enviando()" (click)="reenviar()">
            {{ enviando() ? 'Enviando…' : 'Reenviar enlace' }}
          </button>
        }
      </div>
    }
  `,
  styles: [`
    .aviso-verificacion {
      display: flex; flex-wrap: wrap; align-items: center; justify-content: center; gap: 12px;
      padding: 10px 16px;
      background: var(--color-superficie-alt);
      border-bottom: 1px solid var(--color-borde-suave);
      color: var(--color-texto);
      font-size: 0.9rem;
    }
    .aviso-verificacion p { margin: 0; }
    .aviso-verificacion strong { color: var(--color-texto-fuerte); }
    .aviso-enviado { font-weight: 600; color: var(--color-texto-fuerte); }
  `]
})
export class AvisoVerificacionComponent {
  private readonly usuario;
  readonly visible;
  readonly correo;
  readonly enviando = signal(false);
  readonly enviado = signal(false);

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private toastService: ToastService
  ) {
    this.usuario = toSignal(this.authService.currentUser$, { initialValue: null });
    // Solo un false explícito lo muestra: una sesión antigua sin el campo cuenta como verificada.
    this.visible = computed(() => this.usuario()?.correoVerificado === false);
    this.correo = computed(() => this.usuario()?.correoElectronico ?? '');
  }

  reenviar(): void {
    const id = this.usuario()?.usuarioID;
    if (!id || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.userService.reenviarVerificacion(id).subscribe({
      next: () => {
        this.enviando.set(false);
        this.enviado.set(true);
      },
      error: (err) => {
        this.enviando.set(false);
        this.toastService.error(err.error?.message || 'No se ha podido reenviar el enlace');
      }
    });
  }
}
