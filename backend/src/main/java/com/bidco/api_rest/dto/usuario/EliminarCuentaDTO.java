package com.bidco.api_rest.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EliminarCuentaDTO {

    @NotBlank(message = "La contraseña es obligatoria para eliminar la cuenta")
    @Size(max = 100, message = "La contraseña no es válida")
    private String contrasena;

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
