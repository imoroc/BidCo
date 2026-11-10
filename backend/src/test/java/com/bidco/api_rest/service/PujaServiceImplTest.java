package com.bidco.api_rest.service;

import com.bidco.api_rest.dto.puja.PujaCreateDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.PujaMapper;
import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.impl.PujaServiceImpl;
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

class PujaServiceImplTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    private PujaRepository pujaRepository;
    private PujaMapper pujaMapper;
    private SubastaRepository subastaRepository;
    private UsuarioRepository usuarioRepository;
    private PujaServiceImpl service;

    @BeforeEach
    void setUp() {
        pujaRepository = mock(PujaRepository.class);
        pujaMapper = mock(PujaMapper.class);
        subastaRepository = mock(SubastaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
        service = new PujaServiceImpl(pujaRepository, pujaMapper, subastaRepository, usuarioRepository, clock);
    }

    private static Instant finDeDia(LocalDate dia) {
        return dia.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Subasta subasta(boolean normal, LocalDate inicio, LocalDate fin) {
        Subasta s = new Subasta();
        s.setSubastaID(1L);
        s.setSubastaNormal(normal);
        s.setFechaInicial(inicio);
        s.setFechaFinal(finDeDia(fin));
        s.setPrecioInicial(new BigDecimal("10"));
        s.setPrecioFinal(new BigDecimal("10"));
        Usuario creador = new Usuario();
        creador.setUsuarioID(99L);
        s.setCreador(creador);
        return s;
    }

    private Usuario usuario(long id, String saldo) {
        Usuario u = new Usuario();
        u.setUsuarioID(id);
        u.setSaldo(new BigDecimal(saldo));
        return u;
    }

    private PujaCreateDTO dto(String importe) {
        PujaCreateDTO dto = new PujaCreateDTO(new BigDecimal(importe), null, 1L, 5L);
        return dto;
    }

    @Test
    void pujaEnSubastaCerradaSeRechaza() {
        when(subastaRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(subasta(true, HOY.minusDays(10), HOY.minusDays(1))));

        assertThrows(IllegalStateException.class, () -> service.crearPuja(dto("20")));
        verify(pujaRepository, never()).save(any());
    }

    @Test
    void aLaHoraExactaDeCierreYaNoSePuedePujar() {
        Subasta s = subasta(true, HOY.minusDays(1), HOY);
        s.setFechaFinal(Instant.parse("2026-06-15T10:00:00Z"));
        when(subastaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s));

        assertThrows(IllegalStateException.class, () -> service.crearPuja(dto("20")));
        verify(pujaRepository, never()).save(any());
    }

    @Test
    void unSegundoAntesDelCierreTodaviaSePuedePujar() {
        Subasta s = subasta(true, HOY.minusDays(1), HOY);
        s.setFechaFinal(Instant.parse("2026-06-15T10:00:01Z"));
        when(subastaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s));

        // El importe no supera el precio actual: falla la validación de la puja, no el plazo.
        assertThrows(IllegalArgumentException.class, () -> service.crearPuja(dto("10")));
    }

    @Test
    void pujaConSaldoInsuficienteSeRechaza() {
        when(subastaRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(subasta(true, HOY.minusDays(1), HOY.plusDays(5))));
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(usuario(5L, "5")));
        when(pujaMapper.pujaCreateDTOToPuja(any())).thenReturn(new Puja());

        assertThrows(IllegalArgumentException.class, () -> service.crearPuja(dto("20")));
        verify(pujaRepository, never()).save(any());
    }

    @Test
    void pujaNoSuperiorAlPrecioActualSeRechaza() {
        when(subastaRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(subasta(true, HOY.minusDays(1), HOY.plusDays(5))));

        assertThrows(IllegalArgumentException.class, () -> service.crearPuja(dto("10")));
    }

    @Test
    void elCreadorNoPuedePujarEnSuPropiaSubasta() {
        when(subastaRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(subasta(true, HOY.minusDays(1), HOY.plusDays(5))));
        PujaCreateDTO dto = dto("20");
        dto.setPujadorId(99L);

        assertThrows(ForbiddenException.class, () -> service.crearPuja(dto));
    }

    @Test
    void laPujaValidaRetieneSaldoYActualizaPrecioEnSubastaNormal() {
        Subasta s = subasta(true, HOY.minusDays(1), HOY.plusDays(5));
        Usuario pujador = usuario(5L, "100");
        when(subastaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s));
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(pujador));
        when(pujaMapper.pujaCreateDTOToPuja(any())).thenReturn(new Puja());

        service.crearPuja(dto("20"));

        assertEquals(new BigDecimal("80"), pujador.getSaldo());
        assertEquals(new BigDecimal("20"), s.getPrecioFinal());
        verify(pujaRepository).save(any(Puja.class));
    }

    @Test
    void enSubastaACiegasElPrecioFinalNoRevelaLaPuja() {
        Subasta s = subasta(false, HOY.minusDays(1), HOY.plusDays(5));
        when(subastaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s));
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(usuario(5L, "100")));
        when(pujaMapper.pujaCreateDTOToPuja(any())).thenReturn(new Puja());

        service.crearPuja(dto("50"));

        assertEquals(new BigDecimal("10"), s.getPrecioFinal());
    }

    @Test
    void laPujaMasAltaDeUnaSubastaACiegasNoEsVisibleMientrasEstaAbierta() {
        when(subastaRepository.findById(1L))
                .thenReturn(Optional.of(subasta(false, HOY.minusDays(1), HOY.plusDays(5))));

        assertThrows(ForbiddenException.class, () -> service.obtenerPujaMasAlta(1L));
        verify(pujaRepository, never()).findTopBySubastaIdOrderByImporteDesc(any());
    }

    @Test
    void laPujaMasAltaDeUnaSubastaACiegasEsVisibleTrasElCierre() {
        when(subastaRepository.findById(1L))
                .thenReturn(Optional.of(subasta(false, HOY.minusDays(10), HOY.minusDays(1))));
        when(pujaRepository.findTopBySubastaIdOrderByImporteDesc(1L)).thenReturn(new Puja());

        assertDoesNotThrow(() -> service.obtenerPujaMasAlta(1L));
    }
}
