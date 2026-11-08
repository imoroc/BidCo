package com.bidco.api_rest.dto.cometario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ComentarioCreateDTO {

    @NotBlank(message = "El comentario es obligatorio")
    @Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres")
    private String comentario;

    @NotNull(message = "El precio estimado es obligatorio")
    @PositiveOrZero(message = "El precio estimado no puede ser negativo")
    private BigDecimal precioEstimado;

    // Se ignora al guardar: el autor es el trabajador autenticado.
    private Long trabajadorId;

    @NotNull(message = "La subasta es obligatoria")
    private Long subastaID;
    // Constructor
    public ComentarioCreateDTO(String comentario, BigDecimal precioEstimado, Long trabajadorId, Long subastaID) {
        this.comentario = comentario;
        this.precioEstimado = precioEstimado;
        this.trabajadorId = trabajadorId;
        this.subastaID = subastaID;
    }

    // Getters y Setters
    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Long getTrabajadorId() {
        return trabajadorId;
    }

    public void setTrabajadorId(Long trabajadorId) {
        this.trabajadorId = trabajadorId;
    }

    public Long getSubastaID() {
        return subastaID;
    }

    public void setSubastaID(Long subastaID) {
        this.subastaID = subastaID;
    }

    public BigDecimal getPrecioEstimado() {
        return precioEstimado;
    }

    public void setPrecioEstimado(BigDecimal precioEstimado) {
        this.precioEstimado = precioEstimado;
    }
}