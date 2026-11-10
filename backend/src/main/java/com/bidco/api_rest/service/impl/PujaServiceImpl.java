package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.dto.puja.PujaCreateDTO;
import com.bidco.api_rest.dto.puja.PujaResponseDTO;
import com.bidco.api_rest.mapper.PujaMapper;
import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.PujaService;
import com.bidco.api_rest.exception.CorreoNoVerificadoException;
import com.bidco.api_rest.exception.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PujaServiceImpl implements PujaService {

    private final PujaRepository pujaRepository;
    private final PujaMapper pujaMapper;
    private final SubastaRepository subastaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;
    private final Duration ventanaAntisniping;

    public PujaServiceImpl(PujaRepository pujaRepository, PujaMapper pujaMapper, SubastaRepository subastaRepository, UsuarioRepository usuarioRepository, Clock clock,
                           @Value("${app.antisniping.minutos:5}") long minutosAntisniping) {
        this.pujaRepository = pujaRepository;
        this.pujaMapper = pujaMapper;
        this.subastaRepository = subastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
        this.ventanaAntisniping = Duration.ofMinutes(Math.max(minutosAntisniping, 0));
    }

    @Override
    @Transactional
    public PujaResponseDTO crearPuja(PujaCreateDTO pujaCreateDTO) {
        if (pujaCreateDTO == null) {
            throw new IllegalArgumentException("La puja no puede ser nula");
        }
        if (pujaCreateDTO.getImporte() == null || pujaCreateDTO.getImporte().signum() <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor que 0");
        }

        // Bloqueo de la subasta: las pujas de una misma subasta se procesan una a una.
        Subasta subasta = subastaRepository.findByIdForUpdate(pujaCreateDTO.getSubastaID())
                .orElseThrow(() -> new EntityNotFoundException("La subasta no existe"));

        // La fecha la decide el servidor; la que envía el cliente no es de fiar.
        LocalDate hoy = LocalDate.now(clock);
        if (hoy.isBefore(subasta.getFechaInicial())) {
            throw new IllegalStateException("La subasta todavía no ha comenzado");
        }
        if (!clock.instant().isBefore(subasta.getFechaFinal())) {
            throw new IllegalStateException("La subasta ya cerró");
        }

        Long pujadorId = pujaCreateDTO.getPujadorId();
        if (subasta.getCreador() != null && subasta.getCreador().getUsuarioID().equals(pujadorId)) {
            throw new ForbiddenException("No puedes pujar en una subasta que has creado");
        }

        BigDecimal importe = pujaCreateDTO.getImporte();
        Puja pujaAnterior = pujaRepository.findTopBySubastaIdOrderByImporteDesc(subasta.getSubastaID());
        BigDecimal precioInicial = subasta.getPrecioInicial() != null ? subasta.getPrecioInicial() : BigDecimal.ZERO;

        // Subasta normal: hay que superar la puja visible. Subasta a ciegas: solo el precio base,
        // para no revelar a los participantes cuánto han ofrecido los demás.
        if (subasta.isSubastaNormal()) {
            BigDecimal actual = pujaAnterior != null ? pujaAnterior.getImporte() : precioInicial;
            if (importe.compareTo(actual) <= 0) {
                throw new IllegalArgumentException("La puja tiene que ser mayor que el precio actual");
            }
        } else if (importe.compareTo(precioInicial) <= 0) {
            throw new IllegalArgumentException("La puja tiene que ser mayor que el precio base");
        }

        boolean lidera = pujaAnterior == null || importe.compareTo(pujaAnterior.getImporte()) > 0;

        Puja puja = pujaMapper.pujaCreateDTOToPuja(pujaCreateDTO);
        puja.setFecha(hoy);
        puja.setGanadora(false);

        if (lidera) {
            retenerSaldo(pujadorId, pujaAnterior, importe);
            // En subastas a ciegas el precio no se actualiza hasta el cierre: si no,
            // el listado público revelaría la puja máxima.
            if (subasta.isSubastaNormal()) {
                subasta.setPrecioFinal(importe);
                prolongarPlazoSiHaceFalta(subasta);
                subastaRepository.save(subasta);
            }
        }
        // Una puja a ciegas que no supera a la máxima no puede ganar: se registra sin retener saldo.

        pujaRepository.save(puja);
        return pujaMapper.pujaToPujaResponseDTO(puja);
    }

    /**
     * Anti-sniping: si una puja pasa a liderar con menos de la ventana configurada para el cierre,
     * el cierre se retrasa hasta esa ventana después de la puja, para que los demás puedan responder.
     * Nunca se adelanta un cierre. Con una ventana de 0 minutos queda desactivado.
     */
    private void prolongarPlazoSiHaceFalta(Subasta subasta) {
        if (ventanaAntisniping.isZero()) {
            return;
        }
        Instant minimo = clock.instant().plus(ventanaAntisniping);
        if (subasta.getFechaFinal().isBefore(minimo)) {
            subasta.setFechaFinal(minimo);
        }
    }

    /**
     * Devuelve el saldo retenido a la puja líder anterior y retiene el de la nueva.
     * Los usuarios se bloquean en orden de id para evitar interbloqueos entre subastas.
     */
    private void retenerSaldo(Long pujadorId, Puja pujaAnterior, BigDecimal importe) {
        Long anteriorId = pujaAnterior != null ? pujaAnterior.getPujador().getUsuarioID() : null;

        Usuario pujador;
        Usuario anterior = null;
        if (anteriorId == null || anteriorId.equals(pujadorId)) {
            pujador = bloquearUsuario(pujadorId);
            if (anteriorId != null) {
                anterior = pujador;
            }
        } else if (anteriorId < pujadorId) {
            anterior = bloquearUsuario(anteriorId);
            pujador = bloquearUsuario(pujadorId);
        } else {
            pujador = bloquearUsuario(pujadorId);
            anterior = bloquearUsuario(anteriorId);
        }

        if (anterior != null) {
            anterior.setSaldo(anterior.getSaldo().add(pujaAnterior.getImporte()));
        }

        if (!pujador.isCorreoVerificado()) {
            throw new CorreoNoVerificadoException();
        }
        if (pujador.getSaldo().compareTo(importe) < 0) {
            // La transacción se revierte y la devolución anterior no llega a persistirse.
            throw new IllegalArgumentException("Saldo insuficiente");
        }
        pujador.setSaldo(pujador.getSaldo().subtract(importe));

        if (anterior != null && anterior != pujador) {
            usuarioRepository.save(anterior);
        }
        usuarioRepository.save(pujador);
    }

    private Usuario bloquearUsuario(Long usuarioId) {
        return usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
    }

    /** En una subasta a ciegas las pujas no se muestran hasta que termina el plazo. */
    private void comprobarPujasVisibles(Subasta subasta) {
        if (!subasta.isSubastaNormal() && clock.instant().isBefore(subasta.getFechaFinal())) {
            throw new ForbiddenException("Las pujas de una subasta a ciegas no son visibles hasta que termina");
        }
    }

    @Override
    public PujaResponseDTO buscarPujaPorId(Long id, Long usuarioId) {
        Puja puja = pujaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Puja no encontrada"));

        // Las pujas de una subasta a ciegas en curso solo las ve quien las hizo.
        boolean esPropia = puja.getPujador() != null && puja.getPujador().getUsuarioID().equals(usuarioId);
        if (!esPropia) {
            comprobarPujasVisibles(puja.getSubasta());
        }

        return pujaMapper.pujaToPujaResponseDTO(puja);
    }

    @Override
    public PujaResponseDTO obtenerPujaMasAlta(Long subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new EntityNotFoundException("La subasta no existe"));
        comprobarPujasVisibles(subasta);

        Puja pujaMaxima = pujaRepository.findTopBySubastaIdOrderByImporteDesc(subastaId);
        if (pujaMaxima == null) {
            throw new EntityNotFoundException("No hay pujas para esta subasta");
        }

        return pujaMapper.pujaToPujaResponseDTO(pujaMaxima);
    }

    @Override
    public List<Subasta> obtenerSubastasPorUsuarioId(Long usuarioId) {
        return pujaRepository.findSubastasByUsuarioId(usuarioId);
    }

}
