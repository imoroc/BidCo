package com.bidco.api_rest.service;

import com.bidco.api_rest.exception.ServicioNoDisponibleException;
import com.bidco.api_rest.model.TokenVerificacionCorreo;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.TokenVerificacionCorreoRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.EmailService;
import com.bidco.api_rest.service.impl.VerificacionCorreoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

class VerificacionCorreoServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-09T10:00:00Z");

    private UsuarioRepository usuarioRepository;
    private TokenVerificacionCorreoRepository tokenRepository;
    private EmailService emailService;
    private VerificacionCorreoServiceImpl service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRepository = mock(TokenVerificacionCorreoRepository.class);
        emailService = mock(EmailService.class);
        service = new VerificacionCorreoServiceImpl(
                usuarioRepository,
                tokenRepository,
                emailService,
                Clock.fixed(AHORA, ZoneId.of("Europe/Madrid")),
                "https://bid-co.vercel.app/");
    }

    private Usuario usuarioSinVerificar() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioID(7L);
        usuario.setCorreoElectronico("ana@example.com");
        usuario.setCorreoVerificado(false);
        return usuario;
    }

    @Test
    void enviarGuardaSoloElHashYMandaElEnlace() {
        service.enviar(usuarioSinVerificar());

        ArgumentCaptor<TokenVerificacionCorreo> guardado = ArgumentCaptor.forClass(TokenVerificacionCorreo.class);
        verify(tokenRepository).eliminarPorUsuario(7L);
        verify(tokenRepository).save(guardado.capture());
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailService, timeout(2000)).enviar(eq("ana@example.com"), anyString(), html.capture());

        String contenido = html.getValue();
        String prefijo = "/verificar-correo?token=";
        int inicio = contenido.indexOf(prefijo) + prefijo.length();
        String token = contenido.substring(inicio, contenido.indexOf('"', inicio));

        assertTrue(contenido.contains("https://bid-co.vercel.app/verificar-correo?token="));
        assertNotEquals(token, guardado.getValue().getTokenHash());
        assertEquals(hashDeToken(token), guardado.getValue().getTokenHash());
        assertEquals(AHORA.plus(Duration.ofHours(24)), guardado.getValue().getExpiraEn());
    }

    @Test
    void reenviarSinProveedorDeCorreoDevuelveServicioNoDisponible() {
        when(emailService.estaDisponible()).thenReturn(false);

        assertThrows(ServicioNoDisponibleException.class, () -> service.reenviar(7L));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void reenviarAUnaCuentaYaVerificadaNoHaceNada() {
        Usuario verificado = usuarioSinVerificar();
        verificado.setCorreoVerificado(true);
        when(emailService.estaDisponible()).thenReturn(true);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(verificado));

        service.reenviar(7L);

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void reenviarAUnaCuentaSinVerificarGeneraUnEnlaceNuevo() {
        when(emailService.estaDisponible()).thenReturn(true);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuarioSinVerificar()));

        service.reenviar(7L);

        verify(tokenRepository).eliminarPorUsuario(7L);
        verify(tokenRepository).save(any(TokenVerificacionCorreo.class));
    }

    @Test
    void verificarConUnTokenValidoMarcaLaCuentaYLoInvalida() {
        Usuario usuario = usuarioSinVerificar();
        TokenVerificacionCorreo registro = new TokenVerificacionCorreo(7L, hashDeToken("abc"), AHORA.plusSeconds(60));
        when(tokenRepository.findByTokenHash(hashDeToken("abc"))).thenReturn(Optional.of(registro));
        when(usuarioRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(usuario));

        service.verificar("abc");

        assertTrue(usuario.isCorreoVerificado());
        verify(usuarioRepository).save(usuario);
        verify(tokenRepository).eliminarPorUsuario(7L);
    }

    @Test
    void verificarConUnTokenCaducadoSeRechazaSinTocarLaCuenta() {
        TokenVerificacionCorreo caducado = new TokenVerificacionCorreo(7L, hashDeToken("abc"), AHORA.minusSeconds(1));
        when(tokenRepository.findByTokenHash(hashDeToken("abc"))).thenReturn(Optional.of(caducado));

        assertThrows(IllegalArgumentException.class, () -> service.verificar("abc"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void verificarConUnTokenDesconocidoSeRechaza() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.verificar("nada"));
        verify(usuarioRepository, never()).save(any());
    }
}
