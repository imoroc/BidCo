package com.bidco.api_rest.dto.admin;

import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;

public class SorteoCreateDTO {

    private String nombreArticulo;
    private String descripcion;
    private String categoria;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fechaInicio;

    private String fechaFin;

    private Integer puntosNecesarios;
    private MultipartFile imagen;

    // Getters y Setters
    public String getNombreArticulo() { return nombreArticulo; }
    public void setNombreArticulo(String nombreArticulo) { this.nombreArticulo = nombreArticulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public String getFechaFin() { return fechaFin; }
    public void setFechaFin(String fechaFin) { this.fechaFin = fechaFin; }

    public Integer getPuntosNecesarios() { return puntosNecesarios; }
    public void setPuntosNecesarios(Integer puntosNecesarios) { this.puntosNecesarios = puntosNecesarios; }

    public MultipartFile getImagen() { return imagen; }
    public void setImagen(MultipartFile imagen) { this.imagen = imagen; }

    public String getCategoria() { return categoria; }

    public void setCategoria(String categoria) { this.categoria = categoria; }
}