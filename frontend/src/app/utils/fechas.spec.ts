import { MARGEN_CIERRE_MS, finDeDiaMs, hoyISO, mananaISO } from './fechas';

describe('fechas', () => {
    describe('hoyISO', () => {
        it('usa el día de Madrid cuando en UTC todavía es el día anterior (verano)', () => {
            // 22:30 UTC del 30 de junio son las 00:30 del 1 de julio en Madrid (UTC+2).
            expect(hoyISO(new Date('2026-06-30T22:30:00Z'))).toBe('2026-07-01');
        });

        it('usa el día de Madrid cuando en UTC todavía es el día anterior (invierno)', () => {
            // 23:30 UTC del 15 de enero son las 00:30 del 16 en Madrid (UTC+1).
            expect(hoyISO(new Date('2026-01-15T23:30:00Z'))).toBe('2026-01-16');
        });

        it('mantiene el día a media jornada', () => {
            expect(hoyISO(new Date('2026-03-10T12:00:00Z'))).toBe('2026-03-10');
        });
    });

    describe('mananaISO', () => {
        it('suma un día al día de Madrid', () => {
            expect(mananaISO(new Date('2026-06-30T22:30:00Z'))).toBe('2026-07-02');
        });

        it('resuelve el cambio de año', () => {
            expect(mananaISO(new Date('2026-12-31T12:00:00Z'))).toBe('2027-01-01');
        });
    });

    describe('finDeDiaMs', () => {
        const esperado = (isoUtc: string) => new Date(isoUtc).getTime() + MARGEN_CIERRE_MS;

        it('termina a la medianoche de Madrid en invierno', () => {
            expect(finDeDiaMs('2026-01-15')).toBe(esperado('2026-01-15T23:00:00Z'));
        });

        it('termina a la medianoche de Madrid en verano', () => {
            expect(finDeDiaMs('2026-07-15')).toBe(esperado('2026-07-15T22:00:00Z'));
        });

        it('es correcto el día anterior al cambio a horario de verano', () => {
            expect(finDeDiaMs('2026-03-28')).toBe(esperado('2026-03-28T23:00:00Z'));
        });

        it('es correcto el mismo día del cambio a horario de verano', () => {
            expect(finDeDiaMs('2026-03-29')).toBe(esperado('2026-03-29T22:00:00Z'));
        });

        it('es correcto el día anterior al cambio a horario de invierno', () => {
            expect(finDeDiaMs('2026-10-24')).toBe(esperado('2026-10-24T22:00:00Z'));
        });

        it('es correcto el mismo día del cambio a horario de invierno', () => {
            expect(finDeDiaMs('2026-10-25')).toBe(esperado('2026-10-25T23:00:00Z'));
        });

        it('resuelve el cambio de mes y de año', () => {
            expect(finDeDiaMs('2026-12-31')).toBe(esperado('2026-12-31T23:00:00Z'));
        });

        it('interpreta tal cual los valores que ya traen hora', () => {
            expect(finDeDiaMs('2026-05-01T10:00:00Z')).toBe(new Date('2026-05-01T10:00:00Z').getTime());
        });

        it('devuelve NaN si el formato no es válido', () => {
            expect(finDeDiaMs('15/01/2026')).toBeNaN();
        });
    });
});
