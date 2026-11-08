package com.bidco.api_rest.service.contract;

import com.bidco.api_rest.dto.puja.PujaCreateDTO;
import com.bidco.api_rest.dto.puja.PujaResponseDTO;
import com.bidco.api_rest.dto.pujasorteo.PujaSorteoCreateDTO;
import com.bidco.api_rest.dto.pujasorteo.PujaSorteoResponseDTO;

public interface PujaSorteoService {
    PujaSorteoResponseDTO crearPujaSorteo(PujaSorteoCreateDTO pujaCreateDTO);
    PujaSorteoResponseDTO buscarPujaSorteoId(Long id, Long usuarioId);
}
