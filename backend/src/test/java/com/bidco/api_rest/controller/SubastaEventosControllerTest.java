package com.bidco.api_rest.controller;

import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.service.impl.SubastaEventosServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP del flujo de eventos de una subasta, con el servicio real y el repositorio simulado. */
class SubastaEventosControllerTest {

    private static final Instant CIERRE = Instant.parse("2026-12-01T10:00:00Z");

    private SubastaRepository subastaRepository;
    private SubastaEventosServiceImpl servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        subastaRepository = mock(SubastaRepository.class);
        servicio = new SubastaEventosServiceImpl(subastaRepository);
        mvc = MockMvcBuilders.standaloneSetup(new SubastaEventosController(servicio))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Subasta subasta(boolean normal) {
        Subasta s = new Subasta();
        s.setSubastaID(7L);
        s.setSubastaNormal(normal);
        s.setPrecioFinal(new BigDecimal("12.50"));
        s.setFechaFinal(CIERRE);
        when(subastaRepository.findById(7L)).thenReturn(Optional.of(s));
        return s;
    }

    private MvcResult abrir() throws Exception {
        return mvc.perform(get("/api/subastas/7/eventos"))
                .andExpect(request().asyncStarted())
                .andReturn();
    }

    @Test
    void alAbrirElFlujoSeEnviaElEstadoActual() throws Exception {
        subasta(true);

        String contenido = abrir().getResponse().getContentAsString();

        assertTrue(contenido.contains("event:estado"), contenido);
        assertTrue(contenido.contains("{\"precioFinal\":12.50,\"fechaFinal\":\"2026-12-01T10:00:00Z\"}"), contenido);
    }

    @Test
    void unaPujaPublicadaLlegaALosSuscriptoresDeEsaSubasta() throws Exception {
        subasta(true);
        MvcResult flujo = abrir();

        servicio.publicarPuja(7L, new BigDecimal("20"), Instant.parse("2026-12-01T10:05:00Z"));

        String contenido = flujo.getResponse().getContentAsString();
        assertTrue(contenido.contains("{\"precioFinal\":20,\"fechaFinal\":\"2026-12-01T10:05:00Z\"}"), contenido);
    }

    @Test
    void unaPujaDeOtraSubastaNoLlega() throws Exception {
        subasta(true);
        MvcResult flujo = abrir();

        servicio.publicarPuja(8L, new BigDecimal("99"), CIERRE);

        assertFalse(flujo.getResponse().getContentAsString().contains("99"));
    }

    @Test
    void unaSubastaACiegasNoEmiteElPrecio() throws Exception {
        subasta(false);

        mvc.perform(get("/api/subastas/7/eventos")).andExpect(status().isForbidden());
        assertEquals(0, servicio.suscriptoresActivos());
    }

    @Test
    void unaSubastaInexistenteDevuelve404() throws Exception {
        when(subastaRepository.findById(7L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/subastas/7/eventos")).andExpect(status().isNotFound());
    }

    @Test
    void sePuedenAbrirVariasConexionesPeroConUnTopePorSubasta() throws Exception {
        subasta(true);
        for (int i = 0; i < SubastaEventosServiceImpl.MAX_POR_SUBASTA; i++) {
            abrir();
        }

        mvc.perform(get("/api/subastas/7/eventos")).andExpect(status().isTooManyRequests());
        assertEquals(SubastaEventosServiceImpl.MAX_POR_SUBASTA, servicio.suscriptoresActivos());
    }

    @Test
    void elLatidoNoRompeElFlujo() throws Exception {
        subasta(true);
        MvcResult flujo = abrir();

        servicio.latido();

        assertTrue(flujo.getResponse().getContentAsString().contains(":latido"));
    }
}
