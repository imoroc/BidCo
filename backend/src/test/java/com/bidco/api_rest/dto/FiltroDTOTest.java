package com.bidco.api_rest.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FiltroDTOTest {

    @Test
    void lasCategoriasSeNormalizanComoSeGuardan() {
        FiltroDTO filtro = new FiltroDTO();
        filtro.setCategorias(new String[]{"Tecnología", " Moda ", "OTROS"});

        assertArrayEquals(new String[]{"tecnologia", "moda", "otros"}, filtro.categoriasNormalizadas());
        assertFalse(filtro.sinCategorias());
    }

    @Test
    void sinCategoriasNoSeFiltraYElArrayNuncaEstaVacio() {
        FiltroDTO filtro = new FiltroDTO();

        assertTrue(filtro.sinCategorias());
        assertEquals(1, filtro.categoriasNormalizadas().length);

        filtro.setCategorias(new String[0]);
        assertTrue(filtro.sinCategorias());
    }

    @Test
    void elOrdenPorFechaNuncaEsNulo() {
        assertEquals("", new FiltroDTO().getDateOrder());
    }

    @Test
    void sinTamanoNoSePagina() {
        assertTrue(new FiltroDTO().paginacion().isUnpaged());
    }

    @Test
    void conTamanoSePaginaDesdeCero() {
        FiltroDTO filtro = new FiltroDTO();
        filtro.setPage(2);
        filtro.setSize(12);

        assertEquals(2, filtro.paginacion().getPageNumber());
        assertEquals(12, filtro.paginacion().getPageSize());
    }

    @Test
    void elTamanoSeLimitaYLaPaginaNuncaEsNegativa() {
        FiltroDTO filtro = new FiltroDTO();
        filtro.setPage(-3);
        filtro.setSize(100000);

        assertEquals(0, filtro.paginacion().getPageNumber());
        assertEquals(FiltroDTO.TAMANO_MAXIMO, filtro.paginacion().getPageSize());
    }
}
