package com.bidco.api_rest.controller;

import com.bidco.api_rest.config.LoginRateLimiter;
import com.bidco.api_rest.dto.admin.AdminLoginDTO;
import com.bidco.api_rest.dto.admin.SorteoCreateDTO;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.service.contract.AdminService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del contrato HTTP de {@link AdminController}. Se monta sin contexto de Spring: controlador real,
 * manejador de errores real y limitador real con un reloj fijo; solo el servicio está simulado.
 */
class AdminControllerTest {

    private static final int FALLOS_MAXIMOS = 5;

    private final String contrasena = "a".repeat(12);

    private AdminService adminService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        adminService = mock(AdminService.class);
        LoginRateLimiter limitador = new LoginRateLimiter(
                Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(adminService, limitador))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private String cuerpoLogin(String usuario, String clave) {
        return "{\"username\":\"" + usuario + "\",\"password\":\"" + clave + "\",\"rol\":false}";
    }

    private MockHttpServletRequestBuilder peticionLogin(String cuerpo, MockHttpSession sesion) {
        return post("/api/admin/login")
                .session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo);
    }

    // ---- Login ----

    @Test
    void loginCorrectoDevuelve200YPasaLaSesionAlServicio() throws Exception {
        MockHttpSession sesion = new MockHttpSession();

        mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), sesion)).andExpect(status().isOk());

        ArgumentCaptor<AdminLoginDTO> dto = ArgumentCaptor.forClass(AdminLoginDTO.class);
        verify(adminService).login(dto.capture(), any(HttpSession.class));
        assertEquals("admin", dto.getValue().getUsername());
        assertEquals(contrasena, dto.getValue().getPassword());
    }

    @Test
    void loginCorrectoCambiaElIdentificadorDeSesion() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        String antes = sesion.getId();

        mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), sesion)).andExpect(status().isOk());

        assertNotEquals(antes, sesion.getId());
    }

    @Test
    void loginIncorrectoDevuelve401YNoCambiaLaSesion() throws Exception {
        doThrow(new AuthenticationFailedException("Credenciales incorrectas"))
                .when(adminService).login(any(), any());
        MockHttpSession sesion = new MockHttpSession();
        String antes = sesion.getId();

        mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), sesion))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales incorrectas"));

        assertEquals(antes, sesion.getId());
    }

    @Test
    void loginSeBloqueaTrasDemasiadosFallos() throws Exception {
        doThrow(new AuthenticationFailedException("Credenciales incorrectas"))
                .when(adminService).login(any(), any());

        for (int i = 0; i < FALLOS_MAXIMOS; i++) {
            mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), new MockHttpSession()))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), new MockHttpSession()))
                .andExpect(status().isTooManyRequests());

        verify(adminService, times(FALLOS_MAXIMOS)).login(any(), any());
    }

    @Test
    void elBloqueoDeUnUsuarioNoAfectaAOtro() throws Exception {
        doThrow(new AuthenticationFailedException("Credenciales incorrectas"))
                .when(adminService).login(any(), any());
        for (int i = 0; i < FALLOS_MAXIMOS; i++) {
            mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), new MockHttpSession()))
                    .andExpect(status().isUnauthorized());
        }

        doNothing().when(adminService).login(any(), any());
        mvc.perform(peticionLogin(cuerpoLogin("otro", contrasena), new MockHttpSession()))
                .andExpect(status().isOk());
    }

    @Test
    void unAciertoReiniciaElContadorDeFallos() throws Exception {
        doThrow(new AuthenticationFailedException("Credenciales incorrectas"))
                .when(adminService).login(any(), any());
        for (int i = 0; i < FALLOS_MAXIMOS - 1; i++) {
            mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), new MockHttpSession()))
                    .andExpect(status().isUnauthorized());
        }

        doNothing().when(adminService).login(any(), any());
        mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), new MockHttpSession()))
                .andExpect(status().isOk());

        doThrow(new AuthenticationFailedException("Credenciales incorrectas"))
                .when(adminService).login(any(), any());
        for (int i = 0; i < FALLOS_MAXIMOS - 1; i++) {
            mvc.perform(peticionLogin(cuerpoLogin("admin", contrasena), new MockHttpSession()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void loginSinUsuarioODeContrasenaDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(peticionLogin(cuerpoLogin("", contrasena), new MockHttpSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
        mvc.perform(peticionLogin(cuerpoLogin("admin", " "), new MockHttpSession()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(adminService);
    }

    // ---- Alta de sorteos ----

    private static MockMultipartFile parteSorteo(String json) {
        return new MockMultipartFile("sorteo", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void crearUnSorteoConImagenPasaElDtoYLaImagenAlServicio() throws Exception {
        MockMultipartFile imagen = new MockMultipartFile("imagen", "foto.png", MediaType.IMAGE_PNG_VALUE,
                new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/admin/sorteos")
                        .file(parteSorteo("{\"nombreArticulo\":\"Reloj\",\"puntosNecesarios\":10}"))
                        .file(imagen))
                .andExpect(status().isOk());

        ArgumentCaptor<SorteoCreateDTO> dto = ArgumentCaptor.forClass(SorteoCreateDTO.class);
        verify(adminService).createSorteo(dto.capture(), any(HttpSession.class));
        assertEquals("Reloj", dto.getValue().getNombreArticulo());
        assertEquals(10, dto.getValue().getPuntosNecesarios());
        assertEquals("foto.png", dto.getValue().getImagen().getOriginalFilename());
    }

    @Test
    void crearUnSorteoSinImagenEsValido() throws Exception {
        mvc.perform(multipart("/api/admin/sorteos")
                        .file(parteSorteo("{\"nombreArticulo\":\"Reloj\",\"puntosNecesarios\":10}")))
                .andExpect(status().isOk());

        ArgumentCaptor<SorteoCreateDTO> dto = ArgumentCaptor.forClass(SorteoCreateDTO.class);
        verify(adminService).createSorteo(dto.capture(), any(HttpSession.class));
        assertNull(dto.getValue().getImagen());
    }

    @Test
    void crearUnSorteoRechazadoPorElServicioDevuelve400() throws Exception {
        doThrow(new IllegalArgumentException("No autenticado como admin"))
                .when(adminService).createSorteo(any(), any());

        mvc.perform(multipart("/api/admin/sorteos")
                        .file(parteSorteo("{\"nombreArticulo\":\"Reloj\",\"puntosNecesarios\":10}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No autenticado como admin"));
    }

    // ---- Moderación ----

    @Test
    void retirarUnaSubastaDevuelve204YPasaElMotivo() throws Exception {
        mvc.perform(post("/api/admin/subastas/9/retirar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Contenido no permitido\"}"))
                .andExpect(status().isNoContent());

        verify(adminService).retirarSubasta(eq(9L), eq("Contenido no permitido"), any(HttpSession.class));
    }

    @Test
    void retirarSinMotivoDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(post("/api/admin/subastas/9/retirar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(adminService);
    }

    @Test
    void retirarUnaSubastaYaCerradaDevuelve409() throws Exception {
        doThrow(new IllegalStateException("La subasta ya se cerró con ganador y no se puede retirar"))
                .when(adminService).retirarSubasta(eq(9L), any(), any());

        mvc.perform(post("/api/admin/subastas/9/retirar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"x\"}"))
                .andExpect(status().isConflict());
    }
}
