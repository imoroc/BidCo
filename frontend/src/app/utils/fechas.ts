/**
 * Fechas de subastas y sorteos.
 *
 * El servidor trabaja con días naturales (LocalDate) calculados en Europe/Madrid, y solo
 * permite cerrar una subasta o un sorteo a partir del día siguiente a su fecha final.
 * Estas funciones aplican el mismo criterio en el navegador, sea cual sea la zona
 * horaria del dispositivo.
 */

/** Zona en la que el servidor calcula "hoy" (propiedad app.timezone del backend). */
export const ZONA_HORARIA = 'Europe/Madrid';

/** Margen tras la medianoche para no pedir el cierre antes de que el servidor lo acepte. */
export const MARGEN_CIERRE_MS = 5000;

const FECHA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/;

/** Diferencia entre el reloj del servidor y el del dispositivo; 0 hasta que llega la primera respuesta. */
let desfaseServidorMs = 0;

/** Una respuesta que tarda más que esto no permite estimar bien la hora del servidor. */
const MAX_IDA_Y_VUELTA_MS = 5000;

/**
 * Ajusta el reloj con la cabecera Date de una respuesta del servidor. La cabecera solo tiene
 * precisión de segundos, así que se suma medio segundo y se compara con el punto medio de la petición.
 * Devuelve false (sin cambiar nada) si la cabecera no es válida o la respuesta tardó demasiado.
 */
export function sincronizarReloj(cabeceraDate: string | null, enviadoMs: number, recibidoMs: number): boolean {
    const servidorMs = cabeceraDate ? Date.parse(cabeceraDate) : NaN;
    if (Number.isNaN(servidorMs) || recibidoMs < enviadoMs || recibidoMs - enviadoMs > MAX_IDA_Y_VUELTA_MS) {
        return false;
    }
    desfaseServidorMs = servidorMs + 500 - (enviadoMs + recibidoMs) / 2;
    return true;
}

/** Vuelve a confiar en el reloj del dispositivo (para pruebas). */
export function reiniciarReloj(): void {
    desfaseServidorMs = 0;
}

/** Hora actual (ms desde epoch) según el servidor, con el reloj del dispositivo como respaldo. */
export function ahoraMs(): number {
    return Date.now() + desfaseServidorMs;
}

/** Descompone un instante en sus componentes de calendario en la zona del servidor. */
function componentesEnZona(instante: number): { year: number; month: number; day: number; hour: number; minute: number; second: number } {
    const partes = new Intl.DateTimeFormat('en-US', {
        timeZone: ZONA_HORARIA,
        hourCycle: 'h23',
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
    }).formatToParts(new Date(instante));
    const valor = (tipo: string) => Number(partes.find(p => p.type === tipo)?.value);
    return {
        year: valor('year'), month: valor('month'), day: valor('day'),
        hour: valor('hour'), minute: valor('minute'), second: valor('second')
    };
}

/** Fecha de hoy como YYYY-MM-DD en la zona del servidor (no en UTC ni en la del navegador). */
export function hoyISO(ahora: Date = new Date(ahoraMs())): string {
    const { year, month, day } = componentesEnZona(ahora.getTime());
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

/** Fecha de mañana como YYYY-MM-DD en la zona del servidor. */
export function mananaISO(ahora: Date = new Date(ahoraMs())): string {
    const [year, month, day] = hoyISO(ahora).split('-').map(Number);
    return new Date(Date.UTC(year, month - 1, day + 1)).toISOString().split('T')[0];
}

/** Diferencia, en ms, entre la hora de pared de la zona del servidor y UTC en ese instante. */
function desfaseZona(instante: number): number {
    const c = componentesEnZona(instante);
    return Date.UTC(c.year, c.month - 1, c.day, c.hour, c.minute, c.second) - Math.floor(instante / 1000) * 1000;
}

/**
 * Instante (ms desde epoch) en el que termina el último día de la subasta o sorteo:
 * la medianoche siguiente en la zona del servidor, más un pequeño margen.
 * Si el valor trae hora (contiene "T") se interpreta tal cual.
 * Devuelve NaN si el formato no es válido.
 */
export function finDeDiaMs(fechaFinal: string): number {
    if (fechaFinal.includes('T')) {
        return new Date(fechaFinal).getTime();
    }
    const m = FECHA_ISO.exec(fechaFinal);
    if (!m) {
        return NaN;
    }
    const [, year, month, day] = m.map(Number);
    // La medianoche local del día siguiente se localiza a partir de su equivalente en UTC;
    // los cambios de hora ocurren de madrugada, así que el desfase de ese instante es el correcto.
    const medianocheUtc = Date.UTC(year, month - 1, day + 1, 0, 0, 0);
    return medianocheUtc - desfaseZona(medianocheUtc) + MARGEN_CIERRE_MS;
}

/**
 * Indica si una subasta o un sorteo está vencido: el día actual es posterior a su fecha final.
 * El propio día de la fecha final sigue abierto, igual que en el servidor.
 */
export function estaVencida(fechaFinal: string | null | undefined, hoy: string = hoyISO()): boolean {
    return !!fechaFinal && fechaFinal.slice(0, 10) < hoy;
}

/** Quita los vencidos de un listado, salvo que se pida mostrarlos. */
export function ocultarVencidas<T>(
    items: T[],
    fechaFinalDe: (item: T) => string | null | undefined,
    mostrarVencidas: boolean
): T[] {
    if (mostrarVencidas) {
        return items;
    }
    const hoy = hoyISO();
    return items.filter(item => !estaVencida(fechaFinalDe(item), hoy));
}
