import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HttpClient, provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // XHR y no fetch (el valor por defecto en Angular 22): zone.js vigila XHR, así que las respuestas
    // repintan la vista. Con fetch, los campos de los componentes no se actualizaban hasta un clic.
    provideHttpClient(withXhr(), withInterceptors([authInterceptor, errorInterceptor])),
    // Despierta el backend (el plan gratuito de Render lo duerme) sin bloquear el arranque.
    provideAppInitializer(() => {
      inject(HttpClient).get(`${environment.apiUrl}/ping`, { responseType: 'text' }).subscribe({ error: () => {} });
    })
  ]
};