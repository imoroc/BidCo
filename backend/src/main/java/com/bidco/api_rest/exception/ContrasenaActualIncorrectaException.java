package com.bidco.api_rest.exception;

/** Contraseña actual errónea al cambiarla; sigue respondiendo 400 pero permite contar los fallos. */
public class ContrasenaActualIncorrectaException extends IllegalArgumentException {
    public ContrasenaActualIncorrectaException(String message) {
        super(message);
    }
}
