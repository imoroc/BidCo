import { Injectable } from '@angular/core';

/** Claves de localStorage de la sesión. Se mantienen para no cerrar las sesiones ya iniciadas. */
const CLAVE_TOKEN_USUARIO = 'authToken';
const CLAVE_TOKEN_EXPERTO = 'authTokenExpert';
const CLAVE_PERFIL_USUARIO = 'authUser';
const CLAVE_PERFIL_EXPERTO = 'authExpert';

export type RolSesion = 'user' | 'expert';

/**
 * Único punto de acceso al almacenamiento de la sesión: ningún otro fichero debe leer ni escribir
 * estas claves a mano. Los servicios de autenticación siguen siendo dos, pero comparten este almacén.
 */
@Injectable({ providedIn: 'root' })
export class SesionStore {
  /** Token de la sesión activa (usuario o, si no hay, experto), o null sin sesión. */
  token(): string | null {
    return this.tokenUsuario() ?? this.tokenExperto();
  }

  /** Rol de la sesión activa: el de usuario tiene prioridad, igual que el token. */
  rol(): RolSesion | null {
    if (this.tokenUsuario()) {
      return 'user';
    }
    return this.tokenExperto() ? 'expert' : null;
  }

  tokenUsuario(): string | null {
    return localStorage.getItem(CLAVE_TOKEN_USUARIO);
  }

  tokenExperto(): string | null {
    return localStorage.getItem(CLAVE_TOKEN_EXPERTO);
  }

  guardarTokenUsuario(token: string): void {
    localStorage.setItem(CLAVE_TOKEN_USUARIO, token);
  }

  guardarTokenExperto(token: string): void {
    localStorage.setItem(CLAVE_TOKEN_EXPERTO, token);
  }

  perfilUsuario(): string | null {
    return localStorage.getItem(CLAVE_PERFIL_USUARIO);
  }

  perfilExperto(): string | null {
    return localStorage.getItem(CLAVE_PERFIL_EXPERTO);
  }

  guardarPerfilUsuario(perfil: unknown): void {
    localStorage.setItem(CLAVE_PERFIL_USUARIO, JSON.stringify(perfil));
  }

  guardarPerfilExperto(perfil: unknown): void {
    localStorage.setItem(CLAVE_PERFIL_EXPERTO, JSON.stringify(perfil));
  }

  limpiarUsuario(): void {
    localStorage.removeItem(CLAVE_TOKEN_USUARIO);
    localStorage.removeItem(CLAVE_PERFIL_USUARIO);
  }

  limpiarExperto(): void {
    localStorage.removeItem(CLAVE_TOKEN_EXPERTO);
    localStorage.removeItem(CLAVE_PERFIL_EXPERTO);
  }
}
