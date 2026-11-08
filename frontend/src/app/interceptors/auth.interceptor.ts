import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Never attach local credentials to third-party or unrelated URLs.
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }

  const token =
    localStorage.getItem('authToken') ||
    localStorage.getItem('authTokenExpert');

  if (!token) {
    return next(req);
  }

  return next(req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  }));
};
