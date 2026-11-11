package com.bidco.api_rest.service.contract;

import com.bidco.api_rest.model.Usuario;

public interface VerificacionCorreoService {

    /** Indica si se pueden enviar correos de verificación (el envío está configurado). */
    boolean disponible();

    /** Genera un enlace de un solo uso, invalida los anteriores y lo envía en segundo plano. */
    void enviar(Usuario usuario);

    /** Reenvía el enlace a una cuenta aún sin verificar; no hace nada si ya lo está. */
    void reenviar(Long usuarioId);

    /** Confirma el correo con un token válido y no caducado, y lo invalida. */
    void verificar(String token);
}
