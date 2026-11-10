package com.bidco.api_rest.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subasta")
public class Subasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subasta_id")
    private Long subastaID;

    @Column(name = "fecha_inicial",nullable = false)
    private LocalDate fechaInicial;

    @Column(name = "fecha_final",nullable = false)
    private Instant fechaFinal;

    @Column(name = "precio_inicial")
    private BigDecimal precioInicial;

    @Column(name = "subasta_normal")
    private boolean subastaNormal;

    @Column(name = "nombre_articulo",nullable = false)
    private String nombreArticulo;

    @Column(nullable = false)
    private String descripcion;

    @Column(name = "precio_final")
    private BigDecimal precioFinal;

    @ManyToOne
    @JoinColumn(name = "creador_id")
    private Usuario creador;

    @Column(name = "imagen")
    private String imagen;

    @Column(name = "categoria",nullable = false)
    private String categoria;

    @Column(name = "ganador_id")
    private Long ganador;

    @OneToMany(mappedBy = "subasta", fetch = FetchType.LAZY)
    private List<Puja> pujas;

    @OneToMany(mappedBy = "subasta", fetch = FetchType.LAZY)
    private List<Comentario> comentarios;
    

    public Subasta(Long subastaID, LocalDate fechaInicial, Instant fechaFinal, BigDecimal precioInicial, boolean subastaNormal, String nombreArticulo, String descripcion,Usuario creador, String imagen, String categoria) {
        this.subastaID = subastaID;
        this.fechaInicial = fechaInicial;
        this.fechaFinal = fechaFinal;
        this.precioInicial = precioInicial;
        this.subastaNormal = subastaNormal;
        this.nombreArticulo = nombreArticulo;
        this.descripcion = descripcion;
        this.precioFinal = precioInicial;
        this.creador = creador;
        this.pujas = new ArrayList<>();
        this.comentarios = new ArrayList<>();
        this.imagen = imagen;
        this.categoria = categoria;
        this.ganador = null;
    }

    public Subasta() {

    }

    public Long getSubastaID() {
        return subastaID;
    }

    public List<Comentario> getComentarios() {
        return comentarios;
    }

    public Instant getFechaFinal() {
        return fechaFinal;
    }

    public LocalDate getFechaInicial() {
        return fechaInicial;
    }

    public BigDecimal getPrecioInicial() {
        return precioInicial;
    }

    public boolean isSubastaNormal() {
        return subastaNormal;
    }

    public String getNombreArticulo() {
        return nombreArticulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getPrecioFinal() {
        return precioFinal;
    }

    public Usuario getCreador() {
        return creador;
    }

    public String getImagen() {
        return imagen;
    }

    public List<Puja> getPujas() {
        return pujas;
    }

    public void setSubastaID(Long subastaID) {
        this.subastaID = subastaID;
    }

    public void setFechaInicial(LocalDate fechaInicial) {
        this.fechaInicial = fechaInicial;
    }

    public void setFechaFinal(Instant fechaFinal) {
        this.fechaFinal = fechaFinal;
    }

    public void setPrecioInicial(BigDecimal precioInicial) {
        this.precioInicial = precioInicial;
    }

    public void setSubastaNormal(boolean subastaNormal) {
        this.subastaNormal = subastaNormal;
    }

    public void setNombreArticulo(String nombreArticulo) {
        this.nombreArticulo = nombreArticulo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setPrecioFinal(BigDecimal precioFinal) {
        this.precioFinal = precioFinal;
    }

    public void setCreador(Usuario creador) {
        this.creador = creador;
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
