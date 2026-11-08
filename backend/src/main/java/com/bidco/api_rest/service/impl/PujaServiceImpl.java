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
import com.bidco.api_rest.exception.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PujaServiceImpl implements PujaService {

    private final PujaRepository pujaRepository;
    private final PujaMapper pujaMapper;
    private final SubastaRepository subastaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public PujaServiceImpl(PujaRepository pujaRepository, PujaMapper pujaMapper, SubastaRepository subastaRepository, UsuarioRepository usuarioRepository, Clock clock) {
        this.pujaRepository = pujaRepository;
        this.pujaMapper = pujaMapper;
        this.subastaRepository = subastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
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
        if (hoy.isAfter(subasta.getFechaFinal())) {
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
            subasta.setPrecioFinal(importe);
            subastaRepository.save(subasta);
        }
        // Una puja a ciegas que no supera a la máxima no puede ganar: se registra sin retener saldo.

        pujaRepository.save(puja);
        return pujaMapper.pujaToPujaResponseDTO(puja);
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

    @Override
    public PujaResponseDTO buscarPujaPorId(Long id) {
        Puja puja = pujaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Puja no encontrada"));

        return pujaMapper.pujaToPujaResponseDTO(puja);
    }

    @Override
    public PujaResponseDTO obtenerPujaMasAlta(Long subastaId) {
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
