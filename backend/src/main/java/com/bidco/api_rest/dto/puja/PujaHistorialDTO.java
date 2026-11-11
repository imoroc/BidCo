package com.bidco.api_rest.dto.puja;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una puja del historial público de una subasta: el pujador va enmascarado y nunca se expone su identificador. */
public record PujaHistorialDTO(String pujador, BigDecimal importe, LocalDate fecha) {
}
