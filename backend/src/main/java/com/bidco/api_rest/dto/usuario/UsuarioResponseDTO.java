package com.bidco.api_rest.dto.usuario;

import java.math.BigDecimal; // Asegúrate de importar BigDecimal

public class UsuarioResponseDTO {

    private Long usuarioID;
    private String nombreUsuario;
    private String correoElectronico;
    private String pais;
    private String ciudad;
    private String codigoPostal;
    private String calle;
    private int numeroPiso;
    private String letraPiso; // Puede ser null si no aplica
    private BigDecimal saldo; // Añadido por si lo necesitas mostrar
    private int puntos;       // Añadido por si lo necesitas mostrar
    private boolean correoVerificado;

    // --- Constructor actualizado ---
    public UsuarioResponseDTO(Long usuarioID, String nombreUsuario, String correoElectronico,
                              String pais, String ciudad, String codigoPostal, String calle,
                              int numeroPiso, String letraPiso, BigDecimal saldo, int puntos) {
        this.usuarioID = usuarioID;
        this.nombreUsuario = nombreUsuario;
        this.correoElectronico = correoElectronico;
        this.pais = pais;
        this.ciudad = ciudad;
        this.codigoPostal = codigoPostal;
        this.calle = calle;
        this.numeroPiso = numeroPiso;
        this.letraPiso = letraPiso;
        this.saldo = saldo;
        this.puntos = puntos;
    }

    public Long getUsuarioID() { return usuarioID; }
    public void setUsuarioID(Long usuarioID) { this.usuarioID = usuarioID; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }

    public int getNumeroPiso() { return numeroPiso; }
    public void setNumeroPiso(int numeroPiso) { this.numeroPiso = numeroPiso; }

    public String getLetraPiso() { return letraPiso; }
    public void setLetraPiso(String letraPiso) { this.letraPiso = letraPiso; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public int getPuntos() { return puntos; }
    public void setPuntos(int puntos) { this.puntos = puntos; }

    public boolean isCorreoVerificado() { return correoVerificado; }
    public void setCorreoVerificado(boolean correoVerificado) { this.correoVerificado = correoVerificado; }
}
