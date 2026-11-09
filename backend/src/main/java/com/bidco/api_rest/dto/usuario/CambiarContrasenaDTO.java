package com.bidco.api_rest.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CambiarContrasenaDTO {

    @NotBlank(message = "La contraseña actual es obligatoria")
    @Size(max = 100, message = "La contraseña actual no es válida")
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 10, max = 100, message = "La nueva contraseña debe tener entre 10 y 100 caracteres")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
