package com.bidco.api_rest.dto.subasta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Campos que el creador puede cambiar mientras la subasta no tenga pujas. */
public class SubastaUpdateDTO {

    @NotBlank
    @Size(max = 255)
    private String nombreArticulo;

    @NotBlank
    @Size(max = 5000)
    private String descripcion;

    @NotBlank
    @Size(max = 100)
    private String categoria;

    public String getNombreArticulo() {
        return nombreArticulo;
    }

    public void setNombreArticulo(String nombreArticulo) {
        this.nombreArticulo = nombreArticulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
