import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';

import { FormsModule } from '@angular/forms';
import { FooterComponent } from '../../components/footer/footer.component';
import { AuthExpertService } from '../../services/auth.expert.service';
import { SesionStore } from '../../services/sesion.store';

@Component({
  selector: 'app-expert-login',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [RouterModule, FooterComponent, FormsModule],
  templateUrl: './expert-login.component.html',
  styleUrl: './expert-login.component.css'
})
export class ExpertLoginComponent {
    email: string = '';
    password: string = '';
    remember: boolean = false;
    loginError: string = '';

    constructor(private auth: AuthExpertService, private router: Router, private sesion: SesionStore) {}

    login() {
        const loginData = {
            identificador: this.email,
            contraseña: this.password,
            rol: true
        };

        this.auth.loginExpert(loginData).subscribe({
            next: (data) => {
                // data.token contiene el JWT generado en el backend
                if (this.remember) {
                    this.sesion.guardarTokenExperto(data.token);
                }
                // Decodificamos el token y cargamos el perfil completo
                this.auth.setExpertFromToken(data.token);
                this.router.navigateByUrl('/auction');
            },
            error: (err) => {
                console.error("Error en login:", err);
                this.loginError = "Credenciales incorrectas. Revisa tu identificador y contraseña.";
            }
        });
    }
}
