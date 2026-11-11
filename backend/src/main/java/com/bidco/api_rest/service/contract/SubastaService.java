package com.bidco.api_rest.service.contract;

import com.bidco.api_rest.dto.puja.PujaResponseDTO;
import com.bidco.api_rest.dto.subasta.SubastaCreateDTO;
import com.bidco.api_rest.dto.subasta.SubastaResponseDTO;
import com.bidco.api_rest.dto.subasta.SubastaUpdateDTO;
import com.bidco.api_rest.dto.usuario.UsuarioResponseDTO;
import com.bidco.api_rest.dto.FiltroDTO;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface SubastaService {

    SubastaResponseDTO crearSubasta(SubastaCreateDTO subastaCreateDTO, MultipartFile imagen);
    SubastaResponseDTO buscarSubastaPorId(Long id);

    /** Cambia nombre, descripción y categoría. Solo el creador y mientras no haya pujas ni haya cerrado. */
    SubastaResponseDTO actualizarSubasta(Long id, Long usuarioId, SubastaUpdateDTO datos);

    /**
     * Retira una subasta por moderación, con independencia de quién la creó y de si tiene pujas. Devuelve el
     * importe retenido a quien lideraba y avisa al creador con el motivo. No se puede si ya tiene ganador.
     */
    void retirarPorModeracion(Long id, String motivo);

    /** Retira la subasta. Solo el creador y mientras no haya pujas ni haya cerrado. */
    void cancelarSubasta(Long id, Long usuarioId);
    void asignarGanadorYActualizarPrecioFinal(Long id);
    List<SubastaResponseDTO> buscarPorTipo(boolean normal);
    List<SubastaResponseDTO> listarSubastasPorCreador(Long id);
    List<SubastaResponseDTO> buscarPorFiltroNormal(FiltroDTO filtro);
    List<SubastaResponseDTO> buscarPorFiltroCiega(FiltroDTO filtro);
    List<SubastaResponseDTO> buscarPorFiltroMisSubastas(FiltroDTO filtro, int id);
    List<SubastaResponseDTO> buscarPorFiltroMisPujas(FiltroDTO filtro, int id);
    List<SubastaResponseDTO> buscarPorFiltroYNombre(FiltroDTO filtro, String searchTerm);
}
