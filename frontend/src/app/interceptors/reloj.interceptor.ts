import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { sincronizarReloj } from '../utils/fechas';

/**
 * Toma la hora del servidor de la cabecera Date de sus respuestas, para que las cuentas atrás
 * coincidan con el cierre real aunque el reloj del dispositivo vaya adelantado o atrasado.
 */
export const relojInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith(environment.apiUrl)) {
    return next(req);
  }
  const enviado = Date.now();
  return next(req).pipe(
    tap(evento => {
      if (evento instanceof HttpResponse) {
        sincronizarReloj(evento.headers.get('Date'), enviado, Date.now());
      }
    })
  );
};
