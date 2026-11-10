import { Filtro } from '../services/auction.service';
import { consultaAFiltro, filtroAConsulta } from './filtro-url';

const CATEGORIAS = ['Tecnología', 'Hogar', 'Moda'];

describe('filtro-url', () => {
    const porDefecto: Filtro = { minPrice: 0, maxPrice: 10000, categories: [], dateOrder: '', mostrarVencidas: false };

    it('una URL sin consulta da el filtro por defecto', () => {
        expect(consultaAFiltro('', CATEGORIAS, true)).toEqual(porDefecto);
    });

    it('el filtro por defecto no escribe nada en la URL', () => {
        expect(filtroAConsulta(porDefecto)).toBe('');
    });

    it('lo que se escribe se vuelve a leer igual', () => {
        const filtro: Filtro = { minPrice: 50, maxPrice: 900, categories: ['Moda', 'Tecnología'], dateOrder: 'desc', mostrarVencidas: true };
        expect(consultaAFiltro(filtroAConsulta(filtro), CATEGORIAS, true)).toEqual(filtro);
    });

    it('ignora categorías, órdenes y números no válidos', () => {
        const filtro = consultaAFiltro('min=abc&max=&cat=Armas&cat=Hogar&cat=Hogar&orden=zzz', CATEGORIAS, true);
        expect(filtro).toEqual({ ...porDefecto, categories: ['Hogar'] });
    });

    it('acota los precios y corrige un mínimo mayor que el máximo', () => {
        expect(consultaAFiltro('min=-5&max=99999', CATEGORIAS, false)).toEqual(porDefecto);
        const invertido = consultaAFiltro('min=800&max=100', CATEGORIAS, false);
        expect([invertido.minPrice, invertido.maxPrice]).toEqual([100, 800]);
    });

    it('solo respeta «vencidas» si el listado lo permite', () => {
        expect(consultaAFiltro('vencidas=1', CATEGORIAS, false).mostrarVencidas).toBeFalse();
        expect(consultaAFiltro('vencidas=1', CATEGORIAS, true).mostrarVencidas).toBeTrue();
    });

    it('conserva los parámetros ajenos al filtro y sustituye los del filtro', () => {
        const filtro: Filtro = { ...porDefecto, dateOrder: 'asc' };
        expect(filtroAConsulta(filtro, 'q=reloj&orden=desc&min=10')).toBe('q=reloj&orden=asc');
    });

    it('un precio vacío (null) no se escribe en la URL', () => {
        const vacio = { ...porDefecto, minPrice: null as unknown as number };
        expect(filtroAConsulta(vacio)).toBe('');
    });
});
