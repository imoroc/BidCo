package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.dto.FiltroDTO;
import com.bidco.api_rest.dto.sorteo.SorteoCreateDTO;
import com.bidco.api_rest.dto.sorteo.SorteoResponseDTO;
import com.bidco.api_rest.mapper.PujaSorteoMapper;
import com.bidco.api_rest.mapper.SorteoMapper;
import com.bidco.api_rest.model.PujaSorteo;
import com.bidco.api_rest.model.Sorteo;
import com.bidco.api_rest.repository.PujaSorteoRepository;
import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.CloudinaryService;
import com.bidco.api_rest.service.contract.SorteoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import com.bidco.api_rest.util.FechasCierre;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SorteoServiceImpl implements SorteoService {

    private final SorteoRepository sorteoRepository;
    private final SorteoMapper sorteoMapper;
    private final PujaSorteoMapper pujaSorteoMapper;
    private final PujaSorteoRepository pujaSorteoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CloudinaryService cloudinaryService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    // Constructor actualizado con CloudinaryService
    public SorteoServiceImpl(SorteoRepository sorteoRepository, SorteoMapper sorteoMapper, 
                            PujaSorteoMapper pujaSorteoMapper, PujaSorteoRepository pujaSorteoRepository, 
                            UsuarioRepository usuarioRepository, CloudinaryService cloudinaryService, Clock clock) {
        this.sorteoRepository = sorteoRepository;
        this.sorteoMapper = sorteoMapper;
        this.pujaSorteoMapper = pujaSorteoMapper;
        this.pujaSorteoRepository = pujaSorteoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cloudinaryService = cloudinaryService;
        this.clock = clock;
    }

    @Override
    public SorteoResponseDTO crearSorteo(SorteoCreateDTO sorteoCreateDTO) {
        if(sorteoCreateDTO == null){
            throw new IllegalArgumentException("el Sorteo no puede ser nulo");
        }
        
        Sorteo sorteo = sorteoMapper.sorteoCreateDTOToSorteo(sorteoCreateDTO);
        sorteo.setFechaFin(FechasCierre.parsear(sorteoCreateDTO.getFechaFin(), clock.getZone()));

        if (sorteoCreateDTO.getImagen() != null && !sorteoCreateDTO.getImagen().isEmpty()) {
            try {
                // Subimos la imagen a la nube y guardamos la URL
                String urlImagen = cloudinaryService.uploadFile(sorteoCreateDTO.getImagen());
                sorteo.setImagen(urlImagen);
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Error al subir la imagen del sorteo a Cloudinary", e);
            }
        }

        sorteoRepository.save(sorteo);
        return sorteoMapper.sorteoToSorteoResponseDTO(sorteo);
    }

    @Override
    public SorteoResponseDTO buscarSorteoPorId(Long id) {
        Sorteo sorteo = sorteoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("el Sorteo no existe"));
        return sorteoMapper.sorteoToSorteoResponseDTO(sorteo);
    }

    @Override
    @Transactional
    public SorteoResponseDTO asignarGanadorYActualizarPrecioFinal(Long id) {
        Sorteo sorteo = sorteoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el sorteo con ID: " + id));

        // El resultado de un sorteo es definitivo: no se puede volver a tirar.
        if (sorteo.getGanador() != null) {
            return sorteoMapper.sorteoToSorteoResponseDTO(sorteo);
        }

        if (clock.instant().isBefore(sorteo.getFechaFin())) {
            throw new IllegalStateException("El sorteo todavía no ha finalizado");
        }

        List<PujaSorteo> participaciones = pujaSorteoRepository.findBySorteoSorteoID(sorteo.getSorteoID());

        if (!participaciones.isEmpty()) {
            int totalPuntos = participaciones.stream().mapToInt(p -> p.getPuntos().intValue()).sum();
            if (totalPuntos > 0) {
                int corte = random.nextInt(totalPuntos);
                int acumulado = 0;
                PujaSorteo elegida = participaciones.get(participaciones.size() - 1);
                for (PujaSorteo participacion : participaciones) {
                    acumulado += participacion.getPuntos().intValue();
                    if (corte < acumulado) {
                        elegida = participacion;
                        break;
                    }
                }
                sorteo.setGanador(elegida.getPujador().getUsuarioID());
                sorteoRepository.save(sorteo);
            }
        }

        return sorteoMapper.sorteoToSorteoResponseDTO(sorteo);
    }

    @Override
    public List<SorteoResponseDTO> obtenerTodosLosSorteos() {
        List<Sorteo> sorteos = sorteoRepository.findAllByOrderByFechaInicioDesc();
        return sorteos.stream()
                .map(sorteoMapper::sorteoToSorteoResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SorteoResponseDTO> buscarPorFiltro(FiltroDTO filtro) {
        String[] queryCategorias = filtro.categoriasNormalizadas();

        List<Sorteo> sorteo = sorteoRepository.findByFiltro(filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.sinCategorias(), filtro.getDateOrder(), filtro.paginacion());
        return sorteo.stream()
                .map(sorteoMapper::sorteoToSorteoResponseDTO)
                .toList();
    }
}