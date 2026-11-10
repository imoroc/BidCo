package com.bidco.api_rest.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private static final int GENERAL_POR_MINUTO = 20;

    /** Reloj que avanza solo cuando la prueba lo pide. */
    private static final class RelojManual extends Clock {
        private Instant ahora = Instant.parse("2026-11-10T10:00:00Z");

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private RelojManual reloj;
    private RateLimitFilter filtro;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual();
        filtro = new RateLimitFilter(new RequestRateLimiter(reloj), true, GENERAL_POR_MINUTO);
    }

    private MockHttpServletResponse enviar(String metodo, String ruta, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filtro.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void elRegistroAdmiteCincoPeticionesPorHoraYRechazaLaSiguiente() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, enviar("POST", "/api/usuarios", "10.0.0.1").getStatus());
        }

        MockHttpServletResponse rechazada = enviar("POST", "/api/usuarios", "10.0.0.1");

        assertEquals(429, rechazada.getStatus());
        assertNotNull(rechazada.getHeader("Retry-After"));
        assertTrue(Long.parseLong(rechazada.getHeader("Retry-After")) > 0);
        assertTrue(rechazada.getContentAsString().contains("\"status\":429"));
    }

    @Test
    void cadaIpTieneSuPropioCupo() throws Exception {
        for (int i = 0; i < 5; i++) {
            enviar("POST", "/api/usuarios", "10.0.0.1");
        }

        assertEquals(429, enviar("POST", "/api/usuarios", "10.0.0.1").getStatus());
        assertEquals(200, enviar("POST", "/api/usuarios", "10.0.0.2").getStatus());
    }

    @Test
    void elCupoSeLiberaCuandoTerminaLaVentana() throws Exception {
        for (int i = 0; i < 5; i++) {
            enviar("POST", "/api/usuarios", "10.0.0.1");
        }
        assertEquals(429, enviar("POST", "/api/usuarios", "10.0.0.1").getStatus());

        reloj.avanzar(Duration.ofMinutes(61));

        assertEquals(200, enviar("POST", "/api/usuarios", "10.0.0.1").getStatus());
    }

    @Test
    void laRecargaDeSaldoSeLimitaPorIpAunqueCambieElIdDeUsuario() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, enviar("PATCH", "/api/usuarios/" + (i + 1) + "/recargar-saldo", "10.0.0.1").getStatus());
        }

        assertEquals(429, enviar("PATCH", "/api/usuarios/99/recargar-saldo", "10.0.0.1").getStatus());
    }

    @Test
    void laSubidaDeImagenesLimitaLaCreacionDeSubastas() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200, enviar("POST", "/api/subastas", "10.0.0.1").getStatus());
        }

        assertEquals(429, enviar("POST", "/api/subastas", "10.0.0.1").getStatus());
    }

    @Test
    void lasPujasDeSubastaYDeSorteoCompartenCupo() throws Exception {
        // Tope general alto para que solo actúe la regla de pujas (30 por minuto).
        filtro = new RateLimitFilter(new RequestRateLimiter(reloj), true, 1000);
        for (int i = 0; i < 15; i++) {
            enviar("POST", "/api/pujas", "10.0.0.1");
            enviar("POST", "/api/pujas-sorteo", "10.0.0.1");
        }

        assertEquals(429, enviar("POST", "/api/pujas", "10.0.0.1").getStatus());
        assertEquals(429, enviar("POST", "/api/pujas-sorteo", "10.0.0.1").getStatus());
    }

    @Test
    void unaRutaSinReglaPropiaSoloSeLimitaPorElTopeGeneral() throws Exception {
        for (int i = 0; i < GENERAL_POR_MINUTO; i++) {
            assertEquals(200, enviar("GET", "/api/subastas", "10.0.0.1").getStatus());
        }

        assertEquals(429, enviar("GET", "/api/subastas", "10.0.0.1").getStatus());
    }

    @Test
    void elPingYElPreflightDeCorsNoCuentan() throws Exception {
        for (int i = 0; i < GENERAL_POR_MINUTO * 2; i++) {
            assertEquals(200, enviar("GET", "/api/ping", "10.0.0.1").getStatus());
            assertEquals(200, enviar("OPTIONS", "/api/usuarios", "10.0.0.1").getStatus());
        }

        assertEquals(200, enviar("GET", "/api/subastas", "10.0.0.1").getStatus());
    }

    @Test
    void desactivadoNoLimitaNada() throws Exception {
        filtro = new RateLimitFilter(new RequestRateLimiter(reloj), false, 1);

        for (int i = 0; i < 50; i++) {
            assertEquals(200, enviar("POST", "/api/usuarios", "10.0.0.1").getStatus());
        }
    }
}
