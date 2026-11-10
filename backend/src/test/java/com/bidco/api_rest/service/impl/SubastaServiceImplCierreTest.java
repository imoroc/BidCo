package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.mapper.PujaMapper;
import com.bidco.api_rest.mapper.SubastaMapper;
import com.bidco.api_rest.mapper.UsuarioMapper;
import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubastaServiceImplCierreTest {

    private static final Long ID = 7L;

    private SubastaRepository subastaRepository;
    private PujaRepository pujaRepository;
    private UsuarioRepository usuarioRepository;
    private SubastaServiceImpl servicio;
    private Subasta subasta;

    @BeforeEach
    void preparar() {
        subastaRepository = mock(SubastaRepository.class);
        pujaRepository = mock(PujaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneId.of("Europe/Madrid"));
        servicio = new SubastaServiceImpl(subastaRepository, mock(SubastaMapper.class), mock(UsuarioMapper.class),
                usuarioRepository, pujaRepository, mock(PujaMapper.class), mock(CloudinaryService.class), reloj);

        subasta = new Subasta();
        subasta.setSubastaNormal(true);
        subasta.setFechaFinal(LocalDate.of(2026, 11, 9));
        when(subastaRepository.findByIdForUpdate(ID)).thenReturn(Optional.of(subasta));
    }

    @Test
    void unaSubastaVencidaSinPujasSeCierraSinGanadorNiError() {
        when(pujaRepository.findTopBySubastaIdOrderByImporteDesc(ID)).thenReturn(null);

        assertDoesNotThrow(() -> servicio.asignarGanadorYActualizarPrecioFinal(ID));

        assertNull(subasta.getGanador());
        verify(subastaRepository, never()).save(subasta);
        verify(usuarioRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unaSubastaTodaviaEnCursoSigueRechazandoElCierre() {
        subasta.setFechaFinal(LocalDate.of(2026, 11, 10));

        assertThrows(IllegalStateException.class, () -> servicio.asignarGanadorYActualizarPrecioFinal(ID));
    }

    @Test
    void conPujasAsignaGanadorYSuma100PuntosEnSubastaNormal() {
        Usuario pujador = new Usuario();
        pujador.setUsuarioID(3L);
        pujador.setPuntos(10);
        Puja puja = new Puja();
        puja.setPujador(pujador);
        puja.setImporte(new BigDecimal("55.00"));
        when(pujaRepository.findTopBySubastaIdOrderByImporteDesc(ID)).thenReturn(puja);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(pujador));

        servicio.asignarGanadorYActualizarPrecioFinal(ID);

        assertEquals(3L, subasta.getGanador());
        assertEquals(110, pujador.getPuntos());
        assertEquals(new BigDecimal("55.00"), subasta.getPrecioFinal());
    }
}
