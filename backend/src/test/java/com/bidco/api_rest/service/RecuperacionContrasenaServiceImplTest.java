package com.bidco.api_rest.service;

import com.bidco.api_rest.exception.ServicioNoDisponibleException;
import com.bidco.api_rest.model.TokenRecuperacion;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.TokenRecuperacionRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.EmailService;
import com.bidco.api_rest.service.impl.RecuperacionContrasenaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static com.bidco.api_rest.service.impl.RecuperacionContrasenaServiceImpl.hashDeToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RecuperacionContrasenaServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-09T10:00:00Z");
    private static final String NUEVA_CONTRASENA = "una-contraseña-nueva";

    private UsuarioRepository usuarioRepository;
    private TokenRecuperacionRepository tokenRepository;
    private EmailService emailService;
    private RecuperacionContrasenaServiceImpl service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(TokenRecuperacionRepository.class);
        emailService = mock(EmailService.class);
        service = new RecuperacionContrasenaServiceImpl(
                usuarioRepository,
                tokenRepository,
                emailService,
                Clock.fixed(AHORA, ZoneId.of("Europe/Madrid")),
                "https://bid-co.vercel.app/");
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioID(7L);
        usuario.setCorreoElectronico("ana@example.com");
        return usuario;
    }

    @Test
    void solicitarSinProveedorDeCorreoDevuelveServicioNoDisponible() {
        when(emailService.estaDisponible()).thenReturn(false);

        assertThrows(ServicioNoDisponibleException.class, () -> service.solicitar("ana@example.com"));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void solicitarConCorreoExistenteGuardaSoloElHashYEnviaElEnlace() {
        when(emailService.estaDisponible()).thenReturn(true);
        when(usuarioRepository.findByCorreoElectronico("ana@example.com")).thenReturn(Optional.of(usuario()));

        service.solicitar(" ana@example.com ");

        ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).eliminarPorUsuario(7L);
        verify(tokenRepository).save(guardado.capture());
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).enviar(eq("ana@example.com"), anyString(), html.capture());

        String contenido = html.getValue();
        int inicio = contenido.indexOf("/reset-password?token=") + "/reset-password?token=".length();
        String token = contenido.substring(inicio, contenido.indexOf('"', inicio));

        assertTrue(contenido.contains("https://bid-co.vercel.app/reset-password?token="));
        assertNotEquals(token, guardado.getValue().getTokenHash());
        assertEquals(hashDeToken(token), guardado.getValue().getTokenHash());
        assertEquals(AHORA.plus(Duration.ofMinutes(30)), guardado.getValue().getExpiraEn());
    }

    @Test
    void solicitarConCorreoInexistenteNoGuardaNiEnviaNada() {
        when(emailService.estaDisponible()).thenReturn(true);
        when(usuarioRepository.findByCorreoElectronico("nadie@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.solicitar("nadie@example.com"));

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void restablecerConTokenValidoCambiaLaContrasenaEInvalidaLosEnlaces() {
        Usuario usuario = usuario();
        when(tokenRepository.findByTokenHash(hashDeToken("tok"))).thenReturn(
                Optional.of(new TokenRecuperacion(7L, hashDeToken("tok"), AHORA.plusSeconds(60))));
        when(usuarioRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(usuario));

        service.restablecer("tok", NUEVA_CONTRASENA);

        assertTrue(new BCryptPasswordEncoder().matches(NUEVA_CONTRASENA, usuario.getContrasena()));
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).eliminarPorUsuario(7L);
    }

    @Test
    void restablecerConTokenCaducadoFalla() {
        when(tokenRepository.findByTokenHash(hashDeToken("tok"))).thenReturn(
                Optional.of(new TokenRecuperacion(7L, hashDeToken("tok"), AHORA.minusSeconds(1))));

        assertThrows(IllegalArgumentException.class, () -> service.restablecer("tok", NUEVA_CONTRASENA));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void restablecerConTokenDesconocidoFalla() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.restablecer("otro", NUEVA_CONTRASENA));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void restablecerConContrasenaCortaFallaSinConsultarElToken() {
        assertThrows(IllegalArgumentException.class, () -> service.restablecer("tok", "corta"));
        verify(tokenRepository, never()).findByTokenHash(anyString());
    }
}
