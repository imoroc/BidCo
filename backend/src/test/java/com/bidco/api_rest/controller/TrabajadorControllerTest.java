package com.bidco.api_rest.controller;

import com.bidco.api_rest.config.LoginRateLimiter;
import com.bidco.api_rest.dto.trabajador.TrabajadorResponseDTO;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.service.contract.TrabajadorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato HTTP de los trabajadores (administración y expertos): validación, identidad del token y límite
 * de intentos de acceso. El limitador es el real, con reloj fijo; solo el servicio está simulado.
 */
class TrabajadorControllerTest {

    private static final String ATRIBUTO_SUJETO = "authenticatedSubject";
    private static final int FALLOS_MAXIMOS = 5;

    private final String clave = "p".repeat(12);
    private final String tokenDeSesion = "k".repeat(30);

    private TrabajadorService trabajadorService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        trabajadorService = mock(TrabajadorService.class);
        LoginRateLimiter limitador = new LoginRateLimiter(
                Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new TrabajadorController(trabajadorService, limitador))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String alta(String correo, String contrasena) {
        return "{\"nombreUsuario\":\"experto1\",\"correoElectronico\":\"" + correo
                + "\",\"contrasena\":\"" + contrasena + "\",\"experto\":true}";
    }

    private static TrabajadorResponseDTO trabajador() {
        return new TrabajadorResponseDTO(4L, "experto1", "experto@example.com");
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder peticion, String cuerpo) {
        return peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private ResultActions login(String identificador) throws Exception {
        String cuerpo = "{\"identificador\":\"" + identificador + "\",\"contraseña\":\"" + clave + "\",\"rol\":false}";
        return mvc.perform(json(post("/api/trabajadores/login"), cuerpo));
    }

    // ---- Alta y actualización ----

    @Test
    void registrarUnTrabajadorDevuelveSusDatos() throws Exception {
        when(trabajadorService.registrarTrabajador(any())).thenReturn(trabajador());

        mvc.perform(json(post("/api/trabajadores"), alta("experto@example.com", clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value("experto1"));
    }

    @Test
    void registrarConUnCorreoInvalidoDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(json(post("/api/trabajadores"), alta("no-es-un-correo", clave)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trabajadorService);
    }

    @Test
    void registrarConUnaContrasenaCortaDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(json(post("/api/trabajadores"), alta("experto@example.com", "c".repeat(5))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(trabajadorService);
    }

    @Test
    void actualizarUsaElSujetoDelToken() throws Exception {
        when(trabajadorService.actualizarTrabajador(eq(4L), any())).thenReturn(trabajador());

        mvc.perform(json(put("/api/trabajadores").requestAttr(ATRIBUTO_SUJETO, "4"),
                        alta("experto@example.com", clave)))
                .andExpect(status().isOk());

        verify(trabajadorService).actualizarTrabajador(eq(4L), any());
    }

    @Test
    void actualizarConUnaIdentidadNoNumericaDevuelve403SinLlamarAlServicio() throws Exception {
        mvc.perform(json(put("/api/trabajadores").requestAttr(ATRIBUTO_SUJETO, "no-numerico"),
                        alta("experto@example.com", clave)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(trabajadorService);
    }

    // ---- Consultas ----

    @Test
    void consultarPorIdDevuelveElTrabajador() throws Exception {
        when(trabajadorService.buscarTrabajadorPorId(4L)).thenReturn(trabajador());

        mvc.perform(get("/api/trabajadores/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trabajadorID").value(4));
    }

    @Test
    void consultarPorNombreDeUsuarioDevuelveElTrabajador() throws Exception {
        when(trabajadorService.buscarTrabajadorPorNombreUsuario("experto1")).thenReturn(trabajador());

        mvc.perform(get("/api/trabajadores/buscar").param("nombreUsuario", "experto1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correoElectronico").value("experto@example.com"));
    }

    // ---- Acceso ----

    @Test
    void loginDevuelveElToken() throws Exception {
        when(trabajadorService.login(any())).thenReturn(tokenDeSesion);

        login("experto1").andExpect(status().isOk()).andExpect(jsonPath("$.token").value(tokenDeSesion));
    }

    @Test
    void loginConCredencialesIncorrectasDevuelve401() throws Exception {
        when(trabajadorService.login(any())).thenThrow(new AuthenticationFailedException("Credenciales incorrectas"));

        login("experto1").andExpect(status().isUnauthorized());
    }

    @Test
    void loginConUnaContrasenaCortaDevuelve400SinLlamarAlServicio() throws Exception {
        String cuerpo = "{\"identificador\":\"experto1\",\"contraseña\":\"" + "c".repeat(5) + "\",\"rol\":false}";

        mvc.perform(json(post("/api/trabajadores/login"), cuerpo)).andExpect(status().isBadRequest());

        verifyNoInteractions(trabajadorService);
    }

    @Test
    void loginSeBloqueaTrasDemasiadosFallos() throws Exception {
        when(trabajadorService.login(any())).thenThrow(new AuthenticationFailedException("Credenciales incorrectas"));

        for (int i = 0; i < FALLOS_MAXIMOS; i++) {
            login("experto1").andExpect(status().isUnauthorized());
        }
        login("experto1").andExpect(status().isTooManyRequests());

        verify(trabajadorService, times(FALLOS_MAXIMOS)).login(any());
    }

    @Test
    void unAciertoReiniciaElContadorDeFallos() throws Exception {
        AuthenticationFailedException fallo = new AuthenticationFailedException("Credenciales incorrectas");
        when(trabajadorService.login(any()))
                .thenThrow(fallo).thenThrow(fallo).thenThrow(fallo).thenThrow(fallo)
                .thenReturn(tokenDeSesion)
                .thenThrow(fallo);

        for (int i = 0; i < FALLOS_MAXIMOS - 1; i++) {
            login("experto1").andExpect(status().isUnauthorized());
        }
        login("experto1").andExpect(status().isOk());
        for (int i = 0; i < FALLOS_MAXIMOS; i++) {
            login("experto1").andExpect(status().isUnauthorized());
        }
        login("experto1").andExpect(status().isTooManyRequests());
    }

    @Test
    void elBloqueoDeUnIdentificadorNoAfectaAOtro() throws Exception {
        when(trabajadorService.login(any())).thenThrow(new AuthenticationFailedException("Credenciales incorrectas"));
        for (int i = 0; i < FALLOS_MAXIMOS; i++) {
            login("experto1").andExpect(status().isUnauthorized());
        }

        login("experto2").andExpect(status().isUnauthorized());
    }
}
