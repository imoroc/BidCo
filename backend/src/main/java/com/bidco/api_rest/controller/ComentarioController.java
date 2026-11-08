package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.cometario.ComentarioCreateDTO;
import com.bidco.api_rest.dto.cometario.ComentarioResponseDTO;
import com.bidco.api_rest.service.contract.ComentarioService;
import com.bidco.api_rest.exception.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @PostMapping("/nuevo")
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioResponseDTO añadirComentario(
            @Valid @RequestBody ComentarioCreateDTO comentarioCreateDTO,
            @RequestAttribute("authenticatedSubject") String authenticatedSubject) {
        return comentarioService.añadirComentario(solicitanteId(authenticatedSubject), comentarioCreateDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComentarioResponseDTO> buscarComentarioPorId(@PathVariable Long id) {
        try {
            ComentarioResponseDTO comentarioResponseDTO = comentarioService.buscarComentarioPorId(id);
            return new ResponseEntity<>(comentarioResponseDTO, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/subasta/{subastaId}")
    public ResponseEntity<List<ComentarioResponseDTO>> buscarComentariosPorSubastaId(@PathVariable Long subastaId) {
        List<ComentarioResponseDTO> comentarios = comentarioService.buscarComentariosPorSubastaId(subastaId);
        return ResponseEntity.ok(comentarios);
    }

    @PutMapping("editar/{id}")
    public ComentarioResponseDTO editarComentarioPorId(
            @PathVariable Long id,
            @Valid @RequestBody ComentarioCreateDTO comentario,
            @RequestAttribute("authenticatedSubject") String authenticatedSubject) {
        return comentarioService.editarComentarioPorId(solicitanteId(authenticatedSubject), id, comentario);
    }

    private Long solicitanteId(String authenticatedSubject) {
        try {
            return Long.valueOf(authenticatedSubject);
        } catch (NumberFormatException exception) {
            throw new ForbiddenException("Identidad no válida");
        }
    }
}