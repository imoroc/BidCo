package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.pujasorteo.PujaSorteoCreateDTO;
import com.bidco.api_rest.dto.pujasorteo.PujaSorteoResponseDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.service.contract.PujaSorteoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP de la participación en sorteos: la identidad sale del token y la validación de los puntos. */
class PujaSorteoControllerTest {

    private static final String ATRIBUTO_USUARIO = "authenticatedUserId";
    private static final long USUARIO = 3L;
    private static final long OTRO_USUARIO = 99L;

    private PujaSorteoService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(PujaSorteoService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PujaSorteoController(servicio))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static MockHttpServletRequestBuilder participar(String cuerpo) {
        return post("/api/pujas-sorteo").requestAttr(ATRIBUTO_USUARIO, USUARIO)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private static PujaSorteoResponseDTO participacion() {
        return new PujaSorteoResponseDTO(12L, 12L, new BigDecimal("50"), LocalDate.of(2026, 11, 11), 6L, USUARIO);
    }

    @Test
    void participarUsaElUsuarioDelTokenYDevuelve201() throws Exception {
        when(servicio.crearPujaSorteo(any())).thenReturn(participacion());

        mvc.perform(participar("{\"puntos\":50,\"sorteoId\":6,\"pujadorId\":" + OTRO_USUARIO + "}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<PujaSorteoCreateDTO> dto = ArgumentCaptor.forClass(PujaSorteoCreateDTO.class);
        verify(servicio).crearPujaSorteo(dto.capture());
        assertEquals(USUARIO, dto.getValue().getPujadorId());
        assertEquals(6L, dto.getValue().getSorteoId());
    }

    @Test
    void participarSinPuntosDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(participar("{\"sorteoId\":6}")).andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void participarConPuntosCeroDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(participar("{\"puntos\":0,\"sorteoId\":6}")).andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void participarConPuntosDecimalesDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(participar("{\"puntos\":10.5,\"sorteoId\":6}")).andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void participarSinSorteoDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(participar("{\"puntos\":50}")).andExpect(status().isBadRequest());

        verifyNoInteractions(servicio);
    }

    @Test
    void participarSinPuntosSuficientesDevuelve400() throws Exception {
        when(servicio.crearPujaSorteo(any())).thenThrow(new IllegalArgumentException("Puntos insuficientes"));

        mvc.perform(participar("{\"puntos\":50,\"sorteoId\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Puntos insuficientes"));
    }

    @Test
    void participarEnUnSorteoCerradoDevuelve409() throws Exception {
        when(servicio.crearPujaSorteo(any())).thenThrow(new IllegalStateException("El sorteo ya ha cerrado"));

        mvc.perform(participar("{\"puntos\":50,\"sorteoId\":6}")).andExpect(status().isConflict());
    }

    @Test
    void consultarLaParticipacionPropiaPasaElUsuarioDelToken() throws Exception {
        when(servicio.buscarPujaSorteoId(12L, USUARIO)).thenReturn(participacion());

        mvc.perform(get("/api/pujas-sorteo/12").requestAttr(ATRIBUTO_USUARIO, USUARIO))
                .andExpect(status().isOk());
    }

    @Test
    void consultarLaParticipacionDeOtroUsuarioDevuelve403() throws Exception {
        when(servicio.buscarPujaSorteoId(12L, OTRO_USUARIO)).thenThrow(new ForbiddenException("No es tu participación"));

        mvc.perform(get("/api/pujas-sorteo/12").requestAttr(ATRIBUTO_USUARIO, OTRO_USUARIO))
                .andExpect(status().isForbidden());
    }
}
