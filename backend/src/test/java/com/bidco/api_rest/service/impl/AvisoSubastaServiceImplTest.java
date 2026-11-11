package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.service.contract.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvisoSubastaServiceImplTest {

    private EmailService emailService;
    private AvisoSubastaServiceImpl avisos;
    private Subasta subasta;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        emailService = mock(EmailService.class);
        when(emailService.estaDisponible()).thenReturn(true);
        avisos = new AvisoSubastaServiceImpl(emailService, "https://ejemplo.test/");

        subasta = new Subasta();
        subasta.setSubastaID(42L);
        subasta.setSubastaNormal(true);
        subasta.setNombreArticulo("Reloj <b>antiguo</b>");

        usuario = new Usuario();
        usuario.setUsuarioID(1L);
        usuario.setCorreoElectronico("ana@correo.test");
    }

    @Test
    void elAvisoDeSuperadoLlevaElPrecioElEnlaceYElNombreEscapado() {
        avisos.pujaSuperada(usuario, subasta, new BigDecimal("20"));

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).enviar(org.mockito.ArgumentMatchers.eq("ana@correo.test"), anyString(), html.capture());
        assertTrue(html.getValue().contains("20,00 €"));
        assertTrue(html.getValue().contains("https://ejemplo.test/auction/product/42"));
        assertTrue(html.getValue().contains("Reloj &lt;b&gt;antiguo&lt;/b&gt;"));
        assertFalse(html.getValue().contains("<b>antiguo"));
    }

    @Test
    void elAvisoDeGanadorDeUnaSubastaACiegasApuntaASuRuta() {
        subasta.setSubastaNormal(false);

        avisos.subastaGanada(usuario, subasta, new BigDecimal("55.5"));

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).enviar(anyString(), anyString(), html.capture());
        assertTrue(html.getValue().contains("55,50 €"));
        assertTrue(html.getValue().contains("https://ejemplo.test/blind-auctions/product/42"));
    }

    @Test
    void sinCorreoVerificadoNoSeEnvia() {
        usuario.setCorreoVerificado(false);

        avisos.subastaGanada(usuario, subasta, BigDecimal.TEN);

        verify(emailService, after(300).never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void unaCuentaEliminadaNoRecibeAvisos() {
        usuario.setCorreoElectronico("eliminado-1@anonimo.invalid");

        avisos.pujaSuperada(usuario, subasta, BigDecimal.TEN);

        verify(emailService, after(300).never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void sinProveedorConfiguradoNoSeIntentaEnviar() {
        when(emailService.estaDisponible()).thenReturn(false);

        avisos.subastaGanada(usuario, subasta, BigDecimal.TEN);

        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void unFalloDelProveedorNoSePropaga() {
        doThrow(new IllegalStateException("caído")).when(emailService).enviar(anyString(), anyString(), anyString());

        avisos.subastaGanada(usuario, subasta, BigDecimal.TEN);

        verify(emailService, timeout(2000)).enviar(anyString(), anyString(), any());
        assertEquals("&lt;a&gt;", AvisoSubastaServiceImpl.escapar("<a>"));
    }

    @Test
    void elAvisoDeRetiradaAlCreadorIncluyeElMotivoEscapado() {
        avisos.subastaRetiradaAlCreador(usuario, subasta, "Falsificación <script>");

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).enviar(anyString(), anyString(), html.capture());
        assertTrue(html.getValue().contains("Falsificación &lt;script&gt;"));
        assertFalse(html.getValue().contains("<script>"));
    }

    @Test
    void elAvisoDeRetiradaAlPujadorIndicaLoDevuelto() {
        avisos.subastaRetiradaAlPujador(usuario, subasta, new BigDecimal("40"));

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).enviar(anyString(), anyString(), html.capture());
        assertTrue(html.getValue().contains("40,00 €"));
    }
}
