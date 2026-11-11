package com.bidco.api_rest.config;

import com.bidco.api_rest.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Matriz de permisos del filtro: ruta × método × rol → estado esperado. */
class JwtFilterTest {

    private static final String NONE = "NONE";
    private static final int PASA = 200;

    private final JwtUtil jwtUtil = new JwtUtil("k".repeat(40), 3_600_000L);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final JwtFilter filter = new JwtFilter(jwtUtil, usuarios);

    @BeforeEach
    void cuentaSieteConVersionCero() {
        when(usuarios.findTokenVersionById(7L)).thenReturn(Optional.of(0));
    }

    private int ejecutar(String metodo, String ruta, String rol, MockHttpSession sesion, String csrf) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, ruta);
        if (!NONE.equals(rol)) {
            request.addHeader("Authorization", "Bearer " + jwtUtil.generateToken("7", rol));
        }
        if (sesion != null) {
            request.setSession(sesion);
        }
        if (csrf != null) {
            request.addHeader(JwtFilter.ADMIN_CSRF_HEADER, csrf);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        return chain.getRequest() != null ? PASA : response.getStatus();
    }

    @ParameterizedTest(name = "{0} {1} con {2} → {3}")
    @CsvSource({
            // Rutas públicas
            "GET,/api/ping,NONE,200",
            "GET,/actuator/health,NONE,200",
            "GET,/actuator/info,NONE,401",
            "GET,/actuator/env,USER,403",
            "POST,/actuator/health,NONE,401",
            "GET,/api/subastas,NONE,200",
            "GET,/api/subastas/5,NONE,200",
            "GET,/api/subastas/5/eventos,NONE,200",
            "GET,/api/pujas/historial/5,NONE,200",
            "GET,/api/sorteos/3,NONE,200",
            "GET,/api/comentarios/subasta/2,NONE,200",
            "POST,/api/usuarios/login,NONE,200",
            "OPTIONS,/api/pujas,NONE,200",
            // Subastas
            "POST,/api/subastas,NONE,401",
            "POST,/api/subastas,USER,200",
            "POST,/api/subastas,EXPERT,403",
            "PUT,/api/subastas/final/4,USER,200",
            "PUT,/api/subastas/final/4,WORKER,403",
            "GET,/api/subastas/filtrar-creador,USER,200",
            // Pujas
            "POST,/api/pujas,USER,200",
            "POST,/api/pujas,EXPERT,403",
            "POST,/api/pujas-sorteo,USER,200",
            "POST,/api/pujas-sorteo,WORKER,403",
            // Usuarios
            "GET,/api/usuarios/7,USER,200",
            "GET,/api/usuarios/7,EXPERT,403",
            "GET,/api/usuarios/7,NONE,401",
            // Sorteos: solo se consultan o se cierran
            "POST,/api/sorteos,USER,403",
            "PUT,/api/sorteos/final/4,USER,200",
            "PUT,/api/sorteos/final/4,WORKER,403",
            "PUT,/api/sorteos/4,USER,403",
            // Comentarios
            "POST,/api/comentarios/nuevo,EXPERT,200",
            "POST,/api/comentarios/nuevo,USER,403",
            "PUT,/api/comentarios/editar/3,EXPERT,200",
            "PUT,/api/comentarios/editar/3,USER,403",
            // Trabajadores
            "GET,/api/trabajadores/1,EXPERT,200",
            "GET,/api/trabajadores/1,WORKER,200",
            "GET,/api/trabajadores/1,USER,403",
            "POST,/api/trabajadores,EXPERT,403",
            // Denegar por defecto: rutas sin regla explícita
            "GET,/api/desconocido,USER,403",
            "GET,/api/desconocido,EXPERT,403",
            "POST,/api/ping,USER,403",
            "GET,/otra-cosa,WORKER,403"
    })
    void aplicaLaMatrizDePermisos(String metodo, String ruta, String rol, int esperado) throws Exception {
        assertEquals(esperado, ejecutar(metodo, ruta, rol, null, null));
    }

    @Test
    void rechazaTokensInvalidosYCabecerasIncompletas() throws Exception {
        MockHttpServletRequest sinToken = new MockHttpServletRequest("GET", "/api/usuarios/7");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filter.doFilter(sinToken, respuesta, new MockFilterChain());
        assertEquals(401, respuesta.getStatus());

        MockHttpServletRequest manipulado = new MockHttpServletRequest("GET", "/api/usuarios/7");
        manipulado.addHeader("Authorization", "Bearer " + jwtUtil.generateToken("7", "USER") + "x");
        MockHttpServletResponse respuesta2 = new MockHttpServletResponse();
        filter.doFilter(manipulado, respuesta2, new MockFilterChain());
        assertEquals(401, respuesta2.getStatus());

        MockHttpServletRequest vacio = new MockHttpServletRequest("GET", "/api/usuarios/7");
        vacio.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse respuesta3 = new MockHttpServletResponse();
        filter.doFilter(vacio, respuesta3, new MockFilterChain());
        assertEquals(401, respuesta3.getStatus());
    }

    @Test
    void elAreaDeAdministracionExigeSesionYCabeceraCsrf() throws Exception {
        MockHttpSession conSesion = new MockHttpSession();
        conSesion.setAttribute("adminId", 1L);

        assertEquals(401, ejecutar("GET", "/api/admin/sorteos", NONE, null, null));
        assertEquals(PASA, ejecutar("GET", "/api/admin/sorteos", NONE, conSesion, null));
        assertEquals(403, ejecutar("POST", "/api/admin/sorteos", NONE, conSesion, null));
        assertEquals(403, ejecutar("POST", "/api/admin/sorteos", NONE, conSesion, "otro"));
        assertEquals(PASA, ejecutar("POST", "/api/admin/sorteos", NONE, conSesion, "BidCo"));
    }

    @Test
    void soloElAdministradorConSesionPuedeDarDeAltaTrabajadores() throws Exception {
        MockHttpSession conSesion = new MockHttpSession();
        conSesion.setAttribute("adminId", 1L);

        assertEquals(403, ejecutar("POST", "/api/trabajadores", NONE, null, null));
        assertEquals(403, ejecutar("POST", "/api/trabajadores", NONE, conSesion, null));
        assertEquals(PASA, ejecutar("POST", "/api/trabajadores", NONE, conSesion, "BidCo"));
    }

    private int conToken(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/usuarios/7");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain.getRequest() != null ? PASA : response.getStatus();
    }

    @Test
    void unTokenConLaVersionVigenteSePermite() throws Exception {
        when(usuarios.findTokenVersionById(7L)).thenReturn(Optional.of(3));

        assertEquals(PASA, conToken(jwtUtil.generateToken("7", "USER", 3)));
    }

    @Test
    void unTokenAnteriorAlUltimoCambioDeSesionSeRechaza() throws Exception {
        when(usuarios.findTokenVersionById(7L)).thenReturn(Optional.of(4));

        assertEquals(401, conToken(jwtUtil.generateToken("7", "USER", 3)));
    }

    @Test
    void unTokenSinVersionCuentaComoVersionCero() throws Exception {
        assertEquals(PASA, conToken(jwtUtil.generateToken("7", "USER")));

        when(usuarios.findTokenVersionById(7L)).thenReturn(Optional.of(1));
        assertEquals(401, conToken(jwtUtil.generateToken("7", "USER")));
    }

    @Test
    void unTokenDeUnaCuentaInexistenteSeRechaza() throws Exception {
        when(usuarios.findTokenVersionById(7L)).thenReturn(Optional.empty());

        assertEquals(401, conToken(jwtUtil.generateToken("7", "USER", 0)));
    }

    @Test
    void lasSesionesDeExpertosNoConsultanLaVersionDeUsuario() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sorteos/filtro-experto");
        request.addHeader("Authorization", "Bearer " + jwtUtil.generateToken("7", "EXPERT"));
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        verify(usuarios, never()).findTokenVersionById(7L);
    }
}
