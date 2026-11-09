package com.bidco.api_rest.service.contract;

public interface EmailService {

    /** Indica si el envío está configurado (clave de API y remitente). */
    boolean estaDisponible();

    /** Envía un correo HTML; lanza IllegalStateException si el proveedor no lo acepta. */
    void enviar(String destinatario, String asunto, String contenidoHtml);
}
