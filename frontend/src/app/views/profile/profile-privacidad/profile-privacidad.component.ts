import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService, Privacidad } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { ToastService } from '../../../services/toast.service';

import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-profile-privacidad',
  changeDetection: ChangeDetectionStrategy.Eager,
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
  mostrandoEliminar = false;
  contrasenaEliminar = '';
  eliminando = false;
  descargando = false;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
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

  descargarDatos() {
    const userId = this.authService.currentUser.value?.usuarioID;
    if (!userId || this.descargando) {
      return;
    }
    this.descargando = true;
    this.userService.exportarDatos(userId).subscribe({
      next: (blob) => {
        this.descargando = false;
        this.guardarArchivo(blob, 'bidco-mis-datos.json');
      },
      error: (err) => {
        this.descargando = false;
        this.toastService.error(err.error?.message || 'No se han podido descargar tus datos');
      }
    });
  }

  guardarArchivo(blob: Blob, nombre: string) {
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    URL.revokeObjectURL(url);
  }

  abrirEliminar() {
    this.mostrandoEliminar = true;
  }

  cancelarEliminar() {
    this.mostrandoEliminar = false;
    this.contrasenaEliminar = '';
  }

  confirmarEliminar() {
    const userId = this.authService.currentUser.value?.usuarioID;
    if (!userId || !this.contrasenaEliminar || this.eliminando) {
      return;
    }
    this.eliminando = true;
    this.userService.eliminarCuenta(userId, this.contrasenaEliminar).subscribe({
      next: () => {
        this.authService.logout();
        this.toastService.exito('Tu cuenta se ha eliminado');
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.eliminando = false;
        this.toastService.error(err.error?.message || 'No se ha podido eliminar la cuenta');
      }
    });
  }
}
