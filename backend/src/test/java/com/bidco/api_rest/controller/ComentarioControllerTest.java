package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.cometario.ComentarioResponseDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.exception.GlobalExceptionHandler;
import com.bidco.api_rest.service.contract.ComentarioService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP de los comentarios de expertos: el autor sale del token y la validación del cuerpo. */
class ComentarioControllerTest {

    private static final String ATRIBUTO_SUJETO = "authenticatedSubject";
    private static final String CUERPO_VALIDO =
            "{\"comentario\":\"Parece auténtico\",\"precioEstimado\":120,\"subastaID\":5}";

    private ComentarioService comentarioService;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        comentarioService = mock(ComentarioService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ComentarioController(comentarioService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ComentarioResponseDTO comentario() {
        return new ComentarioResponseDTO(9L, new BigDecimal("120"), "Parece auténtico", 4L, 5L);
    }

    private static MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, String sujeto, String cuerpo) {
        return peticion.requestAttr(ATRIBUTO_SUJETO, sujeto).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    @Test
    void comentarUsaElSujetoDelTokenYDevuelve201() throws Exception {
        when(comentarioService.añadirComentario(eq(4L), any())).thenReturn(comentario());

        mvc.perform(conCuerpo(post("/api/comentarios/nuevo"), "4", CUERPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comentarioID").value(9));

        verify(comentarioService).añadirComentario(eq(4L), any());
    }

    @Test
    void comentarConUnaIdentidadNoNumericaDevuelve403SinLlamarAlServicio() throws Exception {
        mvc.perform(conCuerpo(post("/api/comentarios/nuevo"), "no-numerico", CUERPO_VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(comentarioService);
    }

    @Test
    void comentarConElTextoEnBlancoDevuelve400SinLlamarAlServicio() throws Exception {
        String cuerpo = "{\"comentario\":\" \",\"precioEstimado\":120,\"subastaID\":5}";

        mvc.perform(conCuerpo(post("/api/comentarios/nuevo"), "4", cuerpo)).andExpect(status().isBadRequest());

        verifyNoInteractions(comentarioService);
    }

    @Test
    void comentarSinPrecioEstimadoDevuelve400SinLlamarAlServicio() throws Exception {
        String cuerpo = "{\"comentario\":\"Bien\",\"subastaID\":5}";

        mvc.perform(conCuerpo(post("/api/comentarios/nuevo"), "4", cuerpo)).andExpect(status().isBadRequest());

        verifyNoInteractions(comentarioService);
    }

    @Test
    void comentarConUnPrecioNegativoDevuelve400SinLlamarAlServicio() throws Exception {
        String cuerpo = "{\"comentario\":\"Bien\",\"precioEstimado\":-1,\"subastaID\":5}";

        mvc.perform(conCuerpo(post("/api/comentarios/nuevo"), "4", cuerpo)).andExpect(status().isBadRequest());

        verifyNoInteractions(comentarioService);
    }

    @Test
    void comentarSinSubastaDevuelve400SinLlamarAlServicio() throws Exception {
        String cuerpo = "{\"comentario\":\"Bien\",\"precioEstimado\":10}";

        mvc.perform(conCuerpo(post("/api/comentarios/nuevo"), "4", cuerpo)).andExpect(status().isBadRequest());

        verifyNoInteractions(comentarioService);
    }

    @Test
    void consultarUnComentarioInexistenteDevuelve404() throws Exception {
        when(comentarioService.buscarComentarioPorId(9L)).thenThrow(new EntityNotFoundException("No existe"));

        mvc.perform(get("/api/comentarios/9")).andExpect(status().isNotFound());
    }

    @Test
    void consultarUnComentarioLoDevuelve() throws Exception {
        when(comentarioService.buscarComentarioPorId(9L)).thenReturn(comentario());

        mvc.perform(get("/api/comentarios/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subastaID").value(5));
    }

    @Test
    void losComentariosDeUnaSubastaSeListan() throws Exception {
        when(comentarioService.buscarComentariosPorSubastaId(5L)).thenReturn(List.of(comentario()));

        mvc.perform(get("/api/comentarios/subasta/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comentarioID").value(9));
    }

    @Test
    void editarUsaElSujetoDelToken() throws Exception {
        when(comentarioService.editarComentarioPorId(eq(4L), eq(9L), any())).thenReturn(comentario());

        mvc.perform(conCuerpo(put("/api/comentarios/editar/9"), "4", CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comentarioID").value(9));

        verify(comentarioService).editarComentarioPorId(eq(4L), eq(9L), any());
    }

    @Test
    void editarElComentarioDeOtroExpertoDevuelve403() throws Exception {
        when(comentarioService.editarComentarioPorId(eq(4L), eq(9L), any()))
                .thenThrow(new ForbiddenException("Solo el autor puede editarlo"));

        mvc.perform(conCuerpo(put("/api/comentarios/editar/9"), "4", CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void editarConUnaIdentidadNoNumericaDevuelve403SinLlamarAlServicio() throws Exception {
        mvc.perform(conCuerpo(put("/api/comentarios/editar/9"), "x", CUERPO_VALIDO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(comentarioService);
    }
}
