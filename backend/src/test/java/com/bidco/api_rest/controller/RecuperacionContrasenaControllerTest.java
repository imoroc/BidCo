package com.bidco.api_rest.controller;

import com.bidco.api_rest.config.LoginRateLimiter;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.service.contract.RecuperacionContrasenaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de la recuperación de contraseña: la respuesta no revela si la cuenta existe y los dos
 * pasos tienen límite de intentos. El limitador es el real, con reloj fijo; solo el servicio está simulado.
 */
class RecuperacionContrasenaControllerTest {

    private static final int INTENTOS_MAXIMOS = 5;

    private final String contrasenaNueva = "n".repeat(12);
    private final String token = "t".repeat(40);

    private RecuperacionContrasenaService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(RecuperacionContrasenaService.class);
        LoginRateLimiter limitador = new LoginRateLimiter(
                Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new RecuperacionContrasenaController(servicio, limitador))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private ResultActions solicitar(String correo) throws Exception {
        return mvc.perform(post("/api/usuarios/recuperar-contrasena")
                .contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"" + correo + "\"}"));
    }

    private ResultActions restablecer(String enlace, String contrasena) throws Exception {
        return mvc.perform(post("/api/usuarios/restablecer-contrasena")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + enlace + "\",\"contrasena\":\"" + contrasena + "\"}"));
    }

    // ---- Solicitud ----

    @Test
    void solicitarDevuelve202ConUnMensajeQueNoRevelaSiExisteLaCuenta() throws Exception {
        solicitar("ana@example.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").value(
                        "Si existe una cuenta con ese correo, recibirás un enlace para restablecer la contraseña."));

        verify(servicio).solicitar("ana@example.com");
    }

    @Test
    void solicitarConUnCorreoInvalidoDevuelve400SinLlamarAlServicio() throws Exception {
        solicitar("no-es-un-correo").andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void solicitarSeBloqueaTrasDemasiadasSolicitudesDelMismoCorreo() throws Exception {
        for (int i = 0; i < INTENTOS_MAXIMOS; i++) {
            solicitar("ana@example.com").andExpect(status().isAccepted());
        }
        solicitar("ana@example.com").andExpect(status().isTooManyRequests());

        verify(servicio, times(INTENTOS_MAXIMOS)).solicitar("ana@example.com");
    }

    @Test
    void solicitarSeBloqueaPorDireccionAunqueCambieElCorreo() throws Exception {
        for (int i = 0; i < INTENTOS_MAXIMOS; i++) {
            solicitar("usuario" + i + "@example.com").andExpect(status().isAccepted());
        }
        solicitar("otro@example.com").andExpect(status().isTooManyRequests());

        verify(servicio, times(INTENTOS_MAXIMOS)).solicitar(anyString());
    }

    // ---- Restablecimiento ----

    @Test
    void restablecerDevuelve200() throws Exception {
        restablecer(token, contrasenaNueva)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contraseña actualizada. Ya puedes iniciar sesión."));

        verify(servicio).restablecer(token, contrasenaNueva);
    }

    @Test
    void restablecerConUnaContrasenaCortaDevuelve400SinLlamarAlServicio() throws Exception {
        restablecer(token, "c".repeat(9)).andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void restablecerSinEnlaceDevuelve400SinLlamarAlServicio() throws Exception {
        restablecer(" ", contrasenaNueva).andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void unEnlaceInvalidoODeCaducadoDevuelve400() throws Exception {
        doThrow(new IllegalArgumentException("El enlace no es válido o ha caducado"))
                .when(servicio).restablecer(anyString(), anyString());

        restablecer(token, contrasenaNueva)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace no es válido o ha caducado"));
    }

    @Test
    void restablecerSeBloqueaTrasDemasiadosEnlacesInvalidos() throws Exception {
        doThrow(new IllegalArgumentException("El enlace no es válido o ha caducado"))
                .when(servicio).restablecer(anyString(), anyString());

        for (int i = 0; i < INTENTOS_MAXIMOS; i++) {
            restablecer(token, contrasenaNueva).andExpect(status().isBadRequest());
        }
        restablecer(token, contrasenaNueva).andExpect(status().isTooManyRequests());

        verify(servicio, times(INTENTOS_MAXIMOS)).restablecer(anyString(), anyString());
    }

    @Test
    void unAciertoReiniciaElContadorDeEnlacesInvalidos() throws Exception {
        IllegalArgumentException invalido = new IllegalArgumentException("El enlace no es válido o ha caducado");
        doThrow(invalido).doThrow(invalido).doThrow(invalido).doThrow(invalido)
                .doNothing()
                .doThrow(invalido)
                .when(servicio).restablecer(anyString(), anyString());

        for (int i = 0; i < INTENTOS_MAXIMOS - 1; i++) {
            restablecer(token, contrasenaNueva).andExpect(status().isBadRequest());
        }
        restablecer(token, contrasenaNueva).andExpect(status().isOk());
        for (int i = 0; i < INTENTOS_MAXIMOS; i++) {
            restablecer(token, contrasenaNueva).andExpect(status().isBadRequest());
        }
        restablecer(token, contrasenaNueva).andExpect(status().isTooManyRequests());
    }
}
