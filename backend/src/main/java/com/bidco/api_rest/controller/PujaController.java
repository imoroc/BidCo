package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.puja.PujaCreateDTO;
import com.bidco.api_rest.dto.puja.PujaResponseDTO;
import com.bidco.api_rest.dto.subasta.SubastaResponseDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.mapper.SubastaMapper;
import com.bidco.api_rest.service.contract.PujaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pujas")
public class PujaController {

    private final PujaService pujaService;
    private final SubastaMapper subastaMapper;

    public PujaController(PujaService pujaService, SubastaMapper subastaMapper) {
        this.pujaService = pujaService;
        this.subastaMapper = subastaMapper;
    }

    @PostMapping
    public PujaResponseDTO crearPuja(
            @Valid @RequestBody PujaCreateDTO pujaCreateDTO,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        // La identidad procede del JWT, no del identificador enviado por el navegador.
        pujaCreateDTO.setPujadorId(authenticatedUserId);
        return pujaService.crearPuja(pujaCreateDTO);
    }

    @GetMapping("/{id}")
    public PujaResponseDTO buscarPujaPorId(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        return pujaService.buscarPujaPorId(id, authenticatedUserId);
    }

    @GetMapping("/mayor/{subastaId}")
    public PujaResponseDTO obtenerPujaMasAlta(@PathVariable Long subastaId) {
        return pujaService.obtenerPujaMasAlta(subastaId);
    }

    @GetMapping("/subastas-usuario/{usuarioId}")
    public List<SubastaResponseDTO> obtenerSubastasPorUsuario(
            @PathVariable Long usuarioId,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        if (!usuarioId.equals(authenticatedUserId)) {
            throw new ForbiddenException("No puedes consultar las pujas de otro usuario");
        }
        return pujaService.obtenerSubastasPorUsuarioId(authenticatedUserId)
                .stream()
                .map(subastaMapper::subastaToSubastaResponseDTO)
                .toList();
    }
}
