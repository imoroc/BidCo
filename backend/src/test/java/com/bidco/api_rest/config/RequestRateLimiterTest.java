package com.bidco.api_rest.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RequestRateLimiterTest {

    private static final Duration MINUTO = Duration.ofMinutes(1);

    private RelojManual reloj;
    private RequestRateLimiter limitador;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual();
        limitador = new RequestRateLimiter(reloj);
    }

    @Test
    void permiteHastaElMaximoDentroDeLaVentana() {
        for (int i = 0; i < 3; i++) {
            assertEquals(0, limitador.tryAcquire("ip", 3, MINUTO));
        }
    }

    @Test
    void laPeticionQueSuperaElMaximoDevuelveLosSegundosDeEspera() {
        for (int i = 0; i < 3; i++) {
            limitador.tryAcquire("ip", 3, MINUTO);
        }
        reloj.avanzar(Duration.ofSeconds(20));

        assertEquals(40, limitador.tryAcquire("ip", 3, MINUTO));
    }

    @Test
    void laEsperaSeRedondeaHaciaArribaYNuncaEsCero() {
        limitador.tryAcquire("ip", 1, MINUTO);
        reloj.avanzar(Duration.ofMillis(59_900));

        assertEquals(1, limitador.tryAcquire("ip", 1, MINUTO));
    }

    @Test
    void unaPeticionBloqueadaNoAmpliaLaVentana() {
        limitador.tryAcquire("ip", 1, MINUTO);
        limitador.tryAcquire("ip", 1, MINUTO);
        reloj.avanzar(Duration.ofSeconds(30));

        assertEquals(30, limitador.tryAcquire("ip", 1, MINUTO));
    }

    @Test
    void alTerminarLaVentanaVuelveAPermitir() {
        limitador.tryAcquire("ip", 1, MINUTO);
        assertTrue(limitador.tryAcquire("ip", 1, MINUTO) > 0);

        reloj.avanzar(MINUTO);

        assertEquals(0, limitador.tryAcquire("ip", 1, MINUTO));
    }

    @Test
    void cadaClaveTieneSuPropioCupo() {
        limitador.tryAcquire("a", 1, MINUTO);

        assertTrue(limitador.tryAcquire("a", 1, MINUTO) > 0);
        assertEquals(0, limitador.tryAcquire("b", 1, MINUTO));
    }
}
