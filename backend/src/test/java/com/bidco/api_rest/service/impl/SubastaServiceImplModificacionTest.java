package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.dto.subasta.SubastaUpdateDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.PujaMapper;
import com.bidco.api_rest.mapper.SubastaMapper;
import com.bidco.api_rest.mapper.UsuarioMapper;
import com.bidco.api_rest.model.Puja;
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

import java.math.BigDecimal;
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
    private AvisoSubastaService avisos;
    private SubastaServiceImpl servicio;
    private Subasta subasta;
    private Usuario creador;

    @BeforeEach
    void preparar() {
        subastaRepository = mock(SubastaRepository.class);
        pujaRepository = mock(PujaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        comentarioRepository = mock(ComentarioRepository.class);
        avisos = mock(AvisoSubastaService.class);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneId.of("Europe/Madrid"));
        servicio = new SubastaServiceImpl(subastaRepository, mock(SubastaMapper.class), mock(UsuarioMapper.class),
                usuarioRepository, pujaRepository, mock(PujaMapper.class), mock(CloudinaryService.class),
                avisos, comentarioRepository, reloj);

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

    // ---- Moderación ----

    private Usuario lider(long id, String saldo, String importe) {
        Usuario pujador = new Usuario();
        pujador.setUsuarioID(id);
        pujador.setSaldo(new BigDecimal(saldo));
        Puja puja = new Puja();
        puja.setPujador(pujador);
        puja.setImporte(new BigDecimal(importe));
        when(pujaRepository.findTopBySubastaIdOrderByImporteDesc(ID)).thenReturn(puja);
        when(usuarioRepository.findByIdForUpdate(id)).thenReturn(Optional.of(pujador));
        return pujador;
    }

    @Test
    void retirarPorModeracionDevuelveElSaldoAlLiderBorraTodoYAvisa() {
        Usuario pujador = lider(8L, "5", "40");

        servicio.retirarPorModeracion(ID, " Contenido no permitido ");

        assertEquals(new BigDecimal("45"), pujador.getSaldo());
        assertEquals(70, creador.getPuntos());
        verify(avisos).subastaRetiradaAlCreador(creador, subasta, "Contenido no permitido");
        verify(avisos).subastaRetiradaAlPujador(pujador, subasta, new BigDecimal("40"));
        verify(pujaRepository).eliminarPorSubasta(ID);
        verify(comentarioRepository).eliminarPorSubasta(ID);
        verify(subastaRepository).delete(subasta);
    }

    @Test
    void retirarPorModeracionSinPujasNoTocaSaldosNiAvisaAPujadores() {
        servicio.retirarPorModeracion(ID, "Motivo");

        verify(avisos, never()).subastaRetiradaAlPujador(any(), any(), any());
        verify(subastaRepository).delete(subasta);
    }

    @Test
    void sePuedeRetirarUnaSubastaConPujasAunqueNoSeaDelUsuario() {
        when(pujaRepository.haySubastaConPujas(ID)).thenReturn(true);

        servicio.retirarPorModeracion(ID, "Motivo");

        verify(subastaRepository).delete(subasta);
    }

    @Test
    void unaSubastaConGanadorNoSePuedeRetirar() {
        subasta.setGanador(8L);

        assertThrows(IllegalStateException.class, () -> servicio.retirarPorModeracion(ID, "Motivo"));
        verify(subastaRepository, never()).delete(any());
    }

    @Test
    void retirarPorModeracionExigeMotivo() {
        assertThrows(IllegalArgumentException.class, () -> servicio.retirarPorModeracion(ID, "  "));
        assertThrows(IllegalArgumentException.class, () -> servicio.retirarPorModeracion(ID, null));
        verify(subastaRepository, never()).delete(any());
    }
}
