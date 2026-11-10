package com.bidco.api_rest.service.impl;


import com.bidco.api_rest.dto.pujasorteo.PujaSorteoCreateDTO;
import com.bidco.api_rest.dto.pujasorteo.PujaSorteoResponseDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.PujaSorteoMapper;
import com.bidco.api_rest.model.PujaSorteo;
import com.bidco.api_rest.model.Sorteo;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaSorteoRepository;
import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.PujaSorteoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class PujaSorteoServiceImpl implements PujaSorteoService {

    private final PujaSorteoRepository pujaSorteoRepository;
    private final PujaSorteoMapper pujaSorteoMapper;
    private final SorteoRepository sorteoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;

    public PujaSorteoServiceImpl(PujaSorteoRepository pujaSorteoRepository, PujaSorteoMapper pujaSorteoMapper,
                                 SorteoRepository sorteoRepository, UsuarioRepository usuarioRepository,
                                 Clock clock) {
        this.pujaSorteoRepository = pujaSorteoRepository;
        this.pujaSorteoMapper = pujaSorteoMapper;
        this.sorteoRepository = sorteoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PujaSorteoResponseDTO crearPujaSorteo(PujaSorteoCreateDTO pujaSorteoCreateDTO) {
        if (pujaSorteoCreateDTO == null) {
            throw new IllegalArgumentException("La puja no puede ser nula");
        }
        if (pujaSorteoCreateDTO.getPuntos() == null || pujaSorteoCreateDTO.getPuntos().signum() <= 0) {
            throw new IllegalArgumentException("Los puntos deben ser mayores que 0");
        }
        if (pujaSorteoCreateDTO.getPuntos().stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Los puntos deben ser un número entero");
        }
        int puntos = pujaSorteoCreateDTO.getPuntos().intValueExact();

        // Bloqueo del sorteo: la participación no puede cruzarse con el cierre del sorteo.
        Sorteo sorteo = sorteoRepository.findByIdForUpdate(pujaSorteoCreateDTO.getSorteoId())
                .orElseThrow(() -> new EntityNotFoundException("El sorteo no existe"));

        // La fecha la decide el servidor; la que envía el cliente no es de fiar.
        LocalDate hoy = LocalDate.now(clock);
        if (sorteo.getGanador() != null || !clock.instant().isBefore(sorteo.getFechaFin())) {
            throw new IllegalStateException("El sorteo ya ha finalizado");
        }
        if (hoy.isBefore(sorteo.getFechaInicio())) {
            throw new IllegalStateException("El sorteo todavía no ha comenzado");
        }
        if (puntos < sorteo.getPuntosNecesarios()) {
            throw new IllegalArgumentException("Debes aportar al menos " + sorteo.getPuntosNecesarios()
                    + " puntos para participar");
        }

        // Bloqueo de la fila del usuario: evita gastar dos veces los mismos puntos en paralelo.
        Usuario usuario = usuarioRepository.findByIdForUpdate(pujaSorteoCreateDTO.getPujadorId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        if (usuario.getPuntos() < puntos) {
            throw new IllegalArgumentException("No tienes puntos suficientes");
        }
        usuario.setPuntos(usuario.getPuntos() - puntos);

        PujaSorteo pujaSorteo = pujaSorteoMapper.pujaSorteoCreateDTOToPujaSorteo(pujaSorteoCreateDTO);
        pujaSorteo.setSorteo(sorteo);
        pujaSorteo.setPujador(usuario);
        pujaSorteo.setFecha(hoy);

        pujaSorteo = pujaSorteoRepository.save(pujaSorteo);
        usuarioRepository.save(usuario);
        return pujaSorteoMapper.pujaSorteoToPujaSorteoResponseDTO(pujaSorteo);
    }

    @Override
    public PujaSorteoResponseDTO buscarPujaSorteoId(Long id, Long usuarioId) {
        PujaSorteo pujaSorteo = pujaSorteoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Puja del sorteo no encontrada"));

        // Una participación solo la puede consultar quien la hizo.
        if (pujaSorteo.getPujador() == null || !pujaSorteo.getPujador().getUsuarioID().equals(usuarioId)) {
            throw new ForbiddenException("No puedes consultar la participación de otro usuario");
        }

        return pujaSorteoMapper.pujaSorteoToPujaSorteoResponseDTO(pujaSorteo);
    }
}
