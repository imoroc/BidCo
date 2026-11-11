import { Injectable } from '@angular/core';
import { combineLatest, distinctUntilChanged, map, Observable } from 'rxjs';

import { AuthService } from './auth.service';
import { AuthExpertService } from './auth.expert.service';

export interface EstadoSesion {
  /** Hay sesión de usuario o de experto. */
  conectado: boolean;
  /** La sesión activa es de experto. Si hay las dos, manda la de usuario. */
  esExperto: boolean;
}

/**
 * Estado de sesión combinado de usuario y experto. Sustituye a que cada vista se suscriba a los dos
 * servicios y repita la regla de prioridad. Emite de forma síncrona al suscribirse y cada vez que cambia.
 */
@Injectable({ providedIn: 'root' })
export class SesionActivaService {
  readonly estado$: Observable<EstadoSesion>;

  constructor(auth: AuthService, expertAuth: AuthExpertService) {
    this.estado$ = combineLatest([auth.isLoggedIn$, expertAuth.isLoggedIn$]).pipe(
      map(([usuario, experto]) => ({ conectado: usuario || experto, esExperto: !usuario && experto })),
      distinctUntilChanged((a, b) => a.conectado === b.conectado && a.esExperto === b.esExperto)
    );
  }
}
