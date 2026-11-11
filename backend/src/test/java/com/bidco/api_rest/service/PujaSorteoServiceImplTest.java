package com.bidco.api_rest.service;

import com.bidco.api_rest.dto.pujasorteo.PujaSorteoCreateDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.PujaSorteoMapper;
import com.bidco.api_rest.model.PujaSorteo;
import com.bidco.api_rest.model.Sorteo;
import com.bidco.api_rest.exception.CorreoNoVerificadoException;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaSorteoRepository;
import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.impl.PujaSorteoServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PujaSorteoServiceImplTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    private PujaSorteoRepository pujaSorteoRepository;
    private PujaSorteoMapper pujaSorteoMapper;
    private SorteoRepository sorteoRepository;
    private UsuarioRepository usuarioRepository;
    private PujaSorteoServiceImpl service;

    @BeforeEach
    void setUp() {
        pujaSorteoRepository = mock(PujaSorteoRepository.class);
        pujaSorteoMapper = mock(PujaSorteoMapper.class);
        sorteoRepository = mock(SorteoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
        service = new PujaSorteoServiceImpl(pujaSorteoRepository, pujaSorteoMapper, sorteoRepository,
                usuarioRepository, clock);

        when(pujaSorteoMapper.pujaSorteoCreateDTOToPujaSorteo(any())).thenReturn(new PujaSorteo());
        when(pujaSorteoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Instant finDeDia(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Sorteo sorteo(LocalDate inicio, LocalDate fin, int puntosNecesarios) {
        Sorteo s = new Sorteo();
        s.setSorteoID(1L);
        s.setFechaInicio(inicio);
        s.setFechaFin(finDeDia(fin));
        s.setPuntosNecesarios(puntosNecesarios);
        return s;
    }

    private Usuario usuario(int puntos) {
        Usuario u = new Usuario();
        u.setUsuarioID(5L);
        u.setPuntos(puntos);
        return u;
    }

    private PujaSorteoCreateDTO dto(String puntos) {
        return new PujaSorteoCreateDTO(new BigDecimal(puntos), null, 1L, 5L);
    }

    private void sorteoAbierto(int puntosNecesarios) {
        when(sorteoRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(sorteo(HOY.minusDays(1), HOY.plusDays(3), puntosNecesarios)));
    }

    @Test
    void participarConElCorreoSinVerificarSeRechazaYNoGastaPuntos() {
        sorteoAbierto(10);
        Usuario u = usuario(100);
        u.setCorreoVerificado(false);
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(u));

        assertThrows(CorreoNoVerificadoException.class, () -> service.crearPujaSorteo(dto("20")));
        assertEquals(100, u.getPuntos());
        verify(pujaSorteoRepository, never()).save(any());
    }

    @Test
    void puntosNegativosSeRechazanYNoSumanPuntos() {
        sorteoAbierto(10);
        Usuario u = usuario(100);
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(u));

        assertThrows(IllegalArgumentException.class, () -> service.crearPujaSorteo(dto("-50")));
        assertEquals(100, u.getPuntos());
        verify(pujaSorteoRepository, never()).save(any());
    }

    @Test
    void puntosCeroSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> service.crearPujaSorteo(dto("0")));
        verify(pujaSorteoRepository, never()).save(any());
    }

    @Test
    void puntosConDecimalesSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> service.crearPujaSorteo(dto("10.5")));
        verify(pujaSorteoRepository, never()).save(any());
    }

    @Test
    void puntosPorDebajoDelMinimoSeRechazan() {
        sorteoAbierto(50);

        assertThrows(IllegalArgumentException.class, () -> service.crearPujaSorteo(dto("49")));
        verify(pujaSorteoRepository, never()).save(any());
        verify(usuarioRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void saldoDePuntosInsuficienteSeRechaza() {
        sorteoAbierto(10);
        Usuario u = usuario(20);
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(u));

        assertThrows(IllegalArgumentException.class, () -> service.crearPujaSorteo(dto("30")));
        assertEquals(20, u.getPuntos());
        verify(pujaSorteoRepository, never()).save(any());
    }

    @Test
    void sorteoCerradoSeRechaza() {
        when(sorteoRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(sorteo(HOY.minusDays(10), HOY.minusDays(1), 10)));

        assertThrows(IllegalStateException.class, () -> service.crearPujaSorteo(dto("20")));
        verify(pujaSorteoRepository, never()).save(any());
    }

    @Test
    void sorteoConGanadorSeRechazaAunqueNoHayaVencido() {
        Sorteo s = sorteo(HOY.minusDays(1), HOY.plusDays(3), 10);
        s.setGanador(7L);
        when(sorteoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s));

        assertThrows(IllegalStateException.class, () -> service.crearPujaSorteo(dto("20")));
        verify(pujaSorteoRepository, never()).save(any());
    }

    @Test
    void sorteoQueNoHaEmpezadoSeRechaza() {
        when(sorteoRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(sorteo(HOY.plusDays(1), HOY.plusDays(5), 10)));

        assertThrows(IllegalStateException.class, () -> service.crearPujaSorteo(dto("20")));
        verify(pujaSorteoRepository, never()).save(any());
    }

    @Test
    void sorteoInexistenteDevuelveNoEncontrado() {
        when(sorteoRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.crearPujaSorteo(dto("20")));
    }

    @Test
    void participacionCorrectaDescuentaPuntosYUsaLaFechaDelServidor() {
        sorteoAbierto(10);
        Usuario u = usuario(100);
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(u));
        PujaSorteoCreateDTO peticion = dto("30");
        peticion.setFecha(LocalDate.of(1999, 1, 1));

        service.crearPujaSorteo(peticion);

        assertEquals(70, u.getPuntos());
        verify(usuarioRepository).save(u);
        verify(pujaSorteoRepository).save(argThat(p -> HOY.equals(p.getFecha())
                && p.getPujador() == u));
    }

    @Test
    void dobleGastoDeLosMismosPuntosSeRechazaLaSegundaVez() {
        sorteoAbierto(10);
        Usuario u = usuario(50);
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(u));

        service.crearPujaSorteo(dto("40"));

        assertThrows(IllegalArgumentException.class, () -> service.crearPujaSorteo(dto("40")));
        assertEquals(10, u.getPuntos());
        verify(pujaSorteoRepository, times(1)).save(any());
    }

    @Test
    void consultarLaParticipacionDeOtroUsuarioSeRechaza() {
        PujaSorteo ajena = new PujaSorteo();
        ajena.setPujador(usuario(0)); // usuario con id 5
        when(pujaSorteoRepository.findById(9L)).thenReturn(Optional.of(ajena));

        assertThrows(ForbiddenException.class, () -> service.buscarPujaSorteoId(9L, 6L));
        verify(pujaSorteoMapper, never()).pujaSorteoToPujaSorteoResponseDTO(any());
    }

    @Test
    void consultarLaPropiaParticipacionEsCorrecto() {
        PujaSorteo propia = new PujaSorteo();
        propia.setPujador(usuario(0));
        when(pujaSorteoRepository.findById(9L)).thenReturn(Optional.of(propia));

        service.buscarPujaSorteoId(9L, 5L);

        verify(pujaSorteoMapper).pujaSorteoToPujaSorteoResponseDTO(propia);
    }
}
