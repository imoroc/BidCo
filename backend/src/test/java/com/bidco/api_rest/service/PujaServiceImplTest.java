package com.bidco.api_rest.service;

import com.bidco.api_rest.dto.puja.PujaCreateDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.PujaMapper;
import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.exception.CorreoNoVerificadoException;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.AvisoSubastaService;
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
    private AvisoSubastaService avisos;
    private PujaServiceImpl service;

    @BeforeEach
    void setUp() {
        pujaRepository = mock(PujaRepository.class);
        pujaMapper = mock(PujaMapper.class);
        subastaRepository = mock(SubastaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        avisos = mock(AvisoSubastaService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
        service = new PujaServiceImpl(pujaRepository, pujaMapper, subastaRepository, usuarioRepository, avisos, clock, 5L);
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

    /** Hace una puja válida de 20 sobre la subasta dada (precio base 10, pujador con saldo de sobra). */
    private void pujaValidaSobre(Subasta s) {
        when(subastaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s));
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(usuario(5L, "100")));
        when(pujaMapper.pujaCreateDTOToPuja(any())).thenReturn(new Puja());
        service.crearPuja(dto("20"));
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
    void pujaConElCorreoSinVerificarSeRechaza() {
        when(subastaRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(subasta(true, HOY.minusDays(1), HOY.plusDays(5))));
        Usuario sinVerificar = usuario(5L, "100");
        sinVerificar.setCorreoVerificado(false);
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sinVerificar));
        when(pujaMapper.pujaCreateDTOToPuja(any())).thenReturn(new Puja());

        assertThrows(CorreoNoVerificadoException.class, () -> service.crearPuja(dto("20")));
        verify(pujaRepository, never()).save(any());
        assertEquals(0, new BigDecimal("100").compareTo(sinVerificar.getSaldo()));
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
    void unaPujaEnLosUltimosMinutosProlongaElCierre() {
        Subasta s = subasta(true, HOY.minusDays(1), HOY);
        s.setFechaFinal(Instant.parse("2026-06-15T10:02:00Z"));
        pujaValidaSobre(s);

        // La puja entra a las 10:00: el cierre pasa a ser 5 minutos después.
        assertEquals(Instant.parse("2026-06-15T10:05:00Z"), s.getFechaFinal());
        verify(subastaRepository).save(s);
    }

    @Test
    void unaPujaConTiempoDeSobraNoCambiaElCierre() {
        Subasta s = subasta(true, HOY.minusDays(1), HOY);
        s.setFechaFinal(Instant.parse("2026-06-15T10:30:00Z"));
        pujaValidaSobre(s);

        assertEquals(Instant.parse("2026-06-15T10:30:00Z"), s.getFechaFinal());
    }

    @Test
    void unaPujaJustoEnElLimiteDeLaVentanaNoCambiaElCierre() {
        Subasta s = subasta(true, HOY.minusDays(1), HOY);
        s.setFechaFinal(Instant.parse("2026-06-15T10:05:00Z"));
        pujaValidaSobre(s);

        assertEquals(Instant.parse("2026-06-15T10:05:00Z"), s.getFechaFinal());
    }

    @Test
    void enSubastaACiegasElCierreNoSeProrroga() {
        Subasta s = subasta(false, HOY.minusDays(1), HOY);
        s.setFechaFinal(Instant.parse("2026-06-15T10:02:00Z"));
        pujaValidaSobre(s);

        assertEquals(Instant.parse("2026-06-15T10:02:00Z"), s.getFechaFinal());
    }

    @Test
    void conLaVentanaACeroElAntisnipingEstaDesactivado() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
        PujaServiceImpl sinAntisniping = new PujaServiceImpl(pujaRepository, pujaMapper, subastaRepository, usuarioRepository, avisos, clock, 0L);
        Subasta s = subasta(true, HOY.minusDays(1), HOY);
        s.setFechaFinal(Instant.parse("2026-06-15T10:02:00Z"));
        when(subastaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s));
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(usuario(5L, "100")));
        when(pujaMapper.pujaCreateDTOToPuja(any())).thenReturn(new Puja());

        sinAntisniping.crearPuja(dto("20"));

        assertEquals(Instant.parse("2026-06-15T10:02:00Z"), s.getFechaFinal());
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

    /** Deja a {@code anteriorId} como líder con 15 y prepara una puja de 20 del usuario 5. */
    private Usuario lideraAntes(Subasta s, long anteriorId) {
        Usuario anterior = usuario(anteriorId, "0");
        Puja pujaAnterior = new Puja();
        pujaAnterior.setPujador(anterior);
        pujaAnterior.setImporte(new BigDecimal("15"));
        when(subastaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(s));
        when(pujaRepository.findTopBySubastaIdOrderByImporteDesc(1L)).thenReturn(pujaAnterior);
        when(usuarioRepository.findByIdForUpdate(anteriorId)).thenReturn(Optional.of(anterior));
        when(usuarioRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(usuario(5L, "100")));
        when(pujaMapper.pujaCreateDTOToPuja(any())).thenReturn(new Puja());
        return anterior;
    }

    @Test
    void alSuperarAlLiderDeUnaSubastaNormalSeLeAvisa() {
        Subasta s = subasta(true, HOY.minusDays(1), HOY.plusDays(5));
        Usuario anterior = lideraAntes(s, 7L);

        service.crearPuja(dto("20"));

        verify(avisos).pujaSuperada(anterior, s, new BigDecimal("20"));
    }

    @Test
    void enSubastaACiegasNoSeAvisaDeQueHanSuperadoAlguien() {
        Subasta s = subasta(false, HOY.minusDays(1), HOY.plusDays(5));
        lideraAntes(s, 7L);

        service.crearPuja(dto("20"));

        verify(avisos, never()).pujaSuperada(any(), any(), any());
    }

    @Test
    void quienSeSuperaASiMismoNoRecibeAviso() {
        Subasta s = subasta(true, HOY.minusDays(1), HOY.plusDays(5));
        lideraAntes(s, 5L);

        service.crearPuja(dto("20"));

        verify(avisos, never()).pujaSuperada(any(), any(), any());
    }

    @Test
    void laPrimeraPujaNoAvisaANadie() {
        pujaValidaSobre(subasta(true, HOY.minusDays(1), HOY.plusDays(5)));

        verify(avisos, never()).pujaSuperada(any(), any(), any());
    }
}
