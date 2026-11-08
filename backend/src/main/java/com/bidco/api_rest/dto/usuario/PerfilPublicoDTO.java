package com.bidco.api_rest.dto.usuario;

/**
 * Datos mínimos que pueden mostrarse públicamente en una ficha de producto.
 */
public record PerfilPublicoDTO(String nombreUsuario, boolean privacidadAnonimoPujas) {
}
