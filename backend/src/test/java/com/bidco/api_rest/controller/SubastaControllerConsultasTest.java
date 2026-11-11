package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.FiltroDTO;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.service.contract.SubastaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP de las consultas de subastas: filtros leídos de la URL y usuario tomado del token. */
class SubastaControllerConsultasTest {

    private static final String ATRIBUTO_USUARIO = "authenticatedUserId";

    private SubastaService subastaService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        subastaService = mock(SubastaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SubastaController(subastaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void elFiltroDeSubastasNormalesSeLeeDeLosParametrosDeLaUrl() throws Exception {
        when(subastaService.buscarPorFiltroNormal(any())).thenReturn(List.of());

        mvc.perform(get("/api/subastas/filtro-normal")
                        .param("minPrice", "5").param("maxPrice", "300")
                        .param("categorias", "tecnologia").param("categorias", "moda")
                        .param("dateOrder", "desc").param("page", "1").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        ArgumentCaptor<FiltroDTO> filtro = ArgumentCaptor.forClass(FiltroDTO.class);
        verify(subastaService).buscarPorFiltroNormal(filtro.capture());
        assertEquals(5, filtro.getValue().getMinPrice());
        assertEquals(300, filtro.getValue().getMaxPrice());
        assertArrayEquals(new String[]{"tecnologia", "moda"}, filtro.getValue().getCategorias());
        assertEquals("desc", filtro.getValue().getDateOrder());
        assertEquals(1, filtro.getValue().getPage());
        assertEquals(12, filtro.getValue().getSize());
    }

    @Test
    void elFiltroDeSubastasACiegasUsaSuPropiaConsulta() throws Exception {
        when(subastaService.buscarPorFiltroCiega(any())).thenReturn(List.of());

        mvc.perform(get("/api/subastas/filtro-ciega").param("maxPrice", "100")).andExpect(status().isOk());

        verify(subastaService).buscarPorFiltroCiega(any());
    }

    @Test
    void misSubastasUsanElUsuarioDelTokenYNoUnParametro() throws Exception {
        when(subastaService.buscarPorFiltroMisSubastas(any(), eq(3))).thenReturn(List.of());

        mvc.perform(get("/api/subastas/filtro-mis-subastas").param("id", "99").requestAttr(ATRIBUTO_USUARIO, 3L))
                .andExpect(status().isOk());

        verify(subastaService).buscarPorFiltroMisSubastas(any(), eq(3));
    }

    @Test
    void misPujasUsanElUsuarioDelTokenYNoUnParametro() throws Exception {
        when(subastaService.buscarPorFiltroMisPujas(any(), eq(3))).thenReturn(List.of());

        mvc.perform(get("/api/subastas/filtro-mis-pujas").param("id", "99").requestAttr(ATRIBUTO_USUARIO, 3L))
                .andExpect(status().isOk());

        verify(subastaService).buscarPorFiltroMisPujas(any(), eq(3));
    }

    @Test
    void laBusquedaPorNombrePasaElTermino() throws Exception {
        when(subastaService.buscarPorFiltroYNombre(any(), eq("reloj"))).thenReturn(List.of());

        mvc.perform(get("/api/subastas/filtro-nombre").param("searchTerm", "reloj")).andExpect(status().isOk());

        verify(subastaService).buscarPorFiltroYNombre(any(), eq("reloj"));
    }

    @Test
    void filtrarPorTipoPasaElIndicador() throws Exception {
        when(subastaService.buscarPorTipo(false)).thenReturn(List.of());

        mvc.perform(get("/api/subastas/filtrar").param("normal", "false")).andExpect(status().isOk());

        verify(subastaService).buscarPorTipo(false);
    }

    @Test
    void filtrarPorCreadorUsaElUsuarioDelToken() throws Exception {
        when(subastaService.listarSubastasPorCreador(3L)).thenReturn(List.of());

        mvc.perform(get("/api/subastas/filtrar-creador").requestAttr(ATRIBUTO_USUARIO, 3L))
                .andExpect(status().isOk());

        verify(subastaService).listarSubastasPorCreador(3L);
    }

    @Test
    void consultarUnaSubastaInexistenteDevuelve404() throws Exception {
        when(subastaService.buscarSubastaPorId(7L)).thenThrow(new EntityNotFoundException("Subasta no encontrada"));

        mvc.perform(get("/api/subastas/7")).andExpect(status().isNotFound());
    }

    @Test
    void finalizarUnaSubastaDevuelve200() throws Exception {
        mvc.perform(put("/api/subastas/final/7")).andExpect(status().isOk());

        verify(subastaService).asignarGanadorYActualizarPrecioFinal(7L);
    }
}
