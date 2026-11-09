import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HeaderComponent } from '../../components/header/header.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { RecuperacionService, mensajeDeRecuperacion } from '../../services/recuperacion.service';

@Component({
  selector: 'app-forgot-password',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [FormsModule, RouterModule, HeaderComponent, FooterComponent],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.css'
})
export class ForgotPasswordComponent {
  email = '';
  readonly enviando = signal(false);
  readonly enviado = signal(false);
  readonly error = signal('');

  constructor(private recuperacion: RecuperacionService) {}

  recoverPassword() {
    if (!this.email || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.error.set('');
    this.recuperacion.solicitar(this.email.trim()).subscribe({
      next: () => {
        this.enviando.set(false);
        this.enviado.set(true);
      },
      error: (err) => {
        this.enviando.set(false);
        this.error.set(mensajeDeRecuperacion(err));
      }
    });
  }
}
