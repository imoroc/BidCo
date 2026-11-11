import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../environments/environment';
import { SesionStore } from '../services/sesion.store';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Never attach local credentials to third-party or unrelated URLs.
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  const token = inject(SesionStore).token();

  if (!token) {
    return next(req);
  }

  return next(req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  }));
};
