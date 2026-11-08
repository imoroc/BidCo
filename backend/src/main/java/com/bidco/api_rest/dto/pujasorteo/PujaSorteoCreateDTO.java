package com.bidco.api_rest.dto.pujasorteo;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PujaSorteoCreateDTO {

    @NotNull(message = "Los puntos son obligatorios")
    @DecimalMin(value = "1", message = "Los puntos deben ser mayores que 0")
    @DecimalMax(value = "1000000", message = "Los puntos son demasiado altos")
    @Digits(integer = 7, fraction = 0, message = "Los puntos deben ser un número entero")
    private BigDecimal puntos;

    // Se mantiene por compatibilidad con los clientes, pero el servidor usa su propia fecha.
    private LocalDate fecha;

    @NotNull(message = "El sorteo es obligatorio")
    private Long sorteoId;
    private Long pujadorId;

    // Constructor
    public PujaSorteoCreateDTO(BigDecimal puntos, LocalDate fecha, Long sorteoId, Long pujadorId) {
        this.puntos = puntos;
        this.fecha = fecha;
        this.sorteoId = sorteoId;
        this.pujadorId = pujadorId;
    }

    public BigDecimal getPuntos() {
        return puntos;
    }

    public void setPuntos(BigDecimal puntos) {
        this.puntos = puntos;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Long getSorteoId() {
        return sorteoId;
    }

    public void setSorteoId(Long sorteoId) {
        this.sorteoId = sorteoId;
    }

    public Long getPujadorId() {
        return pujadorId;
    }

    public void setPujadorId(Long pujadorId) {
        this.pujadorId = pujadorId;
    }
}
