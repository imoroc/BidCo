package com.bidco.api_rest.config;

import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.service.contract.SorteoService;
import com.bidco.api_rest.service.contract.SubastaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CierreAutomaticoTaskTest {

    private static final Instant AHORA = Instant.parse("2026-11-10T10:00:00Z");

    private SubastaRepository subastaRepository;
    private SorteoRepository sorteoRepository;
    private SubastaService subastaService;
    private SorteoService sorteoService;
    private CierreAutomaticoTask tarea;

    @BeforeEach
    void preparar() {
        subastaRepository = mock(SubastaRepository.class);
        sorteoRepository = mock(SorteoRepository.class);
        subastaService = mock(SubastaService.class);
        sorteoService = mock(SorteoService.class);
        Clock reloj = Clock.fixed(AHORA, ZoneId.of("Europe/Madrid"));
        tarea = new CierreAutomaticoTask(subastaRepository, sorteoRepository, subastaService, sorteoService, reloj);
    }

    @Test
    void cierraCadaSubastaVencidaConLaHoraDelServidor() {
        when(subastaRepository.findIdsVencidasConPujasSinGanador(AHORA)).thenReturn(List.of(1L, 2L, 3L));

        int cerradas = tarea.cerrarSubastasVencidas();

        assertEquals(3, cerradas);
        verify(subastaService).asignarGanadorYActualizarPrecioFinal(1L);
        verify(subastaService).asignarGanadorYActualizarPrecioFinal(2L);
        verify(subastaService).asignarGanadorYActualizarPrecioFinal(3L);
    }

    @Test
    void siUnaSubastaFallaSeSiguenProcesandoLasDemas() {
        when(subastaRepository.findIdsVencidasConPujasSinGanador(AHORA)).thenReturn(List.of(1L, 2L, 3L));
        doThrow(new IllegalStateException("fallo simulado")).when(subastaService).asignarGanadorYActualizarPrecioFinal(2L);

        int cerradas = tarea.cerrarSubastasVencidas();

        assertEquals(2, cerradas);
        verify(subastaService).asignarGanadorYActualizarPrecioFinal(3L);
    }

    @Test
    void cierraLosSorteosVencidosYToleraErrores() {
        when(sorteoRepository.findIdsVencidosConParticipacionesSinGanador(AHORA)).thenReturn(List.of(5L, 6L));
        doThrow(new IllegalStateException("fallo simulado")).when(sorteoService).asignarGanadorYActualizarPrecioFinal(5L);

        int cerrados = tarea.cerrarSorteosVencidos();

        assertEquals(1, cerrados);
        verify(sorteoService).asignarGanadorYActualizarPrecioFinal(6L);
    }

    @Test
    void sinPendientesNoLlamaAlosServicios() {
        when(subastaRepository.findIdsVencidasConPujasSinGanador(AHORA)).thenReturn(List.of());
        when(sorteoRepository.findIdsVencidosConParticipacionesSinGanador(AHORA)).thenReturn(List.of());

        tarea.cerrarVencidas();

        verifyNoInteractions(subastaService, sorteoService);
    }
}
