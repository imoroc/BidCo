import { Filtro } from '../services/auction.service';

/** Parámetros de la URL que pertenecen al filtro; el resto de la consulta no se toca. */
const CLAVES = ['min', 'max', 'cat', 'orden', 'vencidas'] as const;

export const PRECIO_MINIMO = 0;
export const PRECIO_MAXIMO = 10000;

const ORDENES = ['asc', 'desc'];

/**
 * Lee el filtro de una consulta de URL. Lo que no es válido (números fuera de rango, categorías o
 * órdenes desconocidos) se ignora: la URL la puede escribir cualquiera.
 */
export function consultaAFiltro(consulta: string, categoriasValidas: string[], permiteVencidas: boolean): Filtro {
    const params = new URLSearchParams(consulta);
    const numero = (clave: string, defecto: number) => {
        const texto = params.get(clave);
        const valor = texto === null || texto.trim() === '' ? NaN : Number(texto);
        return Number.isFinite(valor) ? Math.min(Math.max(valor, PRECIO_MINIMO), PRECIO_MAXIMO) : defecto;
    };
    let minPrice = numero('min', PRECIO_MINIMO);
    let maxPrice = numero('max', PRECIO_MAXIMO);
    if (minPrice > maxPrice) {
        [minPrice, maxPrice] = [maxPrice, minPrice];
    }
    const orden = params.get('orden') ?? '';
    return {
        minPrice,
        maxPrice,
        categories: [...new Set(params.getAll('cat'))].filter(c => categoriasValidas.includes(c)),
        dateOrder: ORDENES.includes(orden) ? orden : '',
        mostrarVencidas: permiteVencidas && params.get('vencidas') === '1'
    };
}

/**
 * Devuelve la consulta con el filtro aplicado, sin el signo `?`. Solo se escribe lo que se aparta de los
 * valores por defecto y los parámetros ajenos al filtro se conservan tal cual.
 */
export function filtroAConsulta(filtro: Filtro, consultaActual = ''): string {
    const params = new URLSearchParams(consultaActual);
    CLAVES.forEach(clave => params.delete(clave));
    if (Number.isFinite(filtro.minPrice) && filtro.minPrice !== PRECIO_MINIMO) {
        params.set('min', String(filtro.minPrice));
    }
    if (Number.isFinite(filtro.maxPrice) && filtro.maxPrice !== PRECIO_MAXIMO) {
        params.set('max', String(filtro.maxPrice));
    }
    filtro.categories.forEach(categoria => params.append('cat', categoria));
    if (filtro.dateOrder) {
        params.set('orden', filtro.dateOrder);
    }
    if (filtro.mostrarVencidas) {
        params.set('vencidas', '1');
    }
    return params.toString();
}
