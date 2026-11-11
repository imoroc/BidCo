import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService, UsuarioResponse } from '../../../services/auth.service';
import { UserService, UsuarioUpdate } from '../../../services/user.service';
import { AuthExpertService } from '../../../services/auth.expert.service';
import { SesionStore } from '../../../services/sesion.store';

@Component({
  selector: 'app-profile-datos',
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-datos.component.html',
  styleUrls: ['./profile-datos.component.css']
})
export class ProfileDatosComponent implements OnInit {
  user: UsuarioResponse = {
    usuarioID: 0,
    nombreUsuario: '',
    correoElectronico: '',
    contrasena: '',
    ciudad: '',
    codigoPostal: '',
    calle: '',
    numeroPiso: 0,
    letraPiso: '',
    pais: '',
    saldo: 0,
    puntos: 0,
  };

  updateMessage: string = '';
  errorMessage: string = '';

  // Variables para cambio de contraseña
  currentPassword: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  isExpert = false;

  constructor(private authService: AuthService, private expertAuthService: AuthExpertService, private userService: UserService, private sesion: SesionStore) {}

  ngOnInit() {
    var storedUser;
    //Comprobamos si un experto ha iniciado sesion
    this.expertAuthService.isLoggedIn$.subscribe((estado) => {
        this.isExpert = estado;
        if (estado) storedUser = this.sesion.perfilExperto();
        else storedUser = this.sesion.perfilUsuario();
    });

    
    // Si hay usuario almacenado pero falta información
    if (storedUser) {
      this.user = JSON.parse(storedUser);
      if (!this.user.nombreUsuario) { // Verifica un campo obligatorio
        this.loadUserFromBackend();
      }
    } else {
      this.loadUserFromBackend();
    }
  }
  
  private loadUserFromBackend() {
    const userId = this.authService.currentUser.value?.usuarioID;
    if (userId) {
      this.userService.getUserById(userId).subscribe({
        next: (user) => {
          this.user = user;
          this.authService.setUser(user);
        },
        error: (err) => console.error('Error cargando usuario:', err)
      });
    }
  }

  saveChanges() {
    const updatedData = {
      nombreUsuario: this.user.nombreUsuario,
      correoElectronico: this.user.correoElectronico,
      ciudad: this.user.ciudad,
      codigoPostal: this.user.codigoPostal,
      calle: this.user.calle,
      numeroPiso: this.user.numeroPiso,
      letraPiso: this.user.letraPiso,
      // Campos requeridos por el backend pero no editables
      pais: this.user.pais,
      saldo: this.user.saldo,
      puntos: this.user.puntos,
      contrasena: this.user.contrasena // Mantenemos la contraseña actual
    };
  
    this.userService.updateUser(this.user.usuarioID, updatedData).subscribe({
      next: (response) => {
        this.authService.setUser(response);
        this.updateMessage = '¡Datos actualizados!';
        setTimeout(() => this.updateMessage = '', 3000);
      },
      error: (error) => {
        this.errorMessage = (error.error?.message || 'Error desconocido');
      }
    });
  }
  
  updatePasswdMessage: string = '';
  errorPasswdMessage: string = '';

  updatePassword() {
    if (this.newPassword !== this.confirmPassword) {
      this.errorPasswdMessage = 'Las contraseñas no coinciden.';
      return;
    }
  
    this.userService.updatePassword(this.user.usuarioID, {
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    }).subscribe({
      next: ({ token }) => {
        // El token anterior ya no vale: se guarda el nuevo para no perder la sesión en este dispositivo.
        this.sesion.guardarTokenUsuario(token);
        this.updatePasswdMessage = '¡Contraseña actualizada correctamente!';
        setTimeout(() => this.updatePasswdMessage = '', 3000);
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
      },
      error: (error) => {
        this.errorPasswdMessage = error.message || 'Error al cambiar la contraseña';
        setTimeout(() => this.errorPasswdMessage = '', 5000); 
      }
    });
  }

  cantidadRecarga: number = 0;
  recargaMessage: string = '';

  recargarSaldo() {
    if (this.cantidadRecarga > 0) {
      this.userService.recargarSaldo(this.user.usuarioID, this.cantidadRecarga).subscribe({
        next: (nuevoSaldo) => {
          this.user.saldo = nuevoSaldo;
          this.authService.setUser(this.user);
          this.recargaMessage = '¡Saldo recargado correctamente!';
          this.cantidadRecarga = 0;
          setTimeout(() => this.recargaMessage = '', 5000);
        },
        error: () => {
          this.recargaMessage = 'Error al recargar saldo.';
          setTimeout(() => this.recargaMessage = '', 5000);
        }
      });
    }
}
}