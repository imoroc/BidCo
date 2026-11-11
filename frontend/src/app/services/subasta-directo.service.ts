import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/** Estado que el servidor difunde en cada puja que pasa a liderar (el cierre cambia con el anti-sniping). */
export interface EstadoSubasta {
    precioFinal: number | null;
    fechaFinal: string | null;
}

/** Abre la conexión de eventos; se sustituye en las pruebas, que no tienen servidor. */
export const CREAR_ORIGEN_EVENTOS = new InjectionToken<(url: string) => EventSource>('CREAR_ORIGEN_EVENTOS', {
    providedIn: 'root',
    factory: () => (url: string) => new EventSource(url)
});

/** Lee el JSON de un evento `estado`; devuelve null si no tiene el formato esperado. */
export function interpretarEstado(datos: unknown): EstadoSubasta | null {
    if (typeof datos !== 'string') {
        return null;
    }
    try {
        const crudo = JSON.parse(datos);
        const precio = crudo?.precioFinal;
        const fin = crudo?.fechaFinal;
        if ((precio !== null && typeof precio !== 'number') || (fin !== null && typeof fin !== 'string')) {
            return null;
        }
        return { precioFinal: precio, fechaFinal: fin };
    } catch {
        return null;
    }
}

/**
 * Precio y cierre en directo de una subasta normal (SSE, solo lectura). `EventSource` reconecta solo y el
 * servidor envía el estado actual al conectar, así que tras un corte la ficha se pone al día sin lógica extra.
 * Al cancelar la suscripción se cierra la conexión.
 */
@Injectable({ providedIn: 'root' })
export class SubastaDirectoService {
    private readonly crearOrigen = inject(CREAR_ORIGEN_EVENTOS);

    seguir(subastaId: number): Observable<EstadoSubasta> {
        return new Observable<EstadoSubasta>(suscriptor => {
            const origen = this.crearOrigen(`${environment.apiUrl}/subastas/${subastaId}/eventos`);
            origen.addEventListener('estado', evento => {
                const estado = interpretarEstado((evento as MessageEvent).data);
                if (estado) {
                    suscriptor.next(estado);
                }
            });
            return () => origen.close();
        });
    }
}
