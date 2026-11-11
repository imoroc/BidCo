package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.FiltroDTO;
import com.bidco.api_rest.dto.sorteo.SorteoResponseDTO;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.service.contract.SorteoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP de los sorteos: consulta, filtro por parámetros de la URL y cierre. */
class SorteoControllerTest {

    private SorteoService sorteoService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        sorteoService = mock(SorteoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SorteoController(sorteoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static SorteoResponseDTO sorteo() {
        return new SorteoResponseDTO(6L, "Reloj", "Funciona", LocalDate.of(2026, 11, 1),
                Instant.parse("2026-12-01T10:00:00Z"), 50, 0, 1L, null, "moda", null);
    }

    @Test
    void consultarUnSorteoLoDevuelve() throws Exception {
        when(sorteoService.buscarSorteoPorId(6L)).thenReturn(sorteo());

        mvc.perform(get("/api/sorteos/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sorteoID").value(6))
                .andExpect(jsonPath("$.puntosNecesarios").value(50));
    }

    @Test
    void consultarUnSorteoInexistenteDevuelve404() throws Exception {
        when(sorteoService.buscarSorteoPorId(6L)).thenThrow(new EntityNotFoundException("Sorteo no encontrado"));

        mvc.perform(get("/api/sorteos/6")).andExpect(status().isNotFound());
    }

    @Test
    void listarLosSorteosDevuelveTodos() throws Exception {
        when(sorteoService.obtenerTodosLosSorteos()).thenReturn(List.of(sorteo()));

        mvc.perform(get("/api/sorteos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreArticulo").value("Reloj"));
    }

    @Test
    void elFiltroSeLeeDeLosParametrosDeLaUrl() throws Exception {
        when(sorteoService.buscarPorFiltro(any())).thenReturn(List.of());

        mvc.perform(get("/api/sorteos/filtro")
                        .param("minPrice", "10").param("maxPrice", "200")
                        .param("categorias", "moda").param("categorias", "hogar")
                        .param("dateOrder", "asc").param("page", "2").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        ArgumentCaptor<FiltroDTO> filtro = ArgumentCaptor.forClass(FiltroDTO.class);
        verify(sorteoService).buscarPorFiltro(filtro.capture());
        assertEquals(10, filtro.getValue().getMinPrice());
        assertEquals(200, filtro.getValue().getMaxPrice());
        assertArrayEquals(new String[]{"moda", "hogar"}, filtro.getValue().getCategorias());
        assertEquals("asc", filtro.getValue().getDateOrder());
        assertEquals(2, filtro.getValue().getPage());
        assertEquals(12, filtro.getValue().getSize());
    }

    @Test
    void finalizarUnSorteoDevuelveElResultado() throws Exception {
        when(sorteoService.asignarGanadorYActualizarPrecioFinal(6L)).thenReturn(sorteo());

        mvc.perform(put("/api/sorteos/final/6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sorteoID").value(6));
    }

    @Test
    void finalizarUnSorteoYaCerradoDevuelve409() throws Exception {
        when(sorteoService.asignarGanadorYActualizarPrecioFinal(6L))
                .thenThrow(new IllegalStateException("El sorteo ya está cerrado"));

        mvc.perform(put("/api/sorteos/final/6")).andExpect(status().isConflict());
    }
}
