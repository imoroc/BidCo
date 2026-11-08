package com.bidco.api_rest.dto.puja;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PujaCreateDTO {

    @NotNull(message = "El importe es obligatorio")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor que 0")
    @DecimalMax(value = "1000000000", message = "El importe es demasiado alto")
    @Digits(integer = 10, fraction = 2, message = "El importe admite como máximo 2 decimales")
    private BigDecimal importe;

    // Se mantiene por compatibilidad con los clientes, pero el servidor usa su propia fecha.
    private LocalDate fecha;

    @NotNull(message = "La subasta es obligatoria")
    private Long subastaId;
    private Long pujadorId;

    // Constructor
    public PujaCreateDTO(BigDecimal importe, LocalDate fecha, Long subastaId, Long pujadorId) {
        this.importe = importe;
        this.fecha = fecha;
        this.subastaId = subastaId;
        this.pujadorId = pujadorId;
    }

    // Getters y Setters
    public BigDecimal getImporte() {
        return importe;
    }

    public void setImporte(BigDecimal importe) {
        this.importe = importe;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Long getSubastaID() {
        return subastaId;
    }

    public void setSubastaID(Long subastaId) {
        this.subastaId = subastaId;
    }

    public Long getPujadorId() {
        return pujadorId;
    }

    public void setPujadorId(Long pujadorId) {
        this.pujadorId = pujadorId;
    }
}

