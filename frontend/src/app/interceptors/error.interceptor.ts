import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { SesionStore } from '../services/sesion.store';

/**
 * Si la API responde 401 a una petición autenticada, la sesión ha caducado:
 * se limpian las credenciales locales y se lleva al usuario al login.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const sesion = inject(SesionStore);

  return next(req).pipe(
    catchError((error: unknown) => {
      const esApiPropia = req.url.startsWith(environment.apiUrl);
      const enviabaToken = req.headers.has('Authorization');
      const esLogin = /\/login\/?$/.test(req.url);

      if (error instanceof HttpErrorResponse && error.status === 401 && esApiPropia && enviabaToken && !esLogin) {
        const eraExperto = sesion.rol() === 'expert';
        authService.logout();
        sesion.limpiarExperto();
        router.navigate([eraExperto ? '/expert' : '/login']);
      }
      return throwError(() => error);
    })
  );
};
