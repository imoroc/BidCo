package com.bidco.api_rest.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class UsuarioCreateDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre de usuario debe tener entre 3 y 100 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    @Size(max = 254)
    private String correoElectronico;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 10, max = 100, message = "La contraseña debe tener entre 10 y 100 caracteres")
    private String contraseña;

    @NotBlank(message = "El país es obligatorio")
    private String pais;

    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;

    @NotBlank(message = "El código postal es obligatorio")
    @Size(max = 20)
    private String codigoPostal;

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 255)
    private String calle;

    @NotNull(message = "El número de piso es obligatorio")
    @PositiveOrZero(message = "El número de piso no puede ser negativo")
    private Integer numeroPiso;

    private String letraPiso;

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getCorreoElectronico() { return correoElectronico; }
    public void setCorreoElectronico(String correoElectronico) { this.correoElectronico = correoElectronico; }
    public String getContraseña() { return contraseña; }
    public void setContraseña(String contraseña) { this.contraseña = contraseña; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }
    public String getCalle() { return calle; }
    public void setCalle(String calle) { this.calle = calle; }
    public Integer getNumeroPiso() { return numeroPiso; }
    public void setNumeroPiso(Integer numeroPiso) { this.numeroPiso = numeroPiso; }
    public String getLetraPiso() { return letraPiso; }
    public void setLetraPiso(String letraPiso) { this.letraPiso = letraPiso; }
}
