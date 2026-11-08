package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.pujasorteo.PujaSorteoCreateDTO;
import com.bidco.api_rest.dto.pujasorteo.PujaSorteoResponseDTO;
import com.bidco.api_rest.service.contract.PujaSorteoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pujas-sorteo")
public class PujaSorteoController {

    private final PujaSorteoService pujaSorteoService;

    public PujaSorteoController(PujaSorteoService pujaSorteoService) {
        this.pujaSorteoService = pujaSorteoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PujaSorteoResponseDTO crearPujaSorteo(
            @Valid @RequestBody PujaSorteoCreateDTO pujaSorteoCreateDTO,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        // El participante se toma del token para impedir suplantación de identidad.
        pujaSorteoCreateDTO.setPujadorId(authenticatedUserId);
        return pujaSorteoService.crearPujaSorteo(pujaSorteoCreateDTO);
    }

    @GetMapping("/{id}")
    public PujaSorteoResponseDTO buscarPujaSorteoPorId(@PathVariable Long id) {
        return pujaSorteoService.buscarPujaSorteoId(id);
    }
}
