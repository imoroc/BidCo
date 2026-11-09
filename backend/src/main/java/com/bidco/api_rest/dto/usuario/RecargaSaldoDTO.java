package com.bidco.api_rest.dto.usuario;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RecargaSaldoDTO {

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
    @Digits(integer = 7, fraction = 2, message = "La cantidad admite como máximo dos decimales")
    private BigDecimal cantidad;

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }
}
