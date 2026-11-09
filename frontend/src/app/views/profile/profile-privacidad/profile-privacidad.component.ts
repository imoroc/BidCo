import { Component, OnInit } from '@angular/core';
import { UserService, Privacidad } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-profile-privacidad',
  imports: [FormsModule],
  standalone: true,
  templateUrl: './profile-privacidad.component.html',
  styleUrls: ['./profile-privacidad.component.css']
})
export class ProfilePrivacidadComponent implements OnInit {
  privacidad: Privacidad = {
    privacidadAnonimoPujas: false,
    privacidadEstadisticas: true,
    privacidadPerfilVisible: true
  };
  cargando = true;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.cargarPrivacidad();
  }

  private cargarPrivacidad() {
    const userId = this.authService.currentUser.value?.usuarioID;
    if (userId) {
      this.userService.getPrivacidad(userId).subscribe({
        next: (data) => {
          this.privacidad = data;
          this.cargando = false;
        },
        error: (err) => {
          console.error('Error cargando configuración:', err);
          this.cargando = false;
        }
      });
    }
  }

  guardarPrivacidad() {
    const userId = this.authService.currentUser.value?.usuarioID;
    if (userId) {
      this.userService.updatePrivacidad(userId, this.privacidad).subscribe({
        next: () => {
          this.toastService.exito('Configuración guardada');
        },
        error: (err) => {
          console.error('Error guardando cambios:', err);
          this.toastService.error(err.error?.message || 'No se han podido guardar los cambios');
        }
      });
    }
  }
}