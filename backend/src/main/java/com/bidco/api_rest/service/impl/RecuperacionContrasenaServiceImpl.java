package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.exception.ServicioNoDisponibleException;
import com.bidco.api_rest.model.TokenRecuperacion;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.TokenRecuperacionRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.EmailService;
import com.bidco.api_rest.service.contract.RecuperacionContrasenaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

@Service
public class RecuperacionContrasenaServiceImpl implements RecuperacionContrasenaService {

    static final Duration VALIDEZ = Duration.ofMinutes(30);
    private static final String ENLACE_INVALIDO = "El enlace no es válido o ha caducado";
    private static final Logger log = LoggerFactory.getLogger(RecuperacionContrasenaServiceImpl.class);

    private final SecureRandom aleatorio = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final EmailService emailService;
    private final Clock clock;
    private final String urlFrontend;

    public RecuperacionContrasenaServiceImpl(
            UsuarioRepository usuarioRepository,
            TokenRecuperacionRepository tokenRepository,
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
    public void solicitar(String correo) {
        if (!emailService.estaDisponible()) {
            throw new ServicioNoDisponibleException(
                    "La recuperación de contraseña no está disponible en este momento. Contacta con soporte.");
        }
        tokenRepository.eliminarCaducados(clock.instant());
        usuarioRepository.findByCorreoElectronico(correo.trim()).ifPresent(usuario -> {
            tokenRepository.eliminarPorUsuario(usuario.getUsuarioID());
            String token = generarToken();
            tokenRepository.save(new TokenRecuperacion(
                    usuario.getUsuarioID(), hashDeToken(token), clock.instant().plus(VALIDEZ)));
            enviarEnSegundoPlano(usuario.getCorreoElectronico(), token);
        });
    }

    @Override
    @Transactional
    public void restablecer(String token, String nuevaContrasena) {
        if (nuevaContrasena == null || nuevaContrasena.length() < 10 || nuevaContrasena.length() > 100) {
            throw new IllegalArgumentException("La nueva contraseña debe tener entre 10 y 100 caracteres");
        }
        TokenRecuperacion registro = tokenRepository.findByTokenHash(hashDeToken(token))
                .filter(t -> t.getExpiraEn().isAfter(clock.instant()))
                .orElseThrow(() -> new IllegalArgumentException(ENLACE_INVALIDO));

        Usuario usuario = usuarioRepository.findByIdForUpdate(registro.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException(ENLACE_INVALIDO));
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        // Quien tuviera acceso a la cuenta (la razón habitual de restablecer) pierde sus sesiones.
        usuario.invalidarSesiones();
        usuarioRepository.save(usuario);
        // Un solo uso: se invalidan este y cualquier otro enlace pendiente de la cuenta.
        tokenRepository.eliminarPorUsuario(usuario.getUsuarioID());
    }

    /** Hash SHA-256 en hexadecimal; es lo único que se guarda del token. */
    public static String hashDeToken(String token) {
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * El envío va en segundo plano para que el tiempo de respuesta no delate si la cuenta existe.
     * Un fallo del proveedor se registra sin datos sensibles y no cambia la respuesta al usuario.
     */
    private void enviarEnSegundoPlano(String destinatario, String token) {
        String enlace = urlFrontend + "/reset-password?token=" + token;
        String html = "<p>Has solicitado restablecer tu contraseña de BidCo.</p>"
                + "<p><a href=\"" + enlace + "\">Elegir una contraseña nueva</a></p>"
                + "<p>El enlace caduca en " + VALIDEZ.toMinutes() + " minutos y solo se puede usar una vez. "
                + "Si no lo has pedido tú, ignora este mensaje: tu contraseña no cambiará.</p>";
        CompletableFuture.runAsync(() -> {
            try {
                emailService.enviar(destinatario, "Restablece tu contraseña de BidCo", html);
            } catch (RuntimeException e) {
                log.warn("No se pudo enviar el correo de recuperación: {}", e.getMessage());
            }
        });
    }
}
