import { TestBed } from '@angular/core/testing';

import { CREAR_ORIGEN_EVENTOS, EstadoSubasta, SubastaDirectoService, interpretarEstado } from './subasta-directo.service';

describe('interpretarEstado', () => {
    it('lee el precio y el cierre', () => {
        expect(interpretarEstado('{"precioFinal":120.5,"fechaFinal":"2026-12-01T22:59:00Z"}'))
            .toEqual({ precioFinal: 120.5, fechaFinal: '2026-12-01T22:59:00Z' });
    });

    it('admite valores nulos', () => {
        expect(interpretarEstado('{"precioFinal":null,"fechaFinal":null}')).toEqual({ precioFinal: null, fechaFinal: null });
    });

    it('descarta lo que no tiene el formato esperado', () => {
        expect(interpretarEstado('no es json')).toBeNull();
        expect(interpretarEstado('{"precioFinal":"10","fechaFinal":null}')).toBeNull();
        expect(interpretarEstado('{"precioFinal":10}')).toBeNull();
        expect(interpretarEstado(undefined)).toBeNull();
    });
});

describe('SubastaDirectoService', () => {
    let escuchas: Record<string, (evento: Event) => void>;
    let cerrado: boolean;
    let urlAbierta: string;

    beforeEach(() => {
        escuchas = {};
        cerrado = false;
        urlAbierta = '';
        const origenFalso = {
            addEventListener: (tipo: string, escucha: (evento: Event) => void) => { escuchas[tipo] = escucha; },
            close: () => { cerrado = true; }
        } as unknown as EventSource;
        TestBed.configureTestingModule({
            providers: [{ provide: CREAR_ORIGEN_EVENTOS, useValue: (url: string) => { urlAbierta = url; return origenFalso; } }]
        });
    });

    it('conecta al flujo de la subasta y emite cada estado recibido', () => {
        const recibidos: EstadoSubasta[] = [];
        TestBed.inject(SubastaDirectoService).seguir(7).subscribe(e => recibidos.push(e));

        expect(urlAbierta).toMatch(/\/subastas\/7\/eventos$/);
        escuchas['estado'](new MessageEvent('estado', { data: '{"precioFinal":50,"fechaFinal":"2026-12-01T22:59:00Z"}' }));
        escuchas['estado'](new MessageEvent('estado', { data: 'basura' }));

        expect(recibidos).toEqual([{ precioFinal: 50, fechaFinal: '2026-12-01T22:59:00Z' }]);
    });

    it('cierra la conexión al cancelar la suscripción', () => {
        const suscripcion = TestBed.inject(SubastaDirectoService).seguir(7).subscribe();
        expect(cerrado).toBeFalse();

        suscripcion.unsubscribe();
        expect(cerrado).toBeTrue();
    });
});
