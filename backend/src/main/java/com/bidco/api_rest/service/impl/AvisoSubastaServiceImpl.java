package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.service.contract.AvisoSubastaService;
import com.bidco.api_rest.service.contract.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CompletableFuture;

@Service
public class AvisoSubastaServiceImpl implements AvisoSubastaService {

    private static final Logger log = LoggerFactory.getLogger(AvisoSubastaServiceImpl.class);
    /** Dominio reservado con el que se anonimizan las cuentas eliminadas: no tiene buzón. */
    private static final String DOMINIO_ANONIMO = "@anonimo.invalid";

    private final EmailService emailService;
    private final String urlFrontend;

    public AvisoSubastaServiceImpl(
            EmailService emailService,
            @Value("${app.frontend-url:https://bid-co.vercel.app}") String urlFrontend) {
        this.emailService = emailService;
        this.urlFrontend = urlFrontend.endsWith("/") ? urlFrontend.substring(0, urlFrontend.length() - 1) : urlFrontend;
    }

    @Override
    public void pujaSuperada(Usuario superado, Subasta subasta, BigDecimal importeNuevo) {
        if (!puedeRecibirAvisos(superado)) {
            return;
        }
        String articulo = escapar(subasta.getNombreArticulo());
        String html = "<p>Otra persona ha superado tu puja en «" + articulo + "». "
                + "El precio actual es de " + formatear(importeNuevo) + ".</p>"
                + enlace(subasta, "Volver a pujar")
                + "<p>Recibes este aviso porque pujaste en esta subasta.</p>";
        enviarTrasConfirmar(superado.getCorreoElectronico(), "Te han superado en «" + subasta.getNombreArticulo() + "»", html);
    }

    @Override
    public void subastaGanada(Usuario ganador, Subasta subasta, BigDecimal importe) {
        if (!puedeRecibirAvisos(ganador)) {
            return;
        }
        String articulo = escapar(subasta.getNombreArticulo());
        String html = "<p>Has ganado la subasta de «" + articulo + "» con una puja de " + formatear(importe) + ".</p>"
                + enlace(subasta, "Ver la subasta")
                + "<p>Recibes este aviso porque ganaste una subasta de BidCo.</p>";
        enviarTrasConfirmar(ganador.getCorreoElectronico(), "Has ganado «" + subasta.getNombreArticulo() + "»", html);
    }

    @Override
    public void subastaRetiradaAlCreador(Usuario creador, Subasta subasta, String motivo) {
        if (!puedeRecibirAvisos(creador)) {
            return;
        }
        String html = "<p>La administración de BidCo ha retirado tu subasta de «" + escapar(subasta.getNombreArticulo()) + "».</p>"
                + "<p>Motivo: " + escapar(motivo) + "</p>"
                + "<p>Se te han retirado los puntos que recibiste por crearla. Si crees que ha sido un error, "
                + "puedes escribirnos desde la página de contacto.</p>";
        enviarTrasConfirmar(creador.getCorreoElectronico(), "Hemos retirado tu subasta «" + subasta.getNombreArticulo() + "»", html);
    }

    @Override
    public void subastaRetiradaAlPujador(Usuario pujador, Subasta subasta, BigDecimal devuelto) {
        if (!puedeRecibirAvisos(pujador)) {
            return;
        }
        String html = "<p>La subasta de «" + escapar(subasta.getNombreArticulo()) + "», en la que ibas ganando, "
                + "ha sido retirada por la administración de BidCo.</p>"
                + "<p>Te hemos devuelto " + formatear(devuelto) + " a tu saldo.</p>";
        enviarTrasConfirmar(pujador.getCorreoElectronico(), "Subasta retirada: «" + subasta.getNombreArticulo() + "»", html);
    }

    private boolean puedeRecibirAvisos(Usuario usuario) {
        return usuario != null
                && usuario.isCorreoVerificado()
                && usuario.getCorreoElectronico() != null
                && !usuario.getCorreoElectronico().endsWith(DOMINIO_ANONIMO)
                && emailService.estaDisponible();
    }

    private String enlace(Subasta subasta, String texto) {
        String ruta = subasta.isSubastaNormal() ? "/auction/product/" : "/blind-auctions/product/";
        return "<p><a href=\"" + urlFrontend + ruta + subasta.getSubastaID() + "\">" + texto + "</a></p>";
    }

    private static String formatear(BigDecimal importe) {
        return importe.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " €";
    }

    /** El nombre del artículo lo escribe un usuario: se escapa antes de ir dentro del HTML. */
    static String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Si hay una transacción en curso, el correo sale solo cuando se confirma: una puja que se revierte
     * no avisa a nadie. El envío es asíncrono y sus fallos no afectan a la operación.
     */
    private void enviarTrasConfirmar(String destinatario, String asunto, String html) {
        Runnable envio = () -> CompletableFuture.runAsync(() -> {
            try {
                emailService.enviar(destinatario, asunto, html);
            } catch (RuntimeException e) {
                log.warn("No se pudo enviar el aviso de subasta: {}", e.getMessage());
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }
}
