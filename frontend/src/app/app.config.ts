import { ApplicationConfig, LOCALE_ID, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { IMAGE_LOADER, registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { TitleStrategy, provideRouter, withViewTransitions } from '@angular/router';
import { HttpClient, provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';
import { relojInterceptor } from './interceptors/reloj.interceptor';
import { SeoTitleStrategy } from './services/seo-title.strategy';
import { cloudinaryLoader } from './utils/cloudinary-loader';
import { environment } from '../environments/environment';

registerLocaleData(localeEs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    { provide: LOCALE_ID, useValue: 'es-ES' },
    { provide: IMAGE_LOADER, useValue: cloudinaryLoader },
    { provide: TitleStrategy, useExisting: SeoTitleStrategy },
    // Fundido corto entre vistas (View Transitions API); sin efecto en la carga inicial.
    provideRouter(routes, withViewTransitions({ skipInitialTransition: true })),
    // XHR y no fetch (el valor por defecto en Angular 22): zone.js vigila XHR, así que las respuestas
    // repintan la vista. Con fetch, los campos de los componentes no se actualizaban hasta un clic.
    provideHttpClient(withXhr(), withInterceptors([authInterceptor, errorInterceptor, relojInterceptor])),
    // Despierta el backend (el plan gratuito de Render lo duerme) sin bloquear el arranque.
    provideAppInitializer(() => {
      inject(HttpClient).get(`${environment.apiUrl}/ping`, { responseType: 'text' }).subscribe({ error: () => {} });
    })
  ]
};