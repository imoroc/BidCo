package com.bidco.api_rest.service;

import com.bidco.api_rest.dto.sorteo.SorteoResponseDTO;
import com.bidco.api_rest.mapper.PujaSorteoMapper;
import com.bidco.api_rest.mapper.SorteoMapper;
import com.bidco.api_rest.model.PujaSorteo;
import com.bidco.api_rest.model.Sorteo;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaSorteoRepository;
import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.CloudinaryService;
import com.bidco.api_rest.service.impl.SorteoServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SorteoServiceImplTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);
    private static final Long ID = 5L;

    private SorteoRepository sorteoRepository;
    private PujaSorteoRepository pujaSorteoRepository;
    private SorteoMapper sorteoMapper;
    private SorteoServiceImpl service;

    @BeforeEach
    void setUp() {
        sorteoRepository = mock(SorteoRepository.class);
        pujaSorteoRepository = mock(PujaSorteoRepository.class);
        sorteoMapper = mock(SorteoMapper.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
        service = new SorteoServiceImpl(sorteoRepository, sorteoMapper, mock(PujaSorteoMapper.class),
                pujaSorteoRepository, mock(UsuarioRepository.class), mock(CloudinaryService.class), reloj);
        when(sorteoMapper.sorteoToSorteoResponseDTO(any(Sorteo.class))).thenReturn(mock(SorteoResponseDTO.class));
    }

    private static Instant finDeDia(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Sorteo sorteo(LocalDate fechaFin, Long ganador) {
        Sorteo s = new Sorteo();
        s.setSorteoID(ID);
        s.setFechaFin(finDeDia(fechaFin));
        s.setGanador(ganador);
        when(sorteoRepository.findByIdForUpdate(ID)).thenReturn(Optional.of(s));
        return s;
    }

    private PujaSorteo participacion(long usuarioId, int puntos) {
        Usuario u = new Usuario();
        u.setUsuarioID(usuarioId);
        PujaSorteo p = new PujaSorteo();
        p.setPujador(u);
        p.setPuntos(BigDecimal.valueOf(puntos));
        return p;
    }

    @Test
    void sorteoInexistenteLanzaExcepcion() {
        when(sorteoRepository.findByIdForUpdate(ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.asignarGanadorYActualizarPrecioFinal(ID));
    }

    @Test
    void sorteoYaResueltoNoSeVuelveATirar() {
        Sorteo s = sorteo(HOY.minusDays(1), 99L);

        service.asignarGanadorYActualizarPrecioFinal(ID);

        assertEquals(99L, s.getGanador());
        verifyNoInteractions(pujaSorteoRepository);
        verify(sorteoRepository, never()).save(any());
    }

    @Test
    void sorteoNoFinalizadoLanzaExcepcion() {
        sorteo(HOY, null);

        assertThrows(IllegalStateException.class, () -> service.asignarGanadorYActualizarPrecioFinal(ID));
        verify(sorteoRepository, never()).save(any());
    }

    @Test
    void sorteoSinParticipantesQuedaSinGanador() {
        Sorteo s = sorteo(HOY.minusDays(1), null);
        when(pujaSorteoRepository.findBySorteoSorteoID(ID)).thenReturn(List.of());

        service.asignarGanadorYActualizarPrecioFinal(ID);

        assertNull(s.getGanador());
        verify(sorteoRepository, never()).save(any());
    }

    @Test
    void conUnSoloParticipanteGanaEse() {
        Sorteo s = sorteo(HOY.minusDays(1), null);
        when(pujaSorteoRepository.findBySorteoSorteoID(ID)).thenReturn(List.of(participacion(42L, 3)));

        service.asignarGanadorYActualizarPrecioFinal(ID);

        assertEquals(42L, s.getGanador());
        verify(sorteoRepository).save(s);
    }

    @Test
    void elGanadorSiempreEsUnParticipanteDelSorteo() {
        Sorteo s = sorteo(HOY.minusDays(1), null);
        when(pujaSorteoRepository.findBySorteoSorteoID(ID))
                .thenReturn(List.of(participacion(1L, 2), participacion(2L, 5), participacion(3L, 1)));

        service.asignarGanadorYActualizarPrecioFinal(ID);

        assertTrue(List.of(1L, 2L, 3L).contains(s.getGanador()));
    }
}
