import { HttpParams } from '@angular/common/http';
import { DestroyRef, Directive, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { Filtro, SubastaResponseDTO } from '../services/auction.service';
import { ListadoFiltrado, TAMANO_PAGINA } from './listado-filtrado';

/**
 * Historial paginado de subastas del usuario con sesión (sus pujas, sus subastas). Muestra también las
 * vencidas. Cada vista indica solo qué endpoint consulta.
 */
@Directive()
export abstract class HistorialSubastas {
    private readonly authService = inject(AuthService);
    private filtro?: Filtro;
    userId: number | null = null;

    readonly listado = new ListadoFiltrado<SubastaResponseDTO>(
        params => this.pedir(params, this.userId!), inject(DestroyRef), undefined, TAMANO_PAGINA);

    /** Petición de una página del historial del usuario `usuarioId`. */
    protected abstract pedir(params: HttpParams, usuarioId: number): Observable<SubastaResponseDTO[]>;

    ngOnInit() {
        this.authService.currentUser$.subscribe(user => {
            if (user && user.usuarioID !== this.userId) {
                this.userId = user.usuarioID;
                // El filtro puede haber llegado antes que el usuario.
                if (this.filtro) this.listado.cargar(this.filtro);
            }
        });
    }

    handleFilterApplied(filtro: Filtro) {
        this.filtro = filtro;
        if (this.userId !== null) this.listado.cargar(filtro);
    }
}
