package com.bidco.api_rest.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VerificarCorreoDTO {

    @NotBlank(message = "El enlace no es válido")
    @Size(max = 200, message = "El enlace no es válido")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
