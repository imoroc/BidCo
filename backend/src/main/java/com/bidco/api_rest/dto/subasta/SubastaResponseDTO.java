package com.bidco.api_rest.dto.subasta;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class SubastaResponseDTO {

    private Long subastaID;
    private LocalDate fechaInicial;
    private Instant fechaFinal;
    private BigDecimal precioInicial;
    private BigDecimal precioFinal;
    private boolean subastaNormal;
    private String nombreArticulo;
    private String descripcion;
    private Long creadorId;
    private String imagen;
    private String categoria;
    private Long ganador;

    public SubastaResponseDTO(Long subastaID, LocalDate fechaInicial, Instant fechaFinal, BigDecimal precioInicial,
                               BigDecimal precioFinal, boolean subastaNormal, String nombreArticulo,
                               String descripcion, Long creadorId, String imagen, String categoria, Long ganador) {
        this.subastaID = subastaID;
        this.fechaInicial = fechaInicial;
        this.fechaFinal = fechaFinal;
        this.precioInicial = precioInicial;
        this.precioFinal = precioFinal;
        this.subastaNormal = subastaNormal;
        this.nombreArticulo = nombreArticulo;
        this.descripcion = descripcion;
        this.creadorId = creadorId;
        this.imagen = imagen;
        this.categoria = categoria;
        this.ganador = ganador;
    }

    public Long getSubastaID() {
        return subastaID;
    }

    public void setSubastaID(Long subastaID) {
        this.subastaID = subastaID;
    }

    public LocalDate getFechaInicial() {
        return fechaInicial;
    }

    public void setFechaInicial(LocalDate fechaInicial) {
        this.fechaInicial = fechaInicial;
    }

    public Instant getFechaFinal() {
        return fechaFinal;
    }

    public void setFechaFinal(Instant fechaFinal) {
        this.fechaFinal = fechaFinal;
    }

    public BigDecimal getPrecioInicial() {
        return precioInicial;
    }

    public void setPrecioInicial(BigDecimal precioInicial) {
        this.precioInicial = precioInicial;
    }

    public BigDecimal getPrecioFinal() {
        return precioFinal;
    }

    public void setPrecioFinal(BigDecimal precioFinal) {
        this.precioFinal = precioFinal;
    }

    public boolean isSubastaNormal() {
        return subastaNormal;
    }

    public void setSubastaNormal(boolean subastaNormal) {
        this.subastaNormal = subastaNormal;
    }

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

    public Long getCreadorId() {
        return creadorId;
    }

    public void setCreadorId(Long creadorId) {
        this.creadorId = creadorId;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Long getGanador() {
        return ganador;
    }

    public void setGanador(Long ganador) {
        this.ganador = ganador;
    }
}
