package com.bidco.api_rest.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sorteo")
public class Sorteo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sorteo_id")
    private Long sorteoID;
    @Column(nullable = false, name = "nombre_articulo")
    private String nombreArticulo;
    @Column(nullable = false)
    private String descripcion;
    @Column(nullable = false,name = "fecha_inicio")
    private LocalDate fechaInicio;
    @Column(nullable = false,name = "fecha_fin")
    private Instant fechaFin;
    @Column(nullable = false,name= "puntos_necesarios")
    private int puntosNecesarios;

    @Column(name = "puntos_finales")
    private int puntosFinales;
    @Column(nullable = false,name= "imagen")
    private String imagen;

    @Column(name = "categoria",nullable = false)
    private String categoria;

    @Column(name = "ganador_id")
    private Long ganador;

    @OneToMany(mappedBy = "sorteo", fetch = FetchType.LAZY)
    private List<PujaSorteo> pujasSorteos;

    @ManyToOne
    @JoinColumn(name = "trabajador_id")
    private Trabajador creador;

    public Sorteo(Long sorteoID, String nombreArticulo, String descripcion, LocalDate fechaInicio, Instant fechaFin, int puntosNecesarios,Trabajador creador, String imagen, String categoria) {
        this.sorteoID = sorteoID;
        this.nombreArticulo = nombreArticulo;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.puntosNecesarios = puntosNecesarios;
        this.pujasSorteos = new ArrayList<>();
        this.puntosFinales = 0;
        this.creador = creador;
        this.imagen = imagen;
        this.categoria = categoria;
        this.ganador = null;
    }
    public Sorteo() {

    }

    public Long getSorteoID() {
        return sorteoID;
    }

    public void setSorteoID(Long sorteoID) {
        this.sorteoID = sorteoID;
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

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Instant fechaFin) {
        this.fechaFin = fechaFin;
    }

    public int getPuntosNecesarios() {
        return puntosNecesarios;
    }

    public void setPuntosNecesarios(int puntosNecesarios) {
        this.puntosNecesarios = puntosNecesarios;
    }

    public List<PujaSorteo> getPujasSorteos() {
        return pujasSorteos;
    }

    public void setPujasSorteos(List<PujaSorteo> pujasSorteos) {
        this.pujasSorteos = pujasSorteos;
    }

    public Trabajador getCreador() {
        return creador;
    }

    public void setCreador(Trabajador creador) {
        this.creador = creador;
    }

    public int getPuntosFinales() {
        return puntosFinales;
    }

    public void setPuntosFinales(int puntosFinales) {
        this.puntosFinales = puntosFinales;
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
