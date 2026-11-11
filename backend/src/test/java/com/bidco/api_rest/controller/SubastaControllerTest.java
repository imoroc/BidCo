package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.subasta.SubastaResponseDTO;
import com.bidco.api_rest.dto.subasta.SubastaUpdateDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.service.contract.SubastaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP de la edición y la cancelación de subastas propias. */
class SubastaControllerTest {

    private static final String ATRIBUTO_USUARIO = "authenticatedUserId";
    private static final String CUERPO_VALIDO =
            "{\"nombreArticulo\":\"Reloj\",\"descripcion\":\"Funciona\",\"categoria\":\"moda\"}";

    private SubastaService subastaService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        subastaService = mock(SubastaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SubastaController(subastaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void editarUsaElUsuarioDelTokenYNoElDelCuerpo() throws Exception {
        when(subastaService.actualizarSubasta(eq(7L), eq(3L), any())).thenReturn(mock(SubastaResponseDTO.class));

        mvc.perform(put("/api/subastas/7").requestAttr(ATRIBUTO_USUARIO, 3L)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isOk());

        ArgumentCaptor<SubastaUpdateDTO> dto = ArgumentCaptor.forClass(SubastaUpdateDTO.class);
        verify(subastaService).actualizarSubasta(eq(7L), eq(3L), dto.capture());
        assertEquals("Reloj", dto.getValue().getNombreArticulo());
    }

    @Test
    void editarConCamposEnBlancoDevuelve400SinLlamarAlServicio() throws Exception {
        String cuerpo = "{\"nombreArticulo\":\" \",\"descripcion\":\"x\",\"categoria\":\"moda\"}";

        mvc.perform(put("/api/subastas/7").requestAttr(ATRIBUTO_USUARIO, 3L)
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(subastaService);
    }

    @Test
    void editarUnaSubastaAjenaDevuelve403() throws Exception {
        when(subastaService.actualizarSubasta(eq(7L), eq(9L), any()))
                .thenThrow(new ForbiddenException("Solo el creador puede modificar la subasta"));

        mvc.perform(put("/api/subastas/7").requestAttr(ATRIBUTO_USUARIO, 9L)
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelarDevuelve204() throws Exception {
        mvc.perform(delete("/api/subastas/7").requestAttr(ATRIBUTO_USUARIO, 3L))
                .andExpect(status().isNoContent());

        verify(subastaService).cancelarSubasta(7L, 3L);
    }

    @Test
    void cancelarUnaSubastaConPujasDevuelve409() throws Exception {
        doThrow(new IllegalStateException("La subasta ya tiene pujas y no se puede modificar"))
                .when(subastaService).cancelarSubasta(7L, 3L);

        mvc.perform(delete("/api/subastas/7").requestAttr(ATRIBUTO_USUARIO, 3L))
                .andExpect(status().isConflict());
    }
}
