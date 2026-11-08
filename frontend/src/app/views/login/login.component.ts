import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { HeaderComponent } from '../../components/header/header.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { AuthService } from '../../services/auth.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterModule, HeaderComponent, FooterComponent, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  // "email" representa correo o nombre de usuario
  email: string = '';
  password: string = '';
  remember: boolean = false;
  loginError: string = '';
  enviando = false;

  constructor(private auth: AuthService, private router: Router) {}

  login() {
    // Evita envíos repetidos: cada intento fallido cuenta para el límite del servidor.
    if (this.enviando) return;
    this.enviando = true;
    this.loginError = '';
    const loginData = {
      identificador: this.email,
      contrasena: this.password
    };

    this.auth.loginUser(loginData).subscribe({
      next: (data) => {
        // data.token contiene el JWT generado en el backend
        if (this.remember) {
          localStorage.setItem('authToken', data.token);
        }
        // Decodificamos el token y cargamos el perfil completo
        this.auth.setUserFromToken(data.token);
        this.router.navigateByUrl('/auction');
      },
      error: (err) => {
        this.enviando = false;
        this.loginError = this.mensajeDeError(err);
      }
    });
  }

  private mensajeDeError(err: any): string {
    if (err?.status === 429) {
      // El servidor indica cuándo se podrá volver a intentar.
      return err.error?.message || 'Demasiados intentos. Inténtalo de nuevo más tarde.';
    }
    if (err?.status === 0) {
      return 'No se ha podido conectar con el servidor. Comprueba tu conexión.';
    }
    if (err?.status === 401) {
      return 'Credenciales incorrectas. Revisa tu identificador y contraseña.';
    }
    return 'No se ha podido iniciar sesión. Inténtalo de nuevo.';
  }
}
