import { Injectable, signal } from '@angular/core';

export type TipoToast = 'exito' | 'error' | 'info';

export interface Toast {
  id: number;
  tipo: TipoToast;
  mensaje: string;
}

/** Avisos breves no bloqueantes. Sustituyen a alert(). */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private siguienteId = 1;
  private readonly _toasts = signal<Toast[]>([]);
  readonly toasts = this._toasts.asReadonly();

  exito(mensaje: string, duracionMs = 4000): void {
    this.mostrar('exito', mensaje, duracionMs);
  }

  error(mensaje: string, duracionMs = 6000): void {
    this.mostrar('error', mensaje, duracionMs);
  }

  info(mensaje: string, duracionMs = 4000): void {
    this.mostrar('info', mensaje, duracionMs);
  }

  cerrar(id: number): void {
    this._toasts.update(lista => lista.filter(t => t.id !== id));
  }

  private mostrar(tipo: TipoToast, mensaje: string, duracionMs: number): void {
    const id = this.siguienteId++;
    this._toasts.update(lista => [...lista, { id, tipo, mensaje }]);
    setTimeout(() => this.cerrar(id), duracionMs);
  }
}
