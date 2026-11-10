package com.bidco.api_rest.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FechasCierreTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    @Test
    void unDiaCompletoCierraALaMedianocheSiguienteEnInvierno() {
        // 00:00 del 2 de diciembre en Madrid (UTC+1) son las 23:00 UTC del día 1.
        assertEquals(Instant.parse("2026-12-01T23:00:00Z"), FechasCierre.parsear("2026-12-01", MADRID));
    }

    @Test
    void unDiaCompletoCierraALaMedianocheSiguienteEnVerano() {
        // En verano Madrid va en UTC+2.
        assertEquals(Instant.parse("2026-07-01T22:00:00Z"), FechasCierre.parsear("2026-07-01", MADRID));
    }

    @Test
    void elDiaDelCambioDeHoraDeOtonoCierraEnElOffsetNuevo() {
        // El 25 de octubre de 2026 se vuelve a UTC+1: la medianoche siguiente ya es 23:00 UTC.
        assertEquals(Instant.parse("2026-10-25T23:00:00Z"), FechasCierre.parsear("2026-10-25", MADRID));
    }

    @Test
    void unInstanteConOffsetSeRespeta() {
        assertEquals(Instant.parse("2026-12-01T17:30:00Z"),
                FechasCierre.parsear("2026-12-01T18:30:00+01:00", MADRID));
    }

    @Test
    void unInstanteEnUtcSeRespeta() {
        assertEquals(Instant.parse("2026-12-01T17:30:00Z"),
                FechasCierre.parsear("2026-12-01T17:30:00Z", MADRID));
    }

    @Test
    void losEspaciosAlrededorSeIgnoran() {
        assertEquals(Instant.parse("2026-12-01T23:00:00Z"), FechasCierre.parsear(" 2026-12-01 ", MADRID));
    }

    @Test
    void sinValorSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> FechasCierre.parsear(null, MADRID));
        assertThrows(IllegalArgumentException.class, () -> FechasCierre.parsear("   ", MADRID));
    }

    @Test
    void unFormatoInvalidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () -> FechasCierre.parsear("01/12/2026", MADRID));
        assertThrows(IllegalArgumentException.class, () -> FechasCierre.parsear("2026-13-45", MADRID));
        assertThrows(IllegalArgumentException.class, () -> FechasCierre.parsear("manana por la tarde", MADRID));
    }
}
