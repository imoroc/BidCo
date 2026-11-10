import { DOCUMENT } from '@angular/common';
import { Injectable, OnDestroy, inject, signal } from '@angular/core';

/** Cada cuánto se refrescan las cuentas atrás que solo muestran días, horas y minutos. */
export const INTERVALO_TICKER_MS = 30_000;

/**
 * Reloj compartido para las cuentas atrás del listado: un único temporizador en lugar de uno por
 * tarjeta. Se detiene mientras la pestaña está oculta y, al volver a verse, refresca al instante.
 */
@Injectable({ providedIn: 'root' })
export class TickerService implements OnDestroy {
    private readonly documento = inject(DOCUMENT);
    private temporizador: ReturnType<typeof setInterval> | null = null;
    private readonly _tick = signal(0);

    /** Cambia en cada pulso; quien la lea dentro de un computed se recalcula cuando toca. */
    readonly tick = this._tick.asReadonly();

    private readonly alCambiarVisibilidad = () => {
        if (this.documento.hidden) {
            this.detener();
        } else {
            this.pulsar();
            this.iniciar();
        }
    };

    constructor() {
        this.documento.addEventListener('visibilitychange', this.alCambiarVisibilidad);
        if (!this.documento.hidden) {
            this.iniciar();
        }
    }

    ngOnDestroy(): void {
        this.documento.removeEventListener('visibilitychange', this.alCambiarVisibilidad);
        this.detener();
    }

    private pulsar(): void {
        this._tick.update(n => n + 1);
    }

    private iniciar(): void {
        if (this.temporizador === null) {
            this.temporizador = setInterval(() => this.pulsar(), INTERVALO_TICKER_MS);
        }
    }

    private detener(): void {
        if (this.temporizador !== null) {
            clearInterval(this.temporizador);
            this.temporizador = null;
        }
    }
}
