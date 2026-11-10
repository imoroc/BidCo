import { DestroyRef, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpParams } from '@angular/common/http';
import { Observable, Subject, catchError, map, of, retry, switchMap, tap, throwError, timer } from 'rxjs';

import { Filtro } from '../services/auction.service';
import { ocultarVencidas } from './fechas';

/**
 * Convierte el filtro de la interfaz en los parámetros de consulta que espera la API.
 * Con `tamano` se pide una sola página (empezando en 0); sin él, la lista completa.
 */
export function filtroAParams(filtro: Filtro, pagina = 0, tamano?: number): HttpParams {
    let params = new HttpParams()
        .append('minPrice', filtro.minPrice)
        .append('maxPrice', filtro.maxPrice);
    filtro.categories.forEach(categoria => {
        params = params.append('categorias', categoria);
    });
    params = params.append('dateOrder', filtro.dateOrder);
    return tamano ? params.append('page', pagina).append('size', tamano) : params;
}

/** Elementos que se piden por página en los listados públicos (el servidor admite hasta 100). */
export const TAMANO_PAGINA = 12;

/** Páginas seguidas que llegan enteras ocultas (vencidas) antes de dejar que el usuario decida. */
const MAX_PAGINAS_OCULTAS_SEGUIDAS = 5;

interface Peticion { params: HttpParams; mostrarVencidas: boolean; acumular: boolean; }
interface Resultado<T> { items: T[]; recibidos: number; error: boolean; peticion: Peticion; }

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
 *
 * Con `tamanoPagina` el listado se pide por páginas: `cargar` empieza de cero y `cargarMas`
 * añade la siguiente. Sin él se pide todo de una vez, como antes.
 */
export class ListadoFiltrado<T> {
    // Señales: la plantilla se actualiza en cuanto llega la respuesta, sin depender de un clic.
    readonly items = signal<T[]>([]);
    readonly cargando = signal(true);
    /** Carga de una página adicional: la lista que ya se ve se mantiene en pantalla. */
    readonly cargandoMas = signal(false);
    readonly errorCarga = signal(false);
    /** Cuántos elementos vencidos se han ocultado hasta ahora. */
    readonly ocultas = signal(0);
    /** Puede haber más páginas: la última llegó completa. */
    readonly hayMas = signal(false);

    private readonly peticiones$ = new Subject<Peticion>();
    private filtro?: Filtro;
    private pagina = 0;
    private paginasOcultasSeguidas = 0;

    /**
     * @param fechaFinalDe si se indica, los elementos vencidos se ocultan salvo que el filtro
     *        pida mostrarlos; sin él se muestra todo (historiales del usuario).
     * @param tamanoPagina elementos por página; sin él no se pagina.
     */
    constructor(
        cargador: (params: HttpParams) => Observable<T[]>,
        destroyRef: DestroyRef,
        private readonly fechaFinalDe?: (item: T) => string | null | undefined,
        private readonly tamanoPagina?: number
    ) {
        this.peticiones$.pipe(
            tap(({ acumular }) => {
                (acumular && this.items().length > 0 ? this.cargandoMas : this.cargando).set(true);
                this.errorCarga.set(false);
            }),
            switchMap(peticion => cargador(peticion.params).pipe(
                reintentarSiArranca<T[]>(),
                map((recibidos): Resultado<T> => ({
                    items: this.fechaFinalDe ? ocultarVencidas(recibidos, this.fechaFinalDe, peticion.mostrarVencidas) : recibidos,
                    recibidos: recibidos.length,
                    error: false,
                    peticion
                })),
                catchError(err => {
                    console.error('Error al obtener el listado: ', err);
                    return of<Resultado<T>>({ items: [], recibidos: 0, error: true, peticion });
                })
            )),
            takeUntilDestroyed(destroyRef)
        ).subscribe(resultado => this.aplicar(resultado));
    }

    cargar(filtro: Filtro) {
        this.filtro = filtro;
        this.pagina = 0;
        this.paginasOcultasSeguidas = 0;
        this.pedir(false);
    }

    /** Añade la página siguiente a la lista; no hace nada si no se pagina o ya no hay más. */
    cargarMas() {
        if (!this.filtro || !this.hayMas() || this.cargandoMas()) {
            return;
        }
        this.pagina++;
        this.pedir(true);
    }

    private pedir(acumular: boolean) {
        const filtro = this.filtro!;
        this.peticiones$.next({
            params: filtroAParams(filtro, this.pagina, this.tamanoPagina),
            mostrarVencidas: filtro.mostrarVencidas ?? false,
            acumular
        });
    }

    private aplicar({ items, recibidos, error, peticion }: Resultado<T>) {
        this.cargandoMas.set(false);
        if (error) {
            // Si falla una página adicional se conserva lo que ya hay y se puede reintentar.
            if (peticion.acumular) {
                this.pagina--;
            } else {
                this.items.set([]);
                this.ocultas.set(0);
                this.hayMas.set(false);
            }
            this.errorCarga.set(true);
            this.cargando.set(false);
            return;
        }
        const previas = peticion.acumular ? this.items() : [];
        const ocultasPrevias = peticion.acumular ? this.ocultas() : 0;
        this.items.set([...previas, ...items]);
        this.ocultas.set(this.fechaFinalDe ? ocultasPrevias + Math.max(recibidos - items.length, 0) : 0);
        this.hayMas.set(this.tamanoPagina !== undefined && recibidos >= this.tamanoPagina);

        // Una página entera de vencidas no enseña nada: se sigue con la siguiente, con un tope.
        this.paginasOcultasSeguidas = items.length === 0 ? this.paginasOcultasSeguidas + 1 : 0;
        if (this.hayMas() && this.items().length === 0 && this.paginasOcultasSeguidas < MAX_PAGINAS_OCULTAS_SEGUIDAS) {
            this.cargarMas();
            return;
        }
        this.cargando.set(false);
    }
}
