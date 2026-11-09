package com.bidco.api_rest.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RestablecerContrasenaDTO {

    @NotBlank(message = "El enlace no es válido")
    @Size(max = 200, message = "El enlace no es válido")
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 10, max = 100, message = "La contraseña debe tener entre 10 y 100 caracteres")
    private String contrasena;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
