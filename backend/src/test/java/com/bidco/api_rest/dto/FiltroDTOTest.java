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
}
