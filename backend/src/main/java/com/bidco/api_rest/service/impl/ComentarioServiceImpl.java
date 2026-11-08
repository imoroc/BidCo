package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.dto.cometario.ComentarioCreateDTO;
import com.bidco.api_rest.dto.cometario.ComentarioResponseDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.ComentarioMapper;
import com.bidco.api_rest.model.Comentario;
import com.bidco.api_rest.repository.ComentarioRepository;
import com.bidco.api_rest.service.contract.ComentarioService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComentarioServiceImpl implements ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final ComentarioMapper comentarioMapper;

    public ComentarioServiceImpl(ComentarioRepository comentarioRepository, ComentarioMapper comentarioMapper) {
        this.comentarioRepository = comentarioRepository;
        this.comentarioMapper = comentarioMapper;
    }

    @Override
    public ComentarioResponseDTO añadirComentario(Long solicitanteId, ComentarioCreateDTO comentarioCreateDTO) {
        if(comentarioCreateDTO==null){
            throw new IllegalArgumentException("el comentario no puede ser nulo");
        }
        if (solicitanteId == null) {
            throw new ForbiddenException("Identidad no válida");
        }
        // El autor es siempre quien está autenticado, no el id que venga en el cuerpo.
        comentarioCreateDTO.setTrabajadorId(solicitanteId);
        Comentario comentario = comentarioMapper.comentarioCreateDTOToComentario(comentarioCreateDTO);
        comentario = comentarioRepository.save(comentario);
        return comentarioMapper.comentarioToComentarioResponseDTO(comentario);
    }

    @Override
    public ComentarioResponseDTO buscarComentarioPorId(Long id) {
        Comentario comentario = comentarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("el comentario no existe"));

        return comentarioMapper.comentarioToComentarioResponseDTO(comentario);
    }

    @Override
    public List<ComentarioResponseDTO> buscarComentariosPorSubastaId(Long id) {
        // Llamada al repositorio para obtener los comentarios
        List<Comentario> comentarios = comentarioRepository.findComentariosBySubastaId(id);

        // Lista para almacenar los DTOs convertidos
        List<ComentarioResponseDTO> comentarioResponseDTOList = new ArrayList<>();

        // Usamos un ciclo for para convertir los Comentarios a ComentarioResponseDTO
        for (Comentario comentario : comentarios) {
            ComentarioResponseDTO dto = comentarioMapper.comentarioToComentarioResponseDTO(comentario);
            comentarioResponseDTOList.add(dto);
        }
        return comentarioResponseDTOList;
    }

    @Override
    public ComentarioResponseDTO editarComentarioPorId(Long solicitanteId, Long id, ComentarioCreateDTO comentarioDTO) {
        if (comentarioDTO == null) {
            throw new IllegalArgumentException("el comentario no puede ser nulo");
        }
        Comentario comentario = comentarioRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Comentario no encontrado con ID: " + id));

        // Cada experto solo puede editar sus propios comentarios.
        if (solicitanteId == null
                || comentario.getTrabajador() == null
                || !solicitanteId.equals(comentario.getTrabajador().getTrabajadorID())) {
            throw new ForbiddenException("No puedes editar el comentario de otro trabajador");
        }
        comentario.setComentario(comentarioDTO.getComentario());
        comentario.setPrecioEstimado(comentarioDTO.getPrecioEstimado());

        Comentario comentarioActualizado = comentarioRepository.save(comentario);

        return comentarioMapper.comentarioToComentarioResponseDTO(comentarioActualizado);
    }
}