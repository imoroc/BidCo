/**
 * Fechas de subastas y sorteos.
 *
 * El cierre es un instante exacto. La API lo devuelve como ISO-8601 con zona (p. ej.
 * "2026-12-01T22:59:00Z"); al crear se puede enviar un día completo ("2026-12-01", que cierra
 * a la medianoche siguiente en la zona del servidor) o una hora de pared en esa zona
 * ("2026-12-01T18:30"). Estas funciones aplican en el navegador el mismo criterio que el
 * servidor, sea cual sea la zona horaria del dispositivo.
 */

/** Zona en la que el servidor calcula "hoy" (propiedad app.timezone del backend). */
export const ZONA_HORARIA = 'Europe/Madrid';

/** Margen tras la medianoche para no pedir el cierre antes de que el servidor lo acepte. */
export const MARGEN_CIERRE_MS = 5000;

const FECHA_ISO = /^(\d{4})-(\d{2})-(\d{2})$/;
const FECHA_HORA_LOCAL = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/;

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
 * Instante (ms desde epoch) correspondiente a una hora de pared de la zona del servidor.
 * La hora se localiza a partir de su equivalente en UTC; los cambios de hora ocurren de madrugada,
 * así que el desfase de ese instante es el correcto salvo entre la 1:00 y las 3:00 de esos días.
 */
function instanteEnZona(year: number, month: number, day: number, hour: number, minute: number, second: number): number {
    const nominalUtc = Date.UTC(year, month - 1, day, hour, minute, second);
    return nominalUtc - desfaseZona(nominalUtc);
}

/**
 * Instante (ms desde epoch) en el que cierra una subasta o un sorteo, sin margen. Acepta:
 * "YYYY-MM-DD" (día completo: cierra a la medianoche siguiente en la zona del servidor),
 * un instante ISO-8601 con zona u offset (se interpreta tal cual) y "YYYY-MM-DDTHH:mm[:ss]"
 * sin offset (hora de pared en la zona del servidor). Devuelve NaN si el formato no es válido.
 */
function instanteCierreMs(fechaFinal: string): number {
    const dia = FECHA_ISO.exec(fechaFinal);
    if (dia) {
        const [, year, month, day] = dia.map(Number);
        return instanteEnZona(year, month, day + 1, 0, 0, 0);
    }
    const local = FECHA_HORA_LOCAL.exec(fechaFinal);
    if (local) {
        const [, year, month, day, hour, minute] = local.map(Number);
        return instanteEnZona(year, month, day, hour, minute, Number(local[6] ?? 0));
    }
    return fechaFinal.includes('T') ? Date.parse(fechaFinal) : NaN;
}

/**
 * Instante en el que hay que pedir el cierre: el del plazo más un pequeño margen, para no
 * adelantarse al servidor. Devuelve NaN si el formato no es válido.
 */
export function cierreMs(fechaFinal: string): number {
    return instanteCierreMs(fechaFinal) + MARGEN_CIERRE_MS;
}

/**
 * Indica si una subasta o un sorteo está vencido: ya se alcanzó su hora de cierre.
 * Un valor sin fecha o con un formato no válido no se considera vencido.
 */
export function estaVencida(fechaFinal: string | null | undefined, ahora: number = ahoraMs()): boolean {
    if (!fechaFinal) {
        return false;
    }
    const cierre = instanteCierreMs(fechaFinal);
    return !Number.isNaN(cierre) && cierre <= ahora;
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
    const ahora = ahoraMs();
    return items.filter(item => !estaVencida(fechaFinalDe(item), ahora));
}
