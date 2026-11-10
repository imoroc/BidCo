package com.bidco.api_rest.mapper;

import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.dto.subasta.SubastaCreateDTO;
import com.bidco.api_rest.dto.subasta.SubastaResponseDTO;
import com.bidco.api_rest.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;


@Mapper(componentModel = "spring")
public interface SubastaMapper {
    
    // Mapear Subasta a SubastaResponseDTO
    @Mapping(source = "subastaID", target = "subastaID")
    @Mapping(source = "fechaInicial", target = "fechaInicial")
    @Mapping(source = "fechaFinal", target = "fechaFinal")
    @Mapping(source = "precioInicial", target = "precioInicial")
    @Mapping(source = "precioFinal", target = "precioFinal")
    @Mapping(source = "subastaNormal", target = "subastaNormal")
    @Mapping(source = "nombreArticulo", target = "nombreArticulo")
    @Mapping(source = "descripcion", target = "descripcion")
    @Mapping(source = "creador.usuarioID", target = "creadorId")
    @Mapping(source = "imagen", target = "imagen")
    @Mapping(source = "categoria", target = "categoria")
    @Mapping(source = "ganador", target = "ganador")
    SubastaResponseDTO subastaToSubastaResponseDTO(Subasta subasta);

    @Named("longToUsuario")
    default Usuario longToUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null; // Maneja como sea necesario si es nulo
        }
        Usuario usuario = new Usuario();
        usuario.setUsuarioID(usuarioId);
        return usuario;
    }

    // Mapear SubastaCreateDTO a Subasta
    @Mapping(source = "subastaID", target = "subastaID")
    @Mapping(source = "creadorId", target = "creador", qualifiedByName = "longToUsuario")
    @Mapping(source = "fechaInicial", target = "fechaInicial")
    @Mapping(target = "fechaFinal", ignore = true)
    @Mapping(source = "precioInicial", target = "precioInicial")
    @Mapping(source = "subastaNormal", target = "subastaNormal")
    @Mapping(source = "nombreArticulo", target = "nombreArticulo")
    @Mapping(source = "descripcion", target = "descripcion")
    @Mapping(source = "imagen", target = "imagen")
    @Mapping(source = "precioFinal", target = "precioFinal")
    Subasta subastaCreateDTOToSubasta(SubastaCreateDTO subastaCreateDTO);

}
