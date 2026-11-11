import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { catchError, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SesionStore } from './sesion.store';

export interface UsuarioResponse {
  usuarioID: number;
  nombreUsuario: string;
  correoElectronico: string;
  ciudad: string;
  codigoPostal: string;
  calle: string;
  numeroPiso: number;
  letraPiso?: string;
  pais: string;
  saldo?: number;
  puntos?: number;
  // Ausente en sesiones guardadas antes de existir la verificación: se trata como verificado.
  correoVerificado?: boolean;
}

export interface UsuarioCreate {
  nombreUsuario: string;
  correoElectronico: string;
  contraseña: string;
  pais: string;
  ciudad: string;
  codigoPostal: string;
  calle: string;
  numeroPiso: number;
  letraPiso?: string;
}

export interface UsuarioUpdate {
  nombreUsuario: string;
  correoElectronico: string;
  ciudad: string;
  codigoPostal: string;
  calle: string;
  numeroPiso: number;
  letraPiso?: string;
}

export interface Privacidad {
  privacidadAnonimoPujas: boolean;
  privacidadEstadisticas: boolean;
  privacidadPerfilVisible: boolean;
}

export interface Stats {
  participatedBids: number;
  wonBids: number;
  createdBids: number;
  participatedDraws: number;
  wonDraws: number;
  createdDraws: number;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient, private sesion: SesionStore) {}

  updateUser(id: number, usuario: UsuarioUpdate): Observable<UsuarioResponse> {
    const token = this.sesion.tokenUsuario();
    return this.http.put<UsuarioResponse>(`${this.apiUrl}/${id}`, usuario, {
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` })
    });
  }

  // Devuelve un token nuevo: el cambio cierra las demás sesiones y esta debe seguir con el token actualizado.
  updatePassword(id: number, passwords: { currentPassword: string, newPassword: string }): Observable<{ token: string }> {
    const token = this.sesion.tokenUsuario();
    return this.http.patch<{ token: string }>(`${this.apiUrl}/${id}/password`, passwords, {
      headers: new HttpHeaders({
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      })
    }).pipe(catchError(error => {
      throw new Error(error.error?.message || 'Error desconocido');
    }));
  }

  // Público: el enlace del correo se abre sin sesión.
  verificarCorreo(token: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/verificar-correo`, { token });
  }

  reenviarVerificacion(id: number): Observable<void> {
    const token = this.sesion.tokenUsuario();
    return this.http.post<void>(`${this.apiUrl}/${id}/reenviar-verificacion`, null, {
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` })
    });
  }

  // Copia en JSON de los datos del usuario (derecho de acceso y portabilidad).
  exportarDatos(id: number): Observable<Blob> {
    const token = this.sesion.tokenUsuario();
    return this.http.get(`${this.apiUrl}/${id}/exportar`, {
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` }),
      responseType: 'blob'
    });
  }

  // La contraseña viaja en el cuerpo (nunca en la URL). Anonimiza la cuenta; el backend rechaza el borrado
  // si quedan subastas o sorteos sin cerrar.
  eliminarCuenta(id: number, contrasena: string): Observable<void> {
    const token = this.sesion.tokenUsuario();
    return this.http.delete<void>(`${this.apiUrl}/${id}`, {
      headers: new HttpHeaders({ Authorization: `Bearer ${token}` }),
      body: { contrasena }
    });
  }

  registerUser(usuario: UsuarioCreate): Observable<UsuarioResponse> {
    return this.http.post<UsuarioResponse>(this.apiUrl, usuario);
  }

  getUserById(id: number): Observable<UsuarioResponse> {
    return this.http.get<UsuarioResponse>(`${this.apiUrl}/${id}`);
  }

  getPrivacidad(id: number): Observable<Privacidad> {
    return this.http.get<Privacidad>(`${this.apiUrl}/${id}/privacidad`);
  }

  updatePrivacidad(id: number, privacidad: Privacidad): Observable<UsuarioResponse> {
    return this.http.put<UsuarioResponse>(`${this.apiUrl}/${id}/privacidad`, privacidad);
  }

  getUserStats(id: number): Observable<Stats> {
    return this.http.get<Stats>(`${this.apiUrl}/${id}/stats`);
  }

  recargarSaldo(usuarioID: number, cantidad: number): Observable<number> {
    return this.http.patch<number>(`${this.apiUrl}/${usuarioID}/recargar-saldo`, { cantidad });
  }
}
