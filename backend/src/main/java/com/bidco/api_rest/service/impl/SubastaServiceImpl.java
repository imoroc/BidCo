package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.dto.subasta.SubastaCreateDTO;
import com.bidco.api_rest.dto.subasta.SubastaResponseDTO;
import com.bidco.api_rest.dto.subasta.SubastaUpdateDTO;
import com.bidco.api_rest.dto.FiltroDTO;
import com.bidco.api_rest.mapper.PujaMapper;
import com.bidco.api_rest.mapper.SubastaMapper;
import com.bidco.api_rest.mapper.UsuarioMapper;
import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.exception.CorreoNoVerificadoException;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.ComentarioRepository;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.AvisoSubastaService;
import com.bidco.api_rest.service.contract.CloudinaryService;
import com.bidco.api_rest.service.contract.SubastaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bidco.api_rest.util.FechasCierre;
import java.time.Clock;
import java.time.Instant;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class SubastaServiceImpl implements SubastaService {

    /** Puntos que recibe quien crea una subasta; se devuelven si la retira. */
    private static final int PUNTOS_POR_CREAR = 50;


    private final SubastaRepository subastaRepository;
    private final SubastaMapper subastaMapper;
    private final UsuarioMapper usuarioMapper;
    private final UsuarioRepository usuarioRepository;
    private final PujaRepository pujaRepository;
    private final PujaMapper pujaMapper;
    private final CloudinaryService cloudinaryService;
    private final AvisoSubastaService avisos;
    private final ComentarioRepository comentarioRepository;
    private final Clock clock;

    public SubastaServiceImpl(SubastaRepository subastaRepository, SubastaMapper subastaMapper, UsuarioMapper usuarioMapper,
                UsuarioRepository usuarioRepository, PujaRepository pujaRepository, PujaMapper pujaMapper,
                CloudinaryService cloudinaryService, AvisoSubastaService avisos, ComentarioRepository comentarioRepository, Clock clock) {
        this.subastaRepository = subastaRepository;
        this.subastaMapper = subastaMapper;
        this.usuarioMapper = usuarioMapper;
        this.usuarioRepository = usuarioRepository;
        this.pujaRepository = pujaRepository;
        this.pujaMapper = pujaMapper;
        this.cloudinaryService = cloudinaryService;
        this.avisos = avisos;
        this.comentarioRepository = comentarioRepository;
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

        Instant fechaFinal = FechasCierre.parsear(subastaCreateDTO.getFechaFinal(), clock.getZone());
        if (!fechaFinal.isAfter(clock.instant())) {
            throw new IllegalArgumentException("La fecha final debe ser posterior a la actual");
        }
        subasta.setFechaFinal(fechaFinal);

        Usuario user = usuarioRepository.findById(subasta.getCreador().getUsuarioID())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        
        if (!user.isCorreoVerificado()) {
            throw new CorreoNoVerificadoException();
        }
        user.setPuntos(user.getPuntos() + PUNTOS_POR_CREAR);

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
    public SubastaResponseDTO actualizarSubasta(Long id, Long usuarioId, SubastaUpdateDTO datos) {
        Subasta subasta = obtenerModificable(id, usuarioId);
        subasta.setNombreArticulo(datos.getNombreArticulo().trim());
        subasta.setDescripcion(datos.getDescripcion().trim());
        subasta.setCategoria(normalizarCategoria(datos.getCategoria()));
        subastaRepository.save(subasta);
        return subastaMapper.subastaToSubastaResponseDTO(subasta);
    }

    @Override
    @Transactional
    public void cancelarSubasta(Long id, Long usuarioId) {
        Subasta subasta = obtenerModificable(id, usuarioId);
        quitarPuntosDeCreacion(usuarioId);
        comentarioRepository.eliminarPorSubasta(id);
        subastaRepository.delete(subasta);
    }

    @Override
    @Transactional
    public void retirarPorModeracion(Long id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Indica el motivo de la retirada");
        }
        Subasta subasta = subastaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Subasta no encontrada"));
        if (subasta.getGanador() != null) {
            throw new IllegalStateException("La subasta ya se cerró con ganador y no se puede retirar");
        }

        // Solo quien lidera tiene saldo retenido (a los demás se les devolvió al ser superados).
        Puja lider = pujaRepository.findTopBySubastaIdOrderByImporteDesc(id);
        Usuario pujador = null;
        if (lider != null) {
            pujador = usuarioRepository.findByIdForUpdate(lider.getPujador().getUsuarioID())
                    .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
            pujador.setSaldo(pujador.getSaldo().add(lider.getImporte()));
            usuarioRepository.save(pujador);
        }

        Usuario creador = subasta.getCreador();
        if (creador != null) {
            quitarPuntosDeCreacion(creador.getUsuarioID());
        }

        // Los avisos leen la subasta ahora y salen solo si la transacción se confirma.
        if (creador != null) {
            avisos.subastaRetiradaAlCreador(creador, subasta, motivo.trim());
        }
        if (pujador != null) {
            avisos.subastaRetiradaAlPujador(pujador, subasta, lider.getImporte());
        }

        pujaRepository.eliminarPorSubasta(id);
        comentarioRepository.eliminarPorSubasta(id);
        subastaRepository.delete(subasta);
    }

    /** Crear la subasta dio 50 puntos: al retirarla se devuelven, para que no se pueda abusar de ello. */
    private void quitarPuntosDeCreacion(Long usuarioId) {
        Usuario creador = usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        creador.setPuntos(Math.max(0, creador.getPuntos() - PUNTOS_POR_CREAR));
        usuarioRepository.save(creador);
    }

    /**
     * Devuelve la subasta bloqueada si el usuario es su creador y todavía se puede tocar. El bloqueo es el
     * mismo que usan las pujas, así que una puja simultánea no puede colarse entre la comprobación y el cambio.
     */
    private Subasta obtenerModificable(Long id, Long usuarioId) {
        Subasta subasta = subastaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Subasta no encontrada"));
        if (subasta.getCreador() == null || !subasta.getCreador().getUsuarioID().equals(usuarioId)) {
            throw new ForbiddenException("Solo el creador puede modificar la subasta");
        }
        if (!clock.instant().isBefore(subasta.getFechaFinal())) {
            throw new IllegalStateException("La subasta ya cerró y no se puede modificar");
        }
        if (pujaRepository.haySubastaConPujas(id)) {
            throw new IllegalStateException("La subasta ya tiene pujas y no se puede modificar");
        }
        return subasta;
    }

    /** Las categorías se guardan en minúsculas y sin tildes («tecnologia», «moda»…). */
    static String normalizarCategoria(String categoria) {
        String sinTildes = Normalizer.normalize(categoria.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
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

        if (clock.instant().isBefore(subasta.getFechaFinal())) {
            throw new IllegalStateException("La subasta todavía no ha finalizado");
        }

        Puja puja = pujaRepository.findTopBySubastaIdOrderByImporteDesc(id);

        // Subasta desierta: vencida sin pujas. Es un cierre válido sin ganador, no un error;
        // no hay nadie a quien puntuar y no se modifica nada.
        if (puja == null) {
            return;
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
        avisos.subastaGanada(user, subasta, puja.getImporte());
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
        String[] queryCategorias = filtro.categoriasNormalizadas();
        List<Subasta> subastas = subastaRepository.findByFiltroNormal(filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.sinCategorias(), filtro.getDateOrder(), filtro.paginacion());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroCiega(FiltroDTO filtro) {
        String[] queryCategorias = filtro.categoriasNormalizadas();
        List<Subasta> subastas = subastaRepository.findByFiltroCiega(filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.sinCategorias(), filtro.getDateOrder(), filtro.paginacion());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroMisSubastas(FiltroDTO filtro, int id) {
        String[] queryCategorias = filtro.categoriasNormalizadas();
        List<Subasta> subastas = subastaRepository.findByFiltroMisSubastas(id, filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.sinCategorias(), filtro.getDateOrder(), filtro.paginacion());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroMisPujas(FiltroDTO filtro, int id) {
        String[] queryCategorias = filtro.categoriasNormalizadas();
        List<Subasta> subastas = subastaRepository.findByFiltroMisPujas(id, filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.sinCategorias(), filtro.getDateOrder(), filtro.paginacion());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

    @Override
    public List<SubastaResponseDTO> buscarPorFiltroYNombre(FiltroDTO filtro, String searchTerm) {
        String[] queryCategorias = filtro.categoriasNormalizadas();
        List<Subasta> subastas = subastaRepository.findByFiltroNombre(searchTerm, filtro.getMinPrice(), filtro.getMaxPrice(), queryCategorias, filtro.sinCategorias(), filtro.getDateOrder(), filtro.paginacion());
        return subastas.stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }

}
