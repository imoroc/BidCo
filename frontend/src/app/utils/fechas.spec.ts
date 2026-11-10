import { MARGEN_CIERRE_MS, ahoraMs, cierreMs, estaVencida, hoyISO, mananaISO, ocultarVencidas, reiniciarReloj, sincronizarReloj } from './fechas';

type Caso = [descripcion: string, entrada: string, esperado: string];

describe('fechas', () => {
    describe('hoyISO', () => {
        const casos: Caso[] = [
            // 22:30 UTC del 30 de junio son las 00:30 del 1 de julio en Madrid (UTC+2).
            ['usa el día de Madrid cuando en UTC todavía es el día anterior (verano)', '2026-06-30T22:30:00Z', '2026-07-01'],
            // 23:30 UTC del 15 de enero son las 00:30 del 16 en Madrid (UTC+1).
            ['usa el día de Madrid cuando en UTC todavía es el día anterior (invierno)', '2026-01-15T23:30:00Z', '2026-01-16'],
            ['mantiene el día a media jornada', '2026-03-10T12:00:00Z', '2026-03-10'],
        ];

        casos.forEach(([descripcion, instante, dia]) => {
            it(descripcion, () => expect(hoyISO(new Date(instante))).toBe(dia));
        });
    });

    describe('mananaISO', () => {
        const casos: Caso[] = [
            ['suma un día al día de Madrid', '2026-06-30T22:30:00Z', '2026-07-02'],
            ['resuelve el cambio de año', '2026-12-31T12:00:00Z', '2027-01-01'],
        ];

        casos.forEach(([descripcion, instante, dia]) => {
            it(descripcion, () => expect(mananaISO(new Date(instante))).toBe(dia));
        });
    });

    describe('cierreMs', () => {
        // Cada caso: fecha final y el instante UTC de la medianoche de Madrid que la cierra.
        const casos: Caso[] = [
            ['termina a la medianoche de Madrid en invierno', '2026-01-15', '2026-01-15T23:00:00Z'],
            ['termina a la medianoche de Madrid en verano', '2026-07-15', '2026-07-15T22:00:00Z'],
            ['es correcto el día anterior al cambio a horario de verano', '2026-03-28', '2026-03-28T23:00:00Z'],
            ['es correcto el mismo día del cambio a horario de verano', '2026-03-29', '2026-03-29T22:00:00Z'],
            ['es correcto el día anterior al cambio a horario de invierno', '2026-10-24', '2026-10-24T22:00:00Z'],
            ['es correcto el mismo día del cambio a horario de invierno', '2026-10-25', '2026-10-25T23:00:00Z'],
            ['resuelve el cambio de mes y de año', '2026-12-31', '2026-12-31T23:00:00Z'],
            ['una hora de pared sin offset es la de Madrid en invierno', '2026-01-15T18:30', '2026-01-15T17:30:00Z'],
            ['una hora de pared sin offset es la de Madrid en verano', '2026-07-15T18:30', '2026-07-15T16:30:00Z'],
            ['admite segundos en la hora de pared', '2026-01-15T18:30:15', '2026-01-15T17:30:15Z'],
        ];

        casos.forEach(([descripcion, fecha, cierreUtc]) => {
            it(descripcion, () => {
                expect(cierreMs(fecha)).toBe(new Date(cierreUtc).getTime() + MARGEN_CIERRE_MS);
            });
        });

        it('interpreta tal cual los instantes con zona y suma el margen', () => {
            expect(cierreMs('2026-05-01T10:00:00Z')).toBe(new Date('2026-05-01T10:00:00Z').getTime() + MARGEN_CIERRE_MS);
            expect(cierreMs('2026-05-01T12:00:00+02:00')).toBe(new Date('2026-05-01T10:00:00Z').getTime() + MARGEN_CIERRE_MS);
        });

        it('devuelve NaN si el formato no es válido', () => {
            expect(cierreMs('15/01/2026')).toBeNaN();
            expect(cierreMs('mañana T tarde')).toBeNaN();
        });
    });

    describe('estaVencida', () => {
        // 2026-06-15 12:00 en Madrid.
        const ahora = Date.parse('2026-06-15T10:00:00Z');
        type CasoVencida = [descripcion: string, fechaFinal: string | null | undefined, esperado: boolean];
        const casos: CasoVencida[] = [
            ['un día completo anterior está vencido', '2026-06-14', true],
            ['el propio día de la fecha final sigue abierto', '2026-06-15', false],
            ['una fecha futura no está vencida', '2026-06-16', false],
            ['una hora de hoy que ya pasó está vencida', '2026-06-15T11:00:00+02:00', true],
            ['una hora de hoy que falta por llegar no lo está', '2026-06-15T13:00:00+02:00', false],
            ['justo a la hora de cierre ya está vencida', '2026-06-15T12:00:00+02:00', true],
            ['la hora sin offset se interpreta en Madrid (ya pasada)', '2026-06-15T11:00', true],
            ['la hora sin offset se interpreta en Madrid (por llegar)', '2026-06-15T13:00', false],
            ['sin fecha no se considera vencida', undefined, false],
            ['un formato inválido no se considera vencido', '15/01/2026', false],
        ];

        casos.forEach(([descripcion, fechaFinal, esperado]) => {
            it(descripcion, () => expect(estaVencida(fechaFinal, ahora)).toBe(esperado));
        });
    });

    describe('ocultarVencidas', () => {
        const fin = (item: { fin: string }) => item.fin;
        // Se calculan al ejecutar cada prueba, para no depender del reloj sincronizado por otras.
        const crearItems = () => {
            const hoy = hoyISO();
            const ayer = new Date(Date.UTC(+hoy.slice(0, 4), +hoy.slice(5, 7) - 1, +hoy.slice(8, 10) - 1)).toISOString().slice(0, 10);
            return [
                { id: 1, fin: ayer },
                { id: 2, fin: hoy },
                { id: 3, fin: mananaISO() },
                { id: 4, fin: new Date(ahoraMs() - 3600000).toISOString() },
                { id: 5, fin: new Date(ahoraMs() + 3600000).toISOString() },
            ];
        };

        it('oculta los vencidos por defecto', () => {
            expect(ocultarVencidas(crearItems(), fin, false).map(i => i.id)).toEqual([2, 3, 5]);
        });

        it('los conserva cuando se piden', () => {
            expect(ocultarVencidas(crearItems(), fin, true).map(i => i.id)).toEqual([1, 2, 3, 4, 5]);
        });
    });

    describe('reloj del servidor', () => {
        afterEach(() => reiniciarReloj());

        it('sin sincronizar usa el reloj del dispositivo', () => {
            const antes = Date.now();
            expect(ahoraMs()).toBeGreaterThanOrEqual(antes);
            expect(ahoraMs() - antes).toBeLessThan(1000);
        });

        it('se adelanta si el servidor va por delante del dispositivo', () => {
            const ahora = Date.now();
            const servidor = new Date(ahora + 60_000).toUTCString();
            expect(sincronizarReloj(servidor, ahora, ahora)).toBeTrue();
            expect(ahoraMs() - ahora).toBeGreaterThan(59_000);
            expect(ahoraMs() - ahora).toBeLessThan(62_000);
        });

        it('ignora una cabecera inválida o una respuesta demasiado lenta', () => {
            const ahora = Date.now();
            expect(sincronizarReloj(null, ahora, ahora)).toBeFalse();
            expect(sincronizarReloj('no es una fecha', ahora, ahora)).toBeFalse();
            expect(sincronizarReloj(new Date(ahora).toUTCString(), ahora, ahora + 10_000)).toBeFalse();
        });
    });
});
