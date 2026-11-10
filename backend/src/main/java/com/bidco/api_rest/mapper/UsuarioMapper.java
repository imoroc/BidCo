package com.bidco.api_rest.mapper;

import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.dto.usuario.UsuarioCreateDTO;
import com.bidco.api_rest.dto.usuario.UsuarioResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    UsuarioResponseDTO usuarioToUsuarioResponseDTO(Usuario usuario);

    @Mapping(target = "contrasena", source = "contraseña")
    @Mapping(target = "saldo", ignore = true)
    @Mapping(target = "puntos", ignore = true)
    @Mapping(target = "usuarioID", ignore = true)
    @Mapping(target = "correoVerificado", ignore = true)
    Usuario usuarioCreateDTOToUsuario(UsuarioCreateDTO usuario);
}
