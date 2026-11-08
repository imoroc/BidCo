package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.dto.subasta.SubastaCreateDTO;
import com.bidco.api_rest.dto.subasta.SubastaResponseDTO;
import com.bidco.api_rest.dto.FiltroDTO;
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
import com.bidco.api_rest.service.contract.SubastaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class SubastaServiceImpl implements SubastaService {

    private final SubastaRepository subastaRepository;
    private final SubastaMapper subastaMapper;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioRepository usuarioRepository;
    private final PujaRepository pujaRepository;
    private final PujaMapper pujaMapper;
    private final CloudinaryService cloudinaryService;
    private final Clock clock;

    public SubastaServiceImpl(SubastaRepository subastaRepository, SubastaMapper subastaMapper, UsuarioMapper usuarioMapper,
                UsuarioRepository usuarioRepository, PujaRepository pujaRepository, PujaMapper pujaMapper,
                CloudinaryService cloudinaryService, Clock clock) {
        this.subastaRepository = subastaRepository;
        this.subastaMapper = subastaMapper;
        this.usuarioMapper = usuarioMapper;
        this.usuarioRepository = usuarioRepository;
        this.pujaRepository = pujaRepository;
        this.pujaMapper = pujaMapper;
        this.cloudinaryService = cloudinaryService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SubastaResponseDTO crearSubasta(SubastaCreateDTO subastaCreateDTO, MultipartFile imagen) {
        if(subastaCreateDTO == null){
            throw new IllegalArgumentException("La subasta no puede ser nula");
        }

        Subasta subasta = subastaMapper.subastaCreateDTOToSubasta(subastaCreateDTO);
        subasta.setPrecioFinal(subasta.getPrecioInicial());

        Usuario user = usuarioRepository.findById(subasta.getCreador().getUsuarioID())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        user.setPuntos(user.getPuntos() + 50);

        if(imagen != null && !imagen.isEmpty()) {
            try {
                // 1. Subimos la imagen a Cloudinary y obtenemos la URL definitiva
                String urlImagenNube = cloudinaryService.uploadFile(imagen);
                
                // 2. Guardamos esa URL en nuestra base de datos
                subasta.setImagen(urlImagenNube);
                
            } catch (IllegalArgumentException e) {
                
                throw e;
                
            } catch (Exception e) {
                throw new RuntimeException("Error al subir la imagen a la nube", e);
            }
        }

        subastaRepository.save(subasta);
        usuarioRepository.save(user);
        
        return subastaMapper.subastaToSubastaResponseDTO(subasta);
    }

    @Override
    public SubastaResponseDTO buscarSubastaPorId(Long id) {
        Subasta subasta = subastaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subasta no encontrada"));

        return subastaMapper.subastaToSubastaResponseDTO(subasta);
    }

    @Override
    @Transactional
    public void asignarGanadorYActualizarPrecioFinal(Long id) {
        // Bloqueo de fila: dos peticiones simultáneas no pueden repartir los puntos dos veces.
        Subasta subasta = subastaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la subasta con ID: " + id));

        // Operación idempotente: si ya tiene ganador no se vuelve a puntuar.
        if (subasta.getGanador() != null) {
            return;
        }

        if (!LocalDate.now(clock).isAfter(subasta.getFechaFinal())) {
            throw new IllegalStateException("La subasta todavía no ha finalizado");
        }

        Puja puja = pujaRepository.findTopBySubastaIdOrderByImporteDesc(id);

        if (puja == null) {
            throw new IllegalStateException("No hubo ninguna puja en la subasta.");
        }

        Usuario user = usuarioRepository.findById(puja.getPujador().getUsuarioID())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        user.setPuntos(user.getPuntos() + (subasta.isSubastaNormal() ? 100 : 200));
        puja.setGanadora(true);
        subasta.setGanador(user.getUsuarioID());
        // En subastas a ciegas el precio definitivo se publica ahora, al cerrarse.
        subasta.setPrecioFinal(puja.getImporte());

        subastaRepository.save(subasta);
        pujaRepository.save(puja);
        usuarioRepository.save(user);
    }

    @Override
    public List<SubastaResponseDTO> buscarPorTipo(boolean normal) {
        List<Subasta> subastas = subastaRepository.findBySubastaNormalOrderByFechaInicialDesc(normal);
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> listarSubastasPorCreador(Long id) {
        List<Subasta> subastas = subastaRepository.findByCreadorId(id);
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroNormal(FiltroDTO filtro) {
        String[] listaCategorias = {"Tecnología", "Hogar", "Moda", "Deportes", "Juguetes", "Otros"};
        String[] queryCategorias;

        if (filtro.getCategorias() == null || filtro.getCategorias().length == 0) {
            queryCategorias = listaCategorias;
        } else {
            queryCategorias = filtro.getCategorias();
        }
        List<Subasta> subastas = subastaRepository.findByFiltroNormal(filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.getDateOrder());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroCiega(FiltroDTO filtro) {
        String[] listaCategorias = {"Tecnología", "Hogar", "Moda", "Deportes", "Juguetes", "Otros"};
        String[] queryCategorias;

        if (filtro.getCategorias() == null || filtro.getCategorias().length == 0) {
            queryCategorias = listaCategorias;
        } else {
            queryCategorias = filtro.getCategorias();
        }
        List<Subasta> subastas = subastaRepository.findByFiltroCiega(filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.getDateOrder());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroMisSubastas(FiltroDTO filtro, int id) {
        String[] listaCategorias = {"Tecnología", "Hogar", "Moda", "Deportes", "Juguetes", "Otros"};
        String[] queryCategorias;

        if (filtro.getCategorias() == null || filtro.getCategorias().length == 0) {
            queryCategorias = listaCategorias;
        } else {
            queryCategorias = filtro.getCategorias();
        }
        List<Subasta> subastas = subastaRepository.findByFiltroMisSubastas(id, filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.getDateOrder());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroMisPujas(FiltroDTO filtro, int id) {
        String[] listaCategorias = {"Tecnología", "Hogar", "Moda", "Deportes", "Juguetes", "Otros"};
        String[] queryCategorias;

        if (filtro.getCategorias() == null || filtro.getCategorias().length == 0) {
            queryCategorias = listaCategorias;
        } else {
            queryCategorias = filtro.getCategorias();
        }
        List<Subasta> subastas = subastaRepository.findByFiltroMisPujas(id, filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.getDateOrder());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroYNombre(FiltroDTO filtro, String searchTerm) {
        String[] listaCategorias = {"Tecnología", "Hogar", "Moda", "Deportes", "Juguetes", "Otros"};
        String[] queryCategorias;

        if (filtro.getCategorias() == null || filtro.getCategorias().length == 0) {
            queryCategorias = listaCategorias;
        } else {
            queryCategorias = filtro.getCategorias();
        }
        List<Subasta> subastas = subastaRepository.findByFiltroNombre(searchTerm, filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.getDateOrder());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    
}