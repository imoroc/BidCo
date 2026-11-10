package com.bidco.api_rest.service.contract;

import java.math.BigDecimal;

import com.bidco.api_rest.dto.usuario.ExportacionDatosDTO;
import com.bidco.api_rest.dto.usuario.LoginDTO;
import com.bidco.api_rest.dto.usuario.PerfilPublicoDTO;
import com.bidco.api_rest.dto.usuario.PrivacidadDTO;
import com.bidco.api_rest.dto.usuario.StatsDTO;
import com.bidco.api_rest.dto.usuario.UsuarioCreateDTO;
import com.bidco.api_rest.dto.usuario.UsuarioResponseDTO;
import com.bidco.api_rest.dto.usuario.UsuarioUpdateDTO;

public interface UsuarioService {

    UsuarioResponseDTO registrarUsuario(UsuarioCreateDTO usuarioCreateDTO);
    UsuarioResponseDTO actualizarUsuario(Long id, UsuarioUpdateDTO usuarioUpdateDTO);
    UsuarioResponseDTO buscarUsuarioPorId(Long id);
    UsuarioResponseDTO buscarUsuarioPorNombreUsuario(String nombreUsuario);
    PerfilPublicoDTO obtenerPerfilPublico(Long id);
    PerfilPublicoDTO buscarPerfilPublicoPorNombreUsuario(String nombreUsuario);
    void updatePassword(Long id, String password, String newPassword);
    PrivacidadDTO getPrivacidad(Long id);
    UsuarioResponseDTO actualizarPrivacidad(Long id, PrivacidadDTO privacidadDTO);
    StatsDTO getStats(Long usuarioId);
    BigDecimal recargarSaldo(Long usuarioId, BigDecimal cantidad);
    String login(LoginDTO loginDTO);
    void eliminarCuenta(Long id, String contrasena);
    ExportacionDatosDTO exportarDatos(Long id);
}
