import { MARGEN_CIERRE_MS, estaVencida, finDeDiaMs, hoyISO, mananaISO, ocultarVencidas } from './fechas';

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

    describe('finDeDiaMs', () => {
        // Cada caso: fecha final y el instante UTC de la medianoche de Madrid que la cierra.
        const casos: Caso[] = [
            ['termina a la medianoche de Madrid en invierno', '2026-01-15', '2026-01-15T23:00:00Z'],
            ['termina a la medianoche de Madrid en verano', '2026-07-15', '2026-07-15T22:00:00Z'],
            ['es correcto el día anterior al cambio a horario de verano', '2026-03-28', '2026-03-28T23:00:00Z'],
            ['es correcto el mismo día del cambio a horario de verano', '2026-03-29', '2026-03-29T22:00:00Z'],
            ['es correcto el día anterior al cambio a horario de invierno', '2026-10-24', '2026-10-24T22:00:00Z'],
            ['es correcto el mismo día del cambio a horario de invierno', '2026-10-25', '2026-10-25T23:00:00Z'],
            ['resuelve el cambio de mes y de año', '2026-12-31', '2026-12-31T23:00:00Z'],
        ];

        casos.forEach(([descripcion, fecha, cierreUtc]) => {
            it(descripcion, () => {
                expect(finDeDiaMs(fecha)).toBe(new Date(cierreUtc).getTime() + MARGEN_CIERRE_MS);
            });
        });

        it('interpreta tal cual los valores que ya traen hora', () => {
            expect(finDeDiaMs('2026-05-01T10:00:00Z')).toBe(new Date('2026-05-01T10:00:00Z').getTime());
        });

        it('devuelve NaN si el formato no es válido', () => {
            expect(finDeDiaMs('15/01/2026')).toBeNaN();
        });
    });

    describe('estaVencida', () => {
        type CasoVencida = [descripcion: string, fechaFinal: string | null | undefined, esperado: boolean];
        const casos: CasoVencida[] = [
            ['es posterior a la fecha final', '2026-06-14', true],
            ['el propio día de la fecha final sigue abierta', '2026-06-15', false],
            ['una fecha futura no está vencida', '2026-06-16', false],
            ['ignora la hora si la fecha la trae', '2026-06-14T10:00:00', true],
            ['sin fecha no se considera vencida', undefined, false],
        ];

        casos.forEach(([descripcion, fechaFinal, esperado]) => {
            it(descripcion, () => expect(estaVencida(fechaFinal, '2026-06-15')).toBe(esperado));
        });
    });

    describe('ocultarVencidas', () => {
        const hoy = hoyISO();
        const ayer = new Date(Date.UTC(+hoy.slice(0, 4), +hoy.slice(5, 7) - 1, +hoy.slice(8, 10) - 1)).toISOString().slice(0, 10);
        const items = [{ id: 1, fin: ayer }, { id: 2, fin: hoy }, { id: 3, fin: mananaISO() }];
        const fin = (item: { fin: string }) => item.fin;

        it('oculta los vencidos por defecto', () => {
            expect(ocultarVencidas(items, fin, false).map(i => i.id)).toEqual([2, 3]);
        });

        it('los conserva cuando se piden', () => {
            expect(ocultarVencidas(items, fin, true).map(i => i.id)).toEqual([1, 2, 3]);
        });
    });
});
