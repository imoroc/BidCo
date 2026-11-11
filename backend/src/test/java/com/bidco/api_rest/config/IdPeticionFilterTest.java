package com.bidco.api_rest.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdPeticionFilterTest {

    private final IdPeticionFilter filtro = new IdPeticionFilter();

    private String ejecutar(String idEnviado, AtomicReference<String> idEnElMdc) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/subastas");
        if (idEnviado != null) {
            request.addHeader(IdPeticionFilter.CABECERA, idEnviado);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain cadena = (req, res) -> idEnElMdc.set(MDC.get(IdPeticionFilter.CLAVE_MDC));

        filtro.doFilter(request, response, cadena);

        return response.getHeader(IdPeticionFilter.CABECERA);
    }

    @Test
    void generaUnIdentificadorSiLaPeticionNoLleva() throws Exception {
        AtomicReference<String> enElMdc = new AtomicReference<>();

        String enLaRespuesta = ejecutar(null, enElMdc);

        assertTrue(enLaRespuesta != null && enLaRespuesta.length() >= 8);
        assertEquals(enLaRespuesta, enElMdc.get());
    }

    @Test
    void conservaUnIdentificadorValidoDelCliente() throws Exception {
        AtomicReference<String> enElMdc = new AtomicReference<>();

        String enLaRespuesta = ejecutar("abc12345-xyz", enElMdc);

        assertEquals("abc12345-xyz", enLaRespuesta);
        assertEquals("abc12345-xyz", enElMdc.get());
    }

    @Test
    void descartaUnIdentificadorConCaracteresNoPermitidos() throws Exception {
        AtomicReference<String> enElMdc = new AtomicReference<>();
        String malicioso = "falso\r\nINFO inyectado";

        String enLaRespuesta = ejecutar(malicioso, enElMdc);

        assertNotEquals(malicioso, enLaRespuesta);
        assertEquals(enLaRespuesta, enElMdc.get());
    }

    @Test
    void descartaUnIdentificadorDemasiadoCorto() throws Exception {
        AtomicReference<String> enElMdc = new AtomicReference<>();

        String enLaRespuesta = ejecutar("a1", enElMdc);

        assertNotEquals("a1", enLaRespuesta);
    }

    @Test
    void limpiaElMdcAunqueLaCadenaFalle() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/subastas");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain cadenaQueFalla = (req, res) -> {
            throw new IllegalStateException("fallo simulado");
        };

        assertThrows(IllegalStateException.class, () -> filtro.doFilter(request, response, cadenaQueFalla));

        assertNull(MDC.get(IdPeticionFilter.CLAVE_MDC));
    }
}
