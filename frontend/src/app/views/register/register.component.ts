import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { NgForm, FormsModule } from '@angular/forms';

import { FooterComponent } from '../../components/footer/footer.component';
import { HeaderComponent } from '../../components/header/header.component';
import { UserService, UsuarioCreate, UsuarioResponse } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [RouterModule, FooterComponent, HeaderComponent, FormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  username = '';
  email = '';
  password = '';
  repeatPassword = '';
  city = '';
  postalCode = '';
  calle = '';
  numeroPiso = '';
  letraPiso = '';
  pais = 'España';
  formError = '';

  constructor(private router: Router, private userService: UserService, private authService: AuthService) {}

  register(form: NgForm) {
    if (form.invalid || this.password !== this.repeatPassword) {
      this.formError = 'Por favor, revisa los campos con errores.';
      Object.values(form.controls).forEach(control => control.markAsTouched());
      return;
    }

    const nuevoUsuario: UsuarioCreate = {
      nombreUsuario: this.username.trim(),
      correoElectronico: this.email.trim(),
      contraseña: this.password,
      pais: this.pais,
      ciudad: this.city.trim(),
      codigoPostal: this.postalCode.trim(),
      calle: this.calle.trim(),
      numeroPiso: Number(this.numeroPiso),
      letraPiso: this.letraPiso.trim()
    };

    this.userService.registerUser(nuevoUsuario).subscribe({
      next: (_respuesta: UsuarioResponse) => {
        this.authService.loginUser({
          identificador: this.email.trim(),
          contrasena: this.password
        }).subscribe({
          next: (data) => {
            this.authService.setUserFromToken(data.token);
            this.router.navigateByUrl('/auction');
          },
          error: () => {
            this.formError = 'La cuenta se creó, pero no se pudo iniciar sesión. Accede desde el formulario de login.';
            this.router.navigateByUrl('/login');
          }
        });
      },
      error: (error) => {
        this.formError = error.error?.message || 'No se pudo completar el registro. Revisa los datos e inténtalo de nuevo.';
      }
    });
  }
}
