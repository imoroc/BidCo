package com.bidco.api_rest.mapper;

import com.bidco.api_rest.dto.sorteo.SorteoCreateDTO;
import com.bidco.api_rest.dto.sorteo.SorteoResponseDTO;
import com.bidco.api_rest.model.Sorteo;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.model.Trabajador;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SorteoMapper {

// 1. Convertir SorteoCreateDTO a Sorteo
    @Mapping(target = "creador", source = "creadorId", qualifiedByName = "longToTrabajador")
    @Mapping(source = "fechaInicio", target = "fechaInicio")
    @Mapping(target = "fechaFin", ignore = true)
    @Mapping(source = "descripcion", target = "descripcion")
    @Mapping(source = "puntosNecesarios", target = "puntosNecesarios")
    @Mapping(source = "nombreArticulo", target = "nombreArticulo")
    @Mapping(target = "imagen", ignore = true) 
    Sorteo sorteoCreateDTOToSorteo(SorteoCreateDTO dto);

    // 2. Convertir Sorteo a SorteoResponseDTO
    @Mapping(source = "sorteoID", target = "sorteoID")
    @Mapping(source = "fechaInicio", target = "fechaInicio")
    @Mapping(source = "fechaFin", target = "fechaFin")
    @Mapping(source = "descripcion", target = "descripcion")
    @Mapping(source = "puntosNecesarios", target = "puntosNecesarios")
    @Mapping(source = "nombreArticulo", target = "nombreArticulo")
    @Mapping(source = "puntosFinales", target = "puntosFinales")
    @Mapping(source = "imagen", target = "imagen")
    @Mapping(source = "categoria", target = "categoria")
    @Mapping(source = "ganador", target = "ganador")
    SorteoResponseDTO sorteoToSorteoResponseDTO(Sorteo sorteo);

    // Convertir Long a Trabajador
    @Named("longToTrabajador")
    default Trabajador longToTrabajador(Long creadorId) {
        if (creadorId == null) {
            return null; // Maneja como sea necesario si es nulo
        }
        Trabajador trabajador = new Trabajador();
        trabajador.setTrabajadorID(creadorId);
        return trabajador;
    }

    // Convertir Long a Usuario (si lo necesitas en algún lugar)
    @Named("longToUsuario")
    default Usuario longToUsuario(Long usuarioId) {
        if (usuarioId == null) {
            return null; // Maneja como sea necesario si es nulo
        }
        Usuario usuario = new Usuario();
        usuario.setUsuarioID(usuarioId);
        return usuario;
    }
}
