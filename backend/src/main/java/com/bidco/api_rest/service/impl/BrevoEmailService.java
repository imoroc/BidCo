package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.service.contract.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Envía correo por la API HTTPS de Brevo. Se usa HTTPS (443) y no SMTP porque los servicios
 * gratuitos de Render bloquean los puertos SMTP salientes.
 */
@Service
public class BrevoEmailService implements EmailService {

    private static final String ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;
    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public BrevoEmailService(
            @Value("${app.mail.brevo-api-key:}") String apiKey,
            @Value("${app.mail.from:}") String remitente,
            @Value("${app.mail.from-name:BidCo}") String nombreRemitente) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.remitente = remitente == null ? "" : remitente.trim();
        this.nombreRemitente = nombreRemitente;
    }

    @Override
    public boolean estaDisponible() {
        return !apiKey.isEmpty() && !remitente.isEmpty();
    }

    @Override
    public void enviar(String destinatario, String asunto, String contenidoHtml) {
        if (!estaDisponible()) {
            throw new IllegalStateException("El envío de correo no está configurado");
        }
        String cuerpo = "{\"sender\":{\"name\":" + json(nombreRemitente) + ",\"email\":" + json(remitente) + "},"
                + "\"to\":[{\"email\":" + json(destinatario) + "}],"
                + "\"subject\":" + json(asunto) + ","
                + "\"htmlContent\":" + json(contenidoHtml) + "}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        try {
            HttpResponse<Void> respuesta = cliente.send(peticion, HttpResponse.BodyHandlers.discarding());
            if (respuesta.statusCode() / 100 != 2) {
                throw new IllegalStateException("El proveedor de correo respondió con el estado " + respuesta.statusCode());
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo contactar con el proveedor de correo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envío de correo interrumpido", e);
        }
    }

    /** Serializa una cadena como literal JSON (sin depender de ninguna librería). */
    static String json(String valor) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : valor.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
