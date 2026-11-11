package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.dto.subasta.SubastaUpdateDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.PujaMapper;
import com.bidco.api_rest.mapper.SubastaMapper;
import com.bidco.api_rest.mapper.UsuarioMapper;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.ComentarioRepository;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.AvisoSubastaService;
import com.bidco.api_rest.service.contract.CloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubastaServiceImplModificacionTest {

    private static final Long ID = 7L;
    private static final Long CREADOR = 3L;

    private SubastaRepository subastaRepository;
    private PujaRepository pujaRepository;
    private UsuarioRepository usuarioRepository;
    private ComentarioRepository comentarioRepository;
    private SubastaServiceImpl servicio;
    private Subasta subasta;
    private Usuario creador;

    @BeforeEach
    void preparar() {
        subastaRepository = mock(SubastaRepository.class);
        pujaRepository = mock(PujaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        comentarioRepository = mock(ComentarioRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneId.of("Europe/Madrid"));
        servicio = new SubastaServiceImpl(subastaRepository, mock(SubastaMapper.class), mock(UsuarioMapper.class),
                usuarioRepository, pujaRepository, mock(PujaMapper.class), mock(CloudinaryService.class),
                mock(AvisoSubastaService.class), comentarioRepository, reloj);

        creador = new Usuario();
        creador.setUsuarioID(CREADOR);
        creador.setPuntos(120);
        subasta = new Subasta();
        subasta.setSubastaID(ID);
        subasta.setCreador(creador);
        subasta.setFechaFinal(Instant.parse("2026-11-20T10:00:00Z"));
        when(subastaRepository.findByIdForUpdate(ID)).thenReturn(Optional.of(subasta));
        when(usuarioRepository.findByIdForUpdate(CREADOR)).thenReturn(Optional.of(creador));
    }

    private static SubastaUpdateDTO datos() {
        SubastaUpdateDTO dto = new SubastaUpdateDTO();
        dto.setNombreArticulo("  Reloj de bolsillo ");
        dto.setDescripcion(" Funciona bien ");
        dto.setCategoria(" Tecnología ");
        return dto;
    }

    @Test
    void elCreadorEditaYLaCategoriaSeNormaliza() {
        servicio.actualizarSubasta(ID, CREADOR, datos());

        assertEquals("Reloj de bolsillo", subasta.getNombreArticulo());
        assertEquals("Funciona bien", subasta.getDescripcion());
        assertEquals("tecnologia", subasta.getCategoria());
        verify(subastaRepository).save(subasta);
    }

    @Test
    void otroUsuarioNoPuedeEditarNiCancelar() {
        assertThrows(ForbiddenException.class, () -> servicio.actualizarSubasta(ID, 99L, datos()));
        assertThrows(ForbiddenException.class, () -> servicio.cancelarSubasta(ID, 99L));
        verify(subastaRepository, never()).delete(any());
    }

    @Test
    void conPujasNoSePuedeEditarNiCancelar() {
        when(pujaRepository.haySubastaConPujas(ID)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> servicio.actualizarSubasta(ID, CREADOR, datos()));
        assertThrows(IllegalStateException.class, () -> servicio.cancelarSubasta(ID, CREADOR));
        verify(subastaRepository, never()).delete(any());
    }

    @Test
    void unaSubastaCerradaNoSePuedeTocar() {
        subasta.setFechaFinal(Instant.parse("2026-11-10T10:00:00Z"));

        assertThrows(IllegalStateException.class, () -> servicio.cancelarSubasta(ID, CREADOR));
        assertThrows(IllegalStateException.class, () -> servicio.actualizarSubasta(ID, CREADOR, datos()));
    }

    @Test
    void unaSubastaInexistenteDaNoEncontrada() {
        when(subastaRepository.findByIdForUpdate(ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> servicio.cancelarSubasta(ID, CREADOR));
    }

    @Test
    void cancelarBorraLaSubastaSusComentariosYDevuelveLosPuntos() {
        servicio.cancelarSubasta(ID, CREADOR);

        verify(comentarioRepository).eliminarPorSubasta(ID);
        verify(subastaRepository).delete(subasta);
        assertEquals(70, creador.getPuntos());
        verify(usuarioRepository).save(creador);
    }

    @Test
    void losPuntosDevueltosNuncaDejanElSaldoDePuntosEnNegativo() {
        creador.setPuntos(20);

        servicio.cancelarSubasta(ID, CREADOR);

        assertEquals(0, creador.getPuntos());
    }
}
