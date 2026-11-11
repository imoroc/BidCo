package com.bidco.api_rest.exception;

/** Se lanza cuando una acción sensible exige haber confirmado el correo electrónico. */
public class CorreoNoVerificadoException extends ForbiddenException {
    public CorreoNoVerificadoException() {
        super("Verifica tu correo electrónico para pujar, participar en sorteos o crear subastas");
    }
}
