package com.bidco.api_rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    private static final String ORIGEN = "https://bid-co.vercel.app";
    private static final String ORIGEN_PREVIEW = "https://bid-co-git-rama-demo.vercel.app";

    private CorsFilter filtro;

    @BeforeEach
    void preparar() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", new String[]{ORIGEN});
        ReflectionTestUtils.setField(config, "allowedOriginPatterns", new String[]{"https://bid-co-*.vercel.app"});
        filtro = config.corsFilterRegistration().getFilter();
    }

    private MockHttpServletRequest peticion(String metodo, String origen) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, "/api/subastas");
        request.addHeader("Origin", origen);
        return request;
    }

    @Test
    void permiteElOrigenConfiguradoYSuCredenciales() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filtro.doFilter(peticion("GET", ORIGEN), response, chain);

        assertEquals(ORIGEN, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        assertNotNull(chain.getRequest());
    }

    @Test
    void permiteLosPreviewsDeVercel() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(peticion("GET", ORIGEN_PREVIEW), response, new MockFilterChain());

        assertEquals(ORIGEN_PREVIEW, response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void rechazaUnOrigenNoPermitido() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filtro.doFilter(peticion("GET", "https://malo.example"), response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void respondeAlPreflightSinLlegarAlControlador() throws Exception {
        MockHttpServletRequest request = peticion("OPTIONS", ORIGEN);
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "authorization");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filtro.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(response.getHeader("Access-Control-Allow-Methods").contains("POST"));
        assertNull(chain.getRequest());
    }

    @Test
    void lasRespuestasDeErrorDeOtrosFiltrosConservanLasCabecerasCors() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain rechaza = (req, res) -> ((HttpServletResponse) res).setStatus(401);

        filtro.doFilter(peticion("GET", ORIGEN), response, rechaza);

        assertEquals(401, response.getStatus());
        assertEquals(ORIGEN, response.getHeader("Access-Control-Allow-Origin"));
    }
}
