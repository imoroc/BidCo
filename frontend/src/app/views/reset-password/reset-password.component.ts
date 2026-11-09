import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';

import { HeaderComponent } from '../../components/header/header.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { RecuperacionService, mensajeDeRecuperacion } from '../../services/recuperacion.service';

const LONGITUD_MINIMA = 10;

@Component({
  selector: 'app-reset-password',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, RouterModule, HeaderComponent, FooterComponent],
  // Mismo aspecto que "Recuperar contraseña".
  styleUrl: '../../components/forgot-password/forgot-password.css',
  template: `
    <app-header></app-header>
    <div class="auth-page-background">
      <main class="auth-container">
        <div class="auth-card">
          <header class="auth-header">
            <h1 class="brand-title">BidCo</h1>
            <h2>Nueva contraseña</h2>
            <p>Elige una contraseña de al menos {{ minimo }} caracteres.</p>
          </header>

          @if (hecho()) {
            <div class="alert-success">
              <span>Contraseña actualizada. Ya puedes iniciar sesión con la nueva.</span>
            </div>
            <a routerLink="/login" class="btn-primary" style="display:block;text-align:center;text-decoration:none">Iniciar sesión</a>
          } @else if (!token) {
            <p class="alert-error" role="alert">
              El enlace no es válido. Pide uno nuevo desde «Recuperar contraseña».
            </p>
          } @else {
            @if (error()) {
              <p class="alert-error" role="alert">{{ error() }}</p>
            }
            <form class="auth-form" (ngSubmit)="guardar()">
              <div class="input-group">
                <label for="contrasena">Nueva contraseña</label>
                <input type="password" id="contrasena" name="contrasena" autocomplete="new-password"
                       required [minlength]="minimo" maxlength="100" [(ngModel)]="contrasena" />
              </div>
              <div class="input-group">
                <label for="repetir">Repite la contraseña</label>
                <input type="password" id="repetir" name="repetir" autocomplete="new-password"
                       required maxlength="100" [(ngModel)]="repetir" />
              </div>
              @if (repetir && contrasena !== repetir) {
                <p class="alert-error" role="alert">Las contraseñas no coinciden.</p>
              }
              <button type="submit" class="btn-primary" [disabled]="!valido() || enviando()">
                {{ enviando() ? 'Guardando…' : 'Guardar contraseña' }}
              </button>
            </form>
          }

          <footer class="auth-footer">
            <p><a routerLink="/forgot-password" class="back-link">Pedir un enlace nuevo</a></p>
          </footer>
        </div>
      </main>
    </div>
    <app-footer></app-footer>
  `
})
export class ResetPasswordComponent {
  readonly minimo = LONGITUD_MINIMA;
  readonly token: string;
  contrasena = '';
  repetir = '';
  readonly enviando = signal(false);
  readonly hecho = signal(false);
  readonly error = signal('');

  constructor(route: ActivatedRoute, private recuperacion: RecuperacionService) {
    this.token = route.snapshot.queryParamMap.get('token') ?? '';
  }

  valido(): boolean {
    return this.contrasena.length >= LONGITUD_MINIMA && this.contrasena === this.repetir;
  }

  guardar() {
    if (!this.valido() || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.error.set('');
    this.recuperacion.restablecer(this.token, this.contrasena).subscribe({
      next: () => {
        this.enviando.set(false);
        this.hecho.set(true);
      },
      error: (err) => {
        this.enviando.set(false);
        this.error.set(mensajeDeRecuperacion(err));
      }
    });
  }
}
