package com.bidco.api_rest.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrevoEmailServiceTest {

    @Test
    void sinClaveORemitenteNoEstaDisponible() {
        assertFalse(new BrevoEmailService("", "", "BidCo").estaDisponible());
        assertFalse(new BrevoEmailService("clave", "", "BidCo").estaDisponible());
        assertFalse(new BrevoEmailService("", "remitente@example.com", "BidCo").estaDisponible());
    }

    @Test
    void conClaveYRemitenteEstaDisponible() {
        assertTrue(new BrevoEmailService("clave", "remitente@example.com", "BidCo").estaDisponible());
    }

    @Test
    void enviarSinConfigurarFalla() {
        BrevoEmailService servicio = new BrevoEmailService("", "", "BidCo");
        assertThrows(IllegalStateException.class, () -> servicio.enviar("a@example.com", "Asunto", "<p>x</p>"));
    }

    @Test
    void jsonEscapaComillasBarrasYSaltosDeLinea() {
        assertEquals("\"a\\\"b\\\\c\\nd\"", BrevoEmailService.json("a\"b\\c\nd"));
        assertEquals("\"\\u0001\"", BrevoEmailService.json("\u0001"));
    }
}
