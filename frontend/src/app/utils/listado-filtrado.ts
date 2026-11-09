import { DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpParams } from '@angular/common/http';
import { Observable, Subject, catchError, map, of, retry, switchMap, tap, throwError, timer } from 'rxjs';

import { Filtro } from '../services/auction.service';
import { ocultarVencidas } from './fechas';

/** Convierte el filtro de la interfaz en los parámetros de consulta que espera la API. */
export function filtroAParams(filtro: Filtro): HttpParams {
    let params = new HttpParams()
        .append('minPrice', filtro.minPrice)
        .append('maxPrice', filtro.maxPrice);
    filtro.categories.forEach(categoria => {
        params = params.append('categorias', categoria);
    });
    return params.append('dateOrder', filtro.dateOrder);
}

/** Errores que suelen deberse a que el servidor aún está arrancando (plan gratuito de Render). */
const ESTADOS_TRANSITORIOS = [0, 502, 503, 504];
const REINTENTOS = 4;

/** Reintenta con espera creciente solo los errores transitorios; el resto se propaga de inmediato. */
function reintentarSiArranca<T>() {
    return retry<T>({
        count: REINTENTOS,
        delay: (error, intento) => ESTADOS_TRANSITORIOS.includes(error?.status)
            ? timer(Math.min(intento * 2000, 8000))
            : throwError(() => error)
    });
}

/**
 * Carga de un listado filtrado. Todas las peticiones pasan por un único flujo:
 * la nueva cancela a la anterior, así nunca se muestra un resultado desfasado
 * y la lista no cambia sola cuando llegan respuestas fuera de orden.
 */
export class ListadoFiltrado<T> {
    // Señales: la plantilla se actualiza en cuanto llega la respuesta, sin depender de un clic.
    readonly items = signal<T[]>([]);
    readonly cargando = signal(true);
    readonly errorCarga = signal(false);

    private readonly peticiones$ = new Subject<{ params: HttpParams; mostrarVencidas: boolean }>();

    /**
     * @param fechaFinalDe si se indica, los elementos vencidos se ocultan salvo que el filtro
     *        pida mostrarlos; sin él se muestra todo (historiales del usuario).
     */
    constructor(
        cargador: (params: HttpParams) => Observable<T[]>,
        destroyRef: DestroyRef,
        private readonly fechaFinalDe?: (item: T) => string | null | undefined
    ) {
        this.peticiones$.pipe(
            tap(() => {
                this.cargando.set(true);
                this.errorCarga.set(false);
            }),
            switchMap(({ params, mostrarVencidas }) => cargador(params).pipe(
                reintentarSiArranca<T[]>(),
                map(items => this.fechaFinalDe ? ocultarVencidas(items, this.fechaFinalDe, mostrarVencidas) : items),
                catchError(err => {
                    console.error('Error al obtener el listado: ', err);
                    this.errorCarga.set(true);
                    return of([] as T[]);
                })
            )),
            takeUntilDestroyed(destroyRef)
        ).subscribe(items => {
            this.items.set(items);
            this.cargando.set(false);
        });
    }

    cargar(filtro: Filtro) {
        this.peticiones$.next({ params: filtroAParams(filtro), mostrarVencidas: filtro.mostrarVencidas ?? false });
    }
}
