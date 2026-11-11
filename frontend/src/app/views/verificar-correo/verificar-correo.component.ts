import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { HeaderComponent } from '../../components/header/header.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';

type Estado = 'verificando' | 'verificado' | 'error';

/** Destino del enlace del correo de verificación. */
@Component({
  selector: 'app-verificar-correo',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [RouterModule, HeaderComponent, FooterComponent],
  // Mismo aspecto que "Recuperar contraseña".
  styleUrl: '../../components/forgot-password/forgot-password.css',
  styles: [`.accion { display: block; text-align: center; text-decoration: none; }`],
  template: `
    <app-header></app-header>
    <div class="auth-page-background">
      <main class="auth-container">
        <div class="auth-card">
          <header class="auth-header">
            <h1 class="brand-title">BidCo</h1>
            <h2>Verificación del correo</h2>
          </header>

          @switch (estado()) {
            @case ('verificando') {
              <p role="status">Comprobando el enlace…</p>
            }
            @case ('verificado') {
              <div class="alert-success" role="status">
                <span>Correo verificado. Ya puedes pujar, participar en sorteos y crear subastas.</span>
              </div>
              <a [routerLink]="authService.isLoggedIn() ? '/auction' : '/login'" class="btn-primary accion">
                {{ authService.isLoggedIn() ? 'Ir a las subastas' : 'Iniciar sesión' }}
              </a>
            }
            @default {
              <p class="alert-error" role="alert">{{ mensajeError() }}</p>
              <footer class="auth-footer">
                <p>Inicia sesión y pulsa «Reenviar enlace» en el aviso de la parte superior para recibir uno nuevo.</p>
                <p><a routerLink="/login" class="back-link">Iniciar sesión</a></p>
              </footer>
            }
          }
        </div>
      </main>
    </div>
    <app-footer></app-footer>
  `
})
export class VerificarCorreoComponent {
  readonly estado = signal<Estado>('verificando');
  readonly mensajeError = signal('');

  constructor(route: ActivatedRoute, private userService: UserService, readonly authService: AuthService) {
    const token = route.snapshot.queryParamMap.get('token') ?? '';
    if (!token) {
      this.fallar('El enlace no es válido.');
      return;
    }
    this.userService.verificarCorreo(token).subscribe({
      next: () => {
        this.estado.set('verificado');
        // Si hay sesión abierta, el aviso de la cabecera debe desaparecer sin recargar.
        this.authService.refrescarUsuario();
      },
      error: (err) => this.fallar(
        err?.status === 400 && err.error?.message
          ? err.error.message
          : 'No se ha podido verificar el correo. Inténtalo de nuevo más tarde.')
    });
  }

  private fallar(mensaje: string): void {
    this.mensajeError.set(mensaje);
    this.estado.set('error');
  }
}
