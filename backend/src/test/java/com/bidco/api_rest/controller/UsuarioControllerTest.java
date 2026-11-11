package com.bidco.api_rest.controller;

import com.bidco.api_rest.config.LoginRateLimiter;
import com.bidco.api_rest.dto.usuario.ExportacionDatosDTO;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.exception.ContrasenaActualIncorrectaException;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.exception.ServicioNoDisponibleException;
import com.bidco.api_rest.service.contract.UsuarioService;
import com.bidco.api_rest.service.contract.VerificacionCorreoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del contrato HTTP de {@link UsuarioController}: propietario del recurso, validación,
 * límite de intentos y códigos de estado. Se monta sin contexto de Spring: el controlador real, el
 * manejador de errores real y el limitador real con un reloj fijo; solo los servicios están simulados.
 */
class UsuarioControllerTest {

    private static final String ATRIBUTO_USUARIO = "authenticatedUserId";
    private static final long USUARIO = 7L;
    private static final long OTRO_USUARIO = 8L;
    private static final int FALLOS_MAXIMOS = 5;

    private final String contrasenaActual = "a".repeat(12);
    private final String contrasenaNueva = "b".repeat(12);

    private UsuarioService usuarioService;
    private VerificacionCorreoService verificacionCorreoService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        usuarioService = mock(UsuarioService.class);
        verificacionCorreoService = mock(VerificacionCorreoService.class);
        LoginRateLimiter limitador = new LoginRateLimiter(
                Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC));
        UsuarioController controlador = new UsuarioController(usuarioService, limitador, verificacionCorreoService);
        mvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static String json(String campo, String valor) {
        return "{\"" + campo + "\":\"" + valor + "\"}";
    }

    private static MockHttpServletRequestBuilder comoUsuario(MockHttpServletRequestBuilder peticion, long id) {
        return peticion.requestAttr(ATRIBUTO_USUARIO, id);
    }

    private static MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, String cuerpo) {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    // ---- Login ----

    @Test
    void loginDevuelveElToken() throws Exception {
        when(usuarioService.login(any())).thenReturn("token-de-prueba");

        mvc.perform(conCuerpo(post("/api/usuarios/login"),
                        "{\"identificador\":\"ana\",\"contrasena\":\"" + contrasenaActual + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-de-prueba"));
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() throws Exception {
        when(usuarioService.login(any())).thenThrow(new AuthenticationFailedException("Credenciales incorrectas"));

        mvc.perform(conCuerpo(post("/api/usuarios/login"),
                        "{\"identificador\":\"ana\",\"contrasena\":\"" + contrasenaActual + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));
    }

    @Test
    void loginSeBloqueaTrasDemasiadosFallos() throws Exception {
        when(usuarioService.login(any())).thenThrow(new AuthenticationFailedException("Credenciales incorrectas"));
        String cuerpo = "{\"identificador\":\"ana\",\"contrasena\":\"" + contrasenaActual + "\"}";

        for (int i = 0; i < FALLOS_MAXIMOS; i++) {
            mvc.perform(conCuerpo(post("/api/usuarios/login"), cuerpo)).andExpect(status().isUnauthorized());
        }
        mvc.perform(conCuerpo(post("/api/usuarios/login"), cuerpo)).andExpect(status().isTooManyRequests());

        verify(usuarioService, times(FALLOS_MAXIMOS)).login(any());
    }

    @Test
    void loginConContrasenaDemasiadoCortaDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(conCuerpo(post("/api/usuarios/login"), "{\"identificador\":\"ana\",\"contrasena\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.contrasena").exists());

        verifyNoInteractions(usuarioService);
    }

    // ---- Propietario del recurso ----

    @Test
    void unUsuarioNoPuedeLeerLaCuentaDeOtro() throws Exception {
        mvc.perform(comoUsuario(get("/api/usuarios/{id}", OTRO_USUARIO), USUARIO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No puedes acceder ni modificar los datos de otro usuario"));

        verifyNoInteractions(usuarioService);
    }

    // ---- Cambio de contraseña ----

    @Test
    void cambiarLaContrasenaDevuelveUnTokenNuevo() throws Exception {
        when(usuarioService.updatePassword(USUARIO, contrasenaActual, contrasenaNueva)).thenReturn("token-nuevo");

        mvc.perform(comoUsuario(conCuerpo(patch("/api/usuarios/{id}/password", USUARIO),
                        "{\"currentPassword\":\"" + contrasenaActual + "\",\"newPassword\":\"" + contrasenaNueva + "\"}"),
                        USUARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-nuevo"));
    }

    @Test
    void cambiarLaContrasenaDeOtroUsuarioDevuelve403() throws Exception {
        mvc.perform(comoUsuario(conCuerpo(patch("/api/usuarios/{id}/password", OTRO_USUARIO),
                        "{\"currentPassword\":\"" + contrasenaActual + "\",\"newPassword\":\"" + contrasenaNueva + "\"}"),
                        USUARIO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambiarLaContrasenaExigeUnaNuevaDeAlMenosDiezCaracteres() throws Exception {
        mvc.perform(comoUsuario(conCuerpo(patch("/api/usuarios/{id}/password", USUARIO),
                        "{\"currentPassword\":\"" + contrasenaActual + "\",\"newPassword\":\"" + "c".repeat(9) + "\"}"),
                        USUARIO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void cambiarLaContrasenaSeBloqueaTrasDemasiadosFallos() throws Exception {
        when(usuarioService.updatePassword(anyLong(), anyString(), anyString()))
                .thenThrow(new ContrasenaActualIncorrectaException("La contraseña actual no es correcta"));
        String cuerpo = "{\"currentPassword\":\"" + contrasenaActual + "\",\"newPassword\":\"" + contrasenaNueva + "\"}";

        for (int i = 0; i < FALLOS_MAXIMOS; i++) {
            mvc.perform(comoUsuario(conCuerpo(patch("/api/usuarios/{id}/password", USUARIO), cuerpo), USUARIO))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(comoUsuario(conCuerpo(patch("/api/usuarios/{id}/password", USUARIO), cuerpo), USUARIO))
                .andExpect(status().isTooManyRequests());

        verify(usuarioService, times(FALLOS_MAXIMOS)).updatePassword(anyLong(), anyString(), anyString());
    }

    // ---- Verificación de correo ----

    @Test
    void verificarElCorreoDevuelve204() throws Exception {
        mvc.perform(conCuerpo(post("/api/usuarios/verificar-correo"), json("token", "enlace-de-prueba")))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(verificacionCorreoService).verificar("enlace-de-prueba");
    }

    @Test
    void verificarElCorreoConUnEnlaceInvalidoDevuelve400() throws Exception {
        doThrow(new IllegalArgumentException("El enlace no es válido o ha caducado"))
                .when(verificacionCorreoService).verificar("caducado");

        mvc.perform(conCuerpo(post("/api/usuarios/verificar-correo"), json("token", "caducado")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace no es válido o ha caducado"));
    }

    @Test
    void verificarElCorreoSinEnlaceDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(conCuerpo(post("/api/usuarios/verificar-correo"), json("token", " ")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(verificacionCorreoService);
    }

    @Test
    void reenviarLaVerificacionDevuelve204() throws Exception {
        mvc.perform(comoUsuario(post("/api/usuarios/{id}/reenviar-verificacion", USUARIO), USUARIO))
                .andExpect(status().isNoContent());

        verify(verificacionCorreoService).reenviar(USUARIO);
    }

    @Test
    void reenviarLaVerificacionDeOtroUsuarioDevuelve403() throws Exception {
        mvc.perform(comoUsuario(post("/api/usuarios/{id}/reenviar-verificacion", OTRO_USUARIO), USUARIO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(verificacionCorreoService);
    }

    @Test
    void reenviarLaVerificacionSinEnvioConfiguradoDevuelve503() throws Exception {
        doThrow(new ServicioNoDisponibleException("El envío de correos no está disponible"))
                .when(verificacionCorreoService).reenviar(USUARIO);

        mvc.perform(comoUsuario(post("/api/usuarios/{id}/reenviar-verificacion", USUARIO), USUARIO))
                .andExpect(status().isServiceUnavailable());
    }

    // ---- Exportar datos ----

    @Test
    void exportarLosDatosLosEntregaComoDescargaSinCache() throws Exception {
        when(usuarioService.exportarDatos(USUARIO))
                .thenReturn(new ExportacionDatosDTO(null, null, List.of(), List.of(), List.of()));

        mvc.perform(comoUsuario(get("/api/usuarios/{id}/exportar", USUARIO), USUARIO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"bidco-mis-datos.json\""))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.pujas").isEmpty());
    }

    @Test
    void exportarLosDatosDeOtroUsuarioDevuelve403() throws Exception {
        mvc.perform(comoUsuario(get("/api/usuarios/{id}/exportar", OTRO_USUARIO), USUARIO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    // ---- Eliminar cuenta ----

    @Test
    void eliminarLaCuentaDevuelve204() throws Exception {
        mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", USUARIO), json("contrasena", contrasenaActual)),
                        USUARIO))
                .andExpect(status().isNoContent());

        verify(usuarioService).eliminarCuenta(USUARIO, contrasenaActual);
    }

    @Test
    void eliminarLaCuentaDeOtroUsuarioDevuelve403() throws Exception {
        mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", OTRO_USUARIO),
                        json("contrasena", contrasenaActual)), USUARIO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void eliminarLaCuentaExigeLaContrasena() throws Exception {
        mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", USUARIO), json("contrasena", "")), USUARIO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.contrasena").exists());

        verifyNoInteractions(usuarioService);
    }

    @Test
    void eliminarLaCuentaConSubastasSinCerrarDevuelve409() throws Exception {
        doThrow(new IllegalStateException("Tienes subastas sin cerrar"))
                .when(usuarioService).eliminarCuenta(USUARIO, contrasenaActual);

        mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", USUARIO), json("contrasena", contrasenaActual)),
                        USUARIO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Tienes subastas sin cerrar"));
    }

    @Test
    void eliminarLaCuentaSeBloqueaTrasDemasiadosFallos() throws Exception {
        doThrow(new ContrasenaActualIncorrectaException("La contraseña no es correcta"))
                .when(usuarioService).eliminarCuenta(anyLong(), anyString());
        String cuerpo = json("contrasena", contrasenaActual);

        for (int i = 0; i < FALLOS_MAXIMOS; i++) {
            mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", USUARIO), cuerpo), USUARIO))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", USUARIO), cuerpo), USUARIO))
                .andExpect(status().isTooManyRequests());

        verify(usuarioService, times(FALLOS_MAXIMOS)).eliminarCuenta(anyLong(), anyString());
    }

    @Test
    void unAciertoReiniciaElContadorDeFallosAlEliminarLaCuenta() throws Exception {
        String cuerpo = json("contrasena", contrasenaActual);
        doThrow(new ContrasenaActualIncorrectaException("La contraseña no es correcta"))
                .when(usuarioService).eliminarCuenta(eq(USUARIO), anyString());
        for (int i = 0; i < FALLOS_MAXIMOS - 1; i++) {
            mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", USUARIO), cuerpo), USUARIO))
                    .andExpect(status().isBadRequest());
        }

        doNothing().when(usuarioService).eliminarCuenta(eq(USUARIO), anyString());
        mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", USUARIO), cuerpo), USUARIO))
                .andExpect(status().isNoContent());

        doThrow(new ContrasenaActualIncorrectaException("La contraseña no es correcta"))
                .when(usuarioService).eliminarCuenta(eq(USUARIO), anyString());
        for (int i = 0; i < FALLOS_MAXIMOS - 1; i++) {
            mvc.perform(comoUsuario(conCuerpo(delete("/api/usuarios/{id}", USUARIO), cuerpo), USUARIO))
                    .andExpect(status().isBadRequest());
        }
    }
}
