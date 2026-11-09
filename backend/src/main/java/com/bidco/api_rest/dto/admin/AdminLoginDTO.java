package com.bidco.api_rest.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminLoginDTO {
    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 100, message = "El usuario no es válido")
    private String username;
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 100, message = "La contraseña no es válida")
    private String password;
    private boolean rol;

    public AdminLoginDTO() {}

    public AdminLoginDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean getRol() {
        return rol;
    }

    public void setRol(boolean rol) {
        this.rol = rol;
    }
}