package com.bidco.api_rest.service.contract;

import com.bidco.api_rest.dto.trabajador.LoginTrabajadorDTO;
import com.bidco.api_rest.dto.trabajador.TrabajadorCreateDTO;
import com.bidco.api_rest.dto.trabajador.TrabajadorResponseDTO;
import com.bidco.api_rest.dto.usuario.UsuarioCreateDTO;
import com.bidco.api_rest.dto.usuario.UsuarioResponseDTO;

public interface TrabajadorService {

    TrabajadorResponseDTO registrarTrabajador(TrabajadorCreateDTO trabajadorCreateDTO);
    TrabajadorResponseDTO actualizarTrabajador(Long solicitanteId, TrabajadorCreateDTO trabajadorCreateDTO);
    TrabajadorResponseDTO buscarTrabajadorPorId(Long id);
    TrabajadorResponseDTO buscarTrabajadorPorNombreUsuario(String nomreUsuario);
    String login(LoginTrabajadorDTO loginDTO);
}
