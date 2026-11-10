package com.bidco.api_rest.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.Instant;

/**
 * Interpreta la fecha de cierre que envía el cliente al crear una subasta o un sorteo.
 *
 * <p>Se aceptan dos formatos:
 * <ul>
 *   <li>{@code YYYY-MM-DD}: el día completo. El cierre es la medianoche siguiente en la zona
 *       indicada, de modo que se puede participar durante todo ese día (comportamiento anterior).</li>
 *   <li>Un instante ISO-8601 con zona u offset, p. ej. {@code 2026-12-01T18:30:00+01:00}.</li>
 *   <li>Fecha y hora sin offset, p. ej. {@code 2026-12-01T18:30}: se interpreta como hora de pared
 *       en la zona indicada, que es lo que elige el usuario en el formulario.</li>
 * </ul>
 */
public final class FechasCierre {

    private FechasCierre() {
    }

    public static Instant parsear(String valor, ZoneId zona) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La fecha final es obligatoria");
        }
        String texto = valor.trim();
        try {
            if (texto.length() == 10) {
                return LocalDate.parse(texto).plusDays(1).atStartOfDay(zona).toInstant();
            }
            return parsearConHora(texto, zona);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha final no tiene un formato válido", e);
        }
    }

    private static Instant parsearConHora(String texto, ZoneId zona) {
        try {
            return OffsetDateTime.parse(texto).toInstant();
        } catch (DateTimeParseException sinOffset) {
            return LocalDateTime.parse(texto).atZone(zona).toInstant();
        }
    }
}
