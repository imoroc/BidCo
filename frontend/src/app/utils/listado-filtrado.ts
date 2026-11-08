import { DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpParams } from '@angular/common/http';
import { Observable, Subject, catchError, of, switchMap, tap } from 'rxjs';

import { Filtro } from '../services/auction.service';

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

/**
 * Carga de un listado filtrado. Todas las peticiones pasan por un único flujo:
 * la nueva cancela a la anterior, así nunca se muestra un resultado desfasado
 * y la lista no cambia sola cuando llegan respuestas fuera de orden.
 */
export class ListadoFiltrado<T> {
    items: T[] = [];
    cargando = true;
    errorCarga = false;

    private readonly peticiones$ = new Subject<HttpParams>();

    constructor(cargador: (params: HttpParams) => Observable<T[]>, destroyRef: DestroyRef) {
        this.peticiones$.pipe(
            tap(() => {
                this.cargando = true;
                this.errorCarga = false;
            }),
            switchMap(params => cargador(params).pipe(
                catchError(err => {
                    console.error('Error al obtener el listado: ', err);
                    this.errorCarga = true;
                    return of([] as T[]);
                })
            )),
            takeUntilDestroyed(destroyRef)
        ).subscribe(items => {
            this.items = items;
            this.cargando = false;
        });
    }

    cargar(filtro: Filtro) {
        this.peticiones$.next(filtroAParams(filtro));
    }
}
