import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

export interface MensajeRespuesta {
  mensaje: string;
}

/** Recuperación de contraseña por correo: endpoints públicos, sin sesión. */
@Injectable({ providedIn: 'root' })
export class RecuperacionService {
  private readonly apiUrl = `${environment.apiUrl}/usuarios`;

  constructor(private http: HttpClient) {}

  solicitar(correo: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(`${this.apiUrl}/recuperar-contrasena`, { correo });
  }

  restablecer(token: string, contrasena: string): Observable<MensajeRespuesta> {
    return this.http.post<MensajeRespuesta>(`${this.apiUrl}/restablecer-contrasena`, { token, contrasena });
  }
}

/** Mensaje legible para un error de estos endpoints; el servidor ya envía textos claros en 400, 429 y 503. */
export function mensajeDeRecuperacion(err: any): string {
  if (err?.status === 0) {
    return 'No se ha podido conectar con el servidor. Comprueba tu conexión e inténtalo de nuevo.';
  }
  if ([400, 429, 503].includes(err?.status) && err.error?.message) {
    return err.error.message;
  }
  return 'No se ha podido completar la operación. Inténtalo de nuevo más tarde.';
}
