import { DestroyRef, inject } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';

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

    describe('paginado', () => {
        const TAMANO = 2;
        const llamadas: HttpParams[] = [];

        /** Cada petición recibe la siguiente respuesta de la lista. */
        const crearPaginado = (respuestas: Observable<Elemento[]>[]) => {
            llamadas.length = 0;
            return TestBed.runInInjectionContext(() => new ListadoFiltrado<Elemento>(
                params => { llamadas.push(params); return respuestas[llamadas.length - 1]; },
                inject(DestroyRef), e => e.fechaFinal, TAMANO));
        };

        it('sin tamaño de página no envía page ni size ni ofrece más', () => {
            llamadas.length = 0;
            const listado = TestBed.runInInjectionContext(() => new ListadoFiltrado<Elemento>(
                params => { llamadas.push(params); return of([activa]); }, inject(DestroyRef), e => e.fechaFinal));
            listado.cargar(filtro(false));
            expect(llamadas[0].has('page')).toBeFalse();
            expect(llamadas[0].has('size')).toBeFalse();
            expect(listado.hayMas()).toBeFalse();
        });

        it('pide la primera página con su tamaño', () => {
            const listado = crearPaginado([of([activa, activa])]);
            listado.cargar(filtro(false));
            expect(llamadas[0].get('page')).toBe('0');
            expect(llamadas[0].get('size')).toBe(String(TAMANO));
        });

        it('una página completa deja pedir más y la siguiente se añade a la lista', () => {
            const listado = crearPaginado([of([activa, activa]), of([activa])]);
            listado.cargar(filtro(false));
            expect(listado.hayMas()).toBeTrue();

            listado.cargarMas();
            expect(llamadas[1].get('page')).toBe('1');
            expect(listado.items().length).toBe(3);
            expect(listado.hayMas()).toBeFalse();
        });

        it('cargar de nuevo empieza otra vez en la primera página', () => {
            const listado = crearPaginado([of([activa, activa]), of([activa, activa])]);
            listado.cargar(filtro(false));
            listado.cargar(filtro(true));
            expect(llamadas[1].get('page')).toBe('0');
            expect(listado.items().length).toBe(2);
        });

        it('una página entera de vencidas pasa sola a la siguiente', () => {
            const listado = crearPaginado([of([vencida, vencida]), of([activa])]);
            listado.cargar(filtro(false));
            expect(llamadas.length).toBe(2);
            expect(listado.items()).toEqual([activa]);
            expect(listado.ocultas()).toBe(2);
            expect(listado.cargando()).toBeFalse();
        });

        it('suma las vencidas ocultas de todas las páginas', () => {
            const listado = crearPaginado([of([activa, vencida]), of([vencida, vencida])]);
            listado.cargar(filtro(false));
            listado.cargarMas();
            expect(listado.items()).toEqual([activa]);
            expect(listado.ocultas()).toBe(3);
        });

        it('si falla una página adicional conserva la lista y permite reintentar', () => {
            spyOn(console, 'error');
            const listado = crearPaginado([of([activa, activa]), throwError(() => ({ status: 500 })), of([activa])]);
            listado.cargar(filtro(false));
            listado.cargarMas();
            expect(listado.errorCarga()).toBeTrue();
            expect(listado.items().length).toBe(2);
            expect(listado.hayMas()).toBeTrue();

            listado.cargarMas();
            expect(llamadas[2].get('page')).toBe('1');
            expect(listado.errorCarga()).toBeFalse();
            expect(listado.items().length).toBe(3);
        });
    });
});
