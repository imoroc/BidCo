import { DestroyRef, inject } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Filtro } from '../services/auction.service';
import { ListadoFiltrado } from './listado-filtrado';

interface Elemento { fechaFinal: string; }

describe('ListadoFiltrado', () => {
    const vencida: Elemento = { fechaFinal: '2000-01-01' };
    const activa: Elemento = { fechaFinal: '2999-12-31' };

    const filtro = (mostrarVencidas: boolean): Filtro =>
        ({ minPrice: 0, maxPrice: 10000, categories: [], dateOrder: '', mostrarVencidas });

    const crear = (datos: Elemento[]) => TestBed.runInInjectionContext(
        () => new ListadoFiltrado<Elemento>(() => of(datos), inject(DestroyRef), e => e.fechaFinal));

    it('oculta las vencidas y cuenta cuántas ha ocultado', () => {
        const listado = crear([vencida, activa, vencida]);
        listado.cargar(filtro(false));
        expect(listado.items()).toEqual([activa]);
        expect(listado.ocultas()).toBe(2);
    });

    it('con todo vencido el listado queda vacío pero informa de lo oculto', () => {
        const listado = crear([vencida, vencida]);
        listado.cargar(filtro(false));
        expect(listado.items().length).toBe(0);
        expect(listado.ocultas()).toBe(2);
    });

    it('al mostrar las vencidas no hay nada oculto', () => {
        const listado = crear([vencida, activa]);
        listado.cargar(filtro(true));
        expect(listado.items().length).toBe(2);
        expect(listado.ocultas()).toBe(0);
    });
});
