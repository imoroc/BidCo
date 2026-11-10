package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.exception.ServicioNoDisponibleException;
import com.bidco.api_rest.model.TokenVerificacionCorreo;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.TokenVerificacionCorreoRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.EmailService;
import com.bidco.api_rest.service.contract.VerificacionCorreoService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

@Service
public class VerificacionCorreoServiceImpl implements VerificacionCorreoService {

    static final Duration VALIDEZ = Duration.ofHours(24);
    private static final String ENLACE_INVALIDO = "El enlace no es válido o ha caducado";
    private static final Logger log = LoggerFactory.getLogger(VerificacionCorreoServiceImpl.class);

    private final SecureRandom aleatorio = new SecureRandom();
    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacionCorreoRepository tokenRepository;
    private final EmailService emailService;
    private final Clock clock;
    private final String urlFrontend;

    public VerificacionCorreoServiceImpl(
            UsuarioRepository usuarioRepository,
            TokenVerificacionCorreoRepository tokenRepository,
            EmailService emailService,
            Clock clock,
            @Value("${app.frontend-url:https://bid-co.vercel.app}") String urlFrontend) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.clock = clock;
        this.urlFrontend = urlFrontend.endsWith("/") ? urlFrontend.substring(0, urlFrontend.length() - 1) : urlFrontend;
    }

    @Override
    public boolean disponible() {
        return emailService.estaDisponible();
    }

    @Override
    public void enviar(Usuario usuario) {
        tokenRepository.eliminarCaducados(clock.instant());
        tokenRepository.eliminarPorUsuario(usuario.getUsuarioID());
        String token = generarToken();
        tokenRepository.save(new TokenVerificacionCorreo(
                usuario.getUsuarioID(),
                RecuperacionContrasenaServiceImpl.hashDeToken(token),
                clock.instant().plus(VALIDEZ)));
        enviarEnSegundoPlano(usuario.getCorreoElectronico(), token);
    }

    @Override
    public void reenviar(Long usuarioId) {
        if (!disponible()) {
            throw new ServicioNoDisponibleException(
                    "La verificación del correo no está disponible en este momento. Contacta con soporte.");
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (!usuario.isCorreoVerificado()) {
            enviar(usuario);
        }
    }

    @Override
    @Transactional
    public void verificar(String token) {
        TokenVerificacionCorreo registro = tokenRepository
                .findByTokenHash(RecuperacionContrasenaServiceImpl.hashDeToken(token))
                .filter(t -> t.getExpiraEn().isAfter(clock.instant()))
                .orElseThrow(() -> new IllegalArgumentException(ENLACE_INVALIDO));

        Usuario usuario = usuarioRepository.findByIdForUpdate(registro.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException(ENLACE_INVALIDO));
        usuario.setCorreoVerificado(true);
        usuarioRepository.save(usuario);
        // Un solo uso: se invalidan este y cualquier otro enlace pendiente de la cuenta.
        tokenRepository.eliminarPorUsuario(usuario.getUsuarioID());
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Un fallo del proveedor se registra sin datos sensibles y no impide el registro ni el cambio de correo. */
    private void enviarEnSegundoPlano(String destinatario, String token) {
        String enlace = urlFrontend + "/verificar-correo?token=" + token;
        String html = "<p>Confirma tu correo electrónico para activar todas las funciones de tu cuenta de BidCo.</p>"
                + "<p><a href=\"" + enlace + "\">Verificar mi correo</a></p>"
                + "<p>El enlace caduca en " + VALIDEZ.toHours() + " horas y solo se puede usar una vez. "
                + "Si no has creado tú esta cuenta, ignora este mensaje.</p>";
        CompletableFuture.runAsync(() -> {
            try {
                emailService.enviar(destinatario, "Verifica tu correo de BidCo", html);
            } catch (RuntimeException e) {
                log.warn("No se pudo enviar el correo de verificación: {}", e.getMessage());
            }
        });
    }
}
