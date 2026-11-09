package com.bidco.api_rest.service.contract;

public interface RecuperacionContrasenaService {

    /**
     * Genera un enlace de un solo uso y lo envía por correo si existe una cuenta con ese
     * correo. No revela si existe: el resultado es el mismo en ambos casos.
     */
    void solicitar(String correo);

    /** Cambia la contraseña con un token válido y no caducado, y lo invalida. */
    void restablecer(String token, String nuevaContrasena);
}
