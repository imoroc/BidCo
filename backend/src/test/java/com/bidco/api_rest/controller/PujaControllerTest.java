package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.puja.PujaCreateDTO;
import com.bidco.api_rest.dto.puja.PujaResponseDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.mapper.SubastaMapper;
import com.bidco.api_rest.service.contract.PujaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

/** Contrato HTTP de las pujas: la identidad sale del token, la validación y los códigos de error. */
class PujaControllerTest {

    private static final String ATRIBUTO_USUARIO = "authenticatedUserId";
    private static final long USUARIO = 3L;
    private static final long OTRO_USUARIO = 99L;

    private PujaService pujaService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        pujaService = mock(PujaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PujaController(pujaService, mock(SubastaMapper.class)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static MockHttpServletRequestBuilder pujar(String cuerpo) {
        return post("/api/pujas").requestAttr(ATRIBUTO_USUARIO, USUARIO)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private static PujaResponseDTO puja(String importe) {
        return new PujaResponseDTO(11L, new BigDecimal(importe), LocalDate.of(2026, 11, 11), false, 5L, USUARIO);
    }

    @Test
    void pujarUsaElUsuarioDelTokenYNoElDelCuerpo() throws Exception {
        when(pujaService.crearPuja(any())).thenReturn(puja("15.50"));

        mvc.perform(pujar("{\"importe\":15.50,\"subastaId\":5,\"pujadorId\":" + OTRO_USUARIO + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pujaID").value(11));

        ArgumentCaptor<PujaCreateDTO> dto = ArgumentCaptor.forClass(PujaCreateDTO.class);
        verify(pujaService).crearPuja(dto.capture());
        assertEquals(USUARIO, dto.getValue().getPujadorId());
        assertEquals(5L, dto.getValue().getSubastaID());
        assertEquals(0, new BigDecimal("15.50").compareTo(dto.getValue().getImporte()));
    }

    @Test
    void pujarConImporteCeroDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(pujar("{\"importe\":0,\"subastaId\":5}")).andExpect(status().isBadRequest());

        verifyNoInteractions(pujaService);
    }

    @Test
    void pujarConMasDeDosDecimalesDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(pujar("{\"importe\":15.505,\"subastaId\":5}")).andExpect(status().isBadRequest());

        verifyNoInteractions(pujaService);
    }

    @Test
    void pujarSinSubastaDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(pujar("{\"importe\":15}")).andExpect(status().isBadRequest());

        verifyNoInteractions(pujaService);
    }

    @Test
    void pujarSinImporteDevuelve400SinLlamarAlServicio() throws Exception {
        mvc.perform(pujar("{\"subastaId\":5}")).andExpect(status().isBadRequest());

        verifyNoInteractions(pujaService);
    }

    @Test
    void unaPujaRechazadaPorElServicioDevuelve400() throws Exception {
        when(pujaService.crearPuja(any())).thenThrow(new IllegalArgumentException("La puja debe superar la actual"));

        mvc.perform(pujar("{\"importe\":1,\"subastaId\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La puja debe superar la actual"));
    }

    @Test
    void pujarEnUnaSubastaCerradaDevuelve409() throws Exception {
        when(pujaService.crearPuja(any())).thenThrow(new IllegalStateException("La subasta ya ha cerrado"));

        mvc.perform(pujar("{\"importe\":10,\"subastaId\":5}")).andExpect(status().isConflict());
    }

    @Test
    void consultarUnaPujaPasaElUsuarioDelToken() throws Exception {
        when(pujaService.buscarPujaPorId(11L, USUARIO)).thenReturn(puja("20"));

        mvc.perform(get("/api/pujas/11").requestAttr(ATRIBUTO_USUARIO, USUARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pujaID").value(11));
    }

    @Test
    void consultarLaPujaDeOtroUsuarioDevuelve403() throws Exception {
        when(pujaService.buscarPujaPorId(11L, OTRO_USUARIO)).thenThrow(new ForbiddenException("No es tu puja"));

        mvc.perform(get("/api/pujas/11").requestAttr(ATRIBUTO_USUARIO, OTRO_USUARIO))
                .andExpect(status().isForbidden());
    }

    @Test
    void laPujaMasAltaSeConsultaSinIdentificarse() throws Exception {
        when(pujaService.obtenerPujaMasAlta(5L)).thenReturn(puja("42"));

        mvc.perform(get("/api/pujas/mayor/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importe").value(42));
    }

    @Test
    void unUsuarioNoPuedeVerLasSubastasEnLasQuePujaOtro() throws Exception {
        mvc.perform(get("/api/pujas/subastas-usuario/" + OTRO_USUARIO).requestAttr(ATRIBUTO_USUARIO, USUARIO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pujaService);
    }

    @Test
    void unUsuarioVeSusPropiasSubastasConPujas() throws Exception {
        when(pujaService.obtenerSubastasPorUsuarioId(USUARIO)).thenReturn(List.of());

        mvc.perform(get("/api/pujas/subastas-usuario/" + USUARIO).requestAttr(ATRIBUTO_USUARIO, USUARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
