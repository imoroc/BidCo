package com.bidco.api_rest.service;

import com.bidco.api_rest.dto.cometario.ComentarioCreateDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.ComentarioMapper;
import com.bidco.api_rest.model.Comentario;
import com.bidco.api_rest.model.Trabajador;
import com.bidco.api_rest.repository.ComentarioRepository;
import com.bidco.api_rest.service.impl.ComentarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ComentarioServiceImplTest {

    private ComentarioRepository comentarioRepository;
    private ComentarioMapper comentarioMapper;
    private ComentarioServiceImpl service;

    @BeforeEach
    void setUp() {
        comentarioRepository = mock(ComentarioRepository.class);
        comentarioMapper = mock(ComentarioMapper.class);
        service = new ComentarioServiceImpl(comentarioRepository, comentarioMapper);
    }

    private ComentarioCreateDTO dto(Long trabajadorId) {
        return new ComentarioCreateDTO("Buen estado", new BigDecimal("100"), trabajadorId, 1L);
    }

    private Comentario comentarioDe(Long trabajadorId) {
        Trabajador trabajador = new Trabajador();
        trabajador.setTrabajadorID(trabajadorId);
        Comentario comentario = new Comentario();
        comentario.setTrabajador(trabajador);
        return comentario;
    }

    @Test
    void anadirUsaElTrabajadorAutenticadoYNoElDelCuerpo() {
        ComentarioCreateDTO dto = dto(2L);
        when(comentarioMapper.comentarioCreateDTOToComentario(any())).thenReturn(new Comentario());
        when(comentarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.añadirComentario(5L, dto);

        assertEquals(5L, dto.getTrabajadorId());
        verify(comentarioMapper).comentarioCreateDTOToComentario(dto);
    }

    @Test
    void anadirSinIdentidadEsRechazado() {
        assertThrows(ForbiddenException.class, () -> service.añadirComentario(null, dto(2L)));
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    void editarComentarioAjenoEsRechazado() {
        when(comentarioRepository.findById(10L)).thenReturn(Optional.of(comentarioDe(2L)));

        assertThrows(ForbiddenException.class, () -> service.editarComentarioPorId(5L, 10L, dto(5L)));
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    void editarComentarioPropioActualizaElTexto() {
        Comentario existente = comentarioDe(5L);
        when(comentarioRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(comentarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.editarComentarioPorId(5L, 10L, dto(5L));

        assertEquals("Buen estado", existente.getComentario());
        verify(comentarioRepository).save(existente);
    }
}
