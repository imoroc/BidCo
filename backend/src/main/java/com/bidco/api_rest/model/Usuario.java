package com.bidco.api_rest.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long usuarioID;
    @Column(unique = true, nullable = false,name = "nombre_usuario")
    private String nombreUsuario;
    @Column(unique = true,nullable = false,name = "correo_electronico")
    private String correoElectronico;
    @Column(name = "password", nullable = false)
    private String contrasena;
    @Column(nullable = false)
    private BigDecimal saldo;
    @Column(nullable = false)
    private int puntos;
    @Column(nullable = false)
    private String pais;
    @Column(nullable = false)
    private String ciudad;
    @Column(nullable = false)
    private String codigoPostal;
    @Column(nullable = false)
    private String calle;
    @Column(nullable = false)
    private int numeroPiso;
    
    private String letraPiso;

    @Column(name = "privacidad_anonimo_pujas", nullable = false)
    private boolean privacidadAnonimoPujas = false;
    
    @Column(name = "privacidad_estadisticas", nullable = false)
    private boolean privacidadEstadisticas = true;
    
    @Column(name = "privacidad_perfil_visible", nullable = false)
    private boolean privacidadPerfilVisible = true;


    // Las cuentas ya existentes y las creadas sin correo configurado cuentan como verificadas; el registro
    // normal la pone a false hasta que se pulsa el enlace del correo.
    @Column(name = "correo_verificado", nullable = false)
    private boolean correoVerificado = true;

    // Versión de sesión: ver V5__version_de_token.sql.
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @OneToMany(mappedBy = "pujador",fetch = FetchType.LAZY)
    private List<PujaSorteo> pujasSorteo;

    @OneToMany(mappedBy = "pujador",fetch = FetchType.LAZY)
    private List<Puja> pujas;

    @OneToMany(mappedBy = "creador",fetch = FetchType.LAZY)
    private List<Subasta> subastas;

    public Usuario(Long usuarioID, String nombreUsuario,
                   String correoElectronico, String contrasena, BigDecimal saldo, int puntos, String pais,
                   String ciudad, String codigoPostal, String calle, int numeroPiso, String letraPiso) {
        this.usuarioID = usuarioID;
        this.nombreUsuario = nombreUsuario;
        this.correoElectronico = correoElectronico;
        this.contrasena = contrasena;
        this.saldo = saldo;
        this.puntos = puntos;
        this.pais = pais;
        this.ciudad = ciudad;
        this.codigoPostal = codigoPostal;
        this.calle = calle;
        this.numeroPiso = numeroPiso;
        this.letraPiso = letraPiso;
        this.pujas = new ArrayList<>();
        this.subastas = new ArrayList<>();
        this.pujasSorteo = new ArrayList<>();
    }

    public Usuario() {

    }

    public Long getUsuarioID() {
        return usuarioID;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public String getContrasena() {
        return contrasena;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public int getPuntos() {
        return puntos;
    }

    public String getPais() {
        return pais;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public String getCalle() {
        return calle;
    }

    public int getNumeroPiso() {
        return numeroPiso;
    }

    public String getLetraPiso() {
        return letraPiso;
    }

    public List<Puja> getPujas() {
        return pujas;
    }

    public void setUsuarioID(Long usuarioID) {
        this.usuarioID = usuarioID;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public void setNumeroPiso(int numeroPiso) {
        this.numeroPiso = numeroPiso;
    }

    public void setLetraPiso(String letraPiso) {
        this.letraPiso = letraPiso;
    }

    public void setPujas(List<Puja> pujas) {
        this.pujas = pujas;
    }

    public List<PujaSorteo> getPujasSorteo() {
        return pujasSorteo;
    }

    public void setPujasSorteo(List<PujaSorteo> pujasSorteo) {
        this.pujasSorteo = pujasSorteo;
    }

    public List<Subasta> getSubastas() {
        return subastas;
    }

    public void setSubastas(List<Subasta> subastas) {
        this.subastas = subastas;
    }

    public int getTokenVersion() { return tokenVersion; }

    /** Invalida todos los tokens emitidos hasta ahora para esta cuenta. */
    public void invalidarSesiones() { this.tokenVersion++; }

    public boolean isCorreoVerificado() { return correoVerificado; }
    public void setCorreoVerificado(boolean correoVerificado) { this.correoVerificado = correoVerificado; }

    public Boolean getPrivacidadAnonimoPujas() { return privacidadAnonimoPujas; }
    public void setPrivacidadAnonimoPujas(Boolean v) { this.privacidadAnonimoPujas = v; }

    public Boolean getPrivacidadEstadisticas() { return privacidadEstadisticas; }
    public void setPrivacidadEstadisticas(Boolean v) { this.privacidadEstadisticas = v; }

    public Boolean getPrivacidadPerfilVisible() { return privacidadPerfilVisible; }
    public void setPrivacidadPerfilVisible(Boolean v) { this.privacidadPerfilVisible = v; }
}