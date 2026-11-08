package com.bidco.api_rest.service.contract;

import java.util.List;

import com.bidco.api_rest.dto.puja.PujaCreateDTO;
import com.bidco.api_rest.dto.puja.PujaResponseDTO;
import com.bidco.api_rest.model.Subasta;

public interface PujaService {

    PujaResponseDTO crearPuja(PujaCreateDTO pujaCreateDTO);
    PujaResponseDTO buscarPujaPorId(Long id, Long usuarioId);
    public PujaResponseDTO obtenerPujaMasAlta(Long subastaId);
    List<Subasta> obtenerSubastasPorUsuarioId(Long usuarioId);
}
