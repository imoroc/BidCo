package com.bidco.api_rest.dto.usuario;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Copia de los datos que BidCo guarda de un usuario (derecho de acceso y portabilidad). Nunca incluye la
 * contraseña ni datos de otras personas.
 */
public record ExportacionDatosDTO(
        Instant generadoEn,
        Perfil perfil,
        List<PujaExportada> pujas,
        List<ParticipacionSorteoExportada> participacionesEnSorteos,
        List<SubastaCreadaExportada> subastasCreadas) {

    public record Perfil(
            String nombreUsuario,
            String correoElectronico,
            String pais,
            String ciudad,
            String codigoPostal,
            String calle,
            int numeroPiso,
            String letraPiso,
            BigDecimal saldo,
            int puntos,
            boolean privacidadAnonimoPujas,
            boolean privacidadEstadisticas,
            boolean privacidadPerfilVisible) {
    }

    public record PujaExportada(
            Long pujaId,
            LocalDate fecha,
            BigDecimal importe,
            boolean ganadora,
            Long subastaId,
            String articulo) {
    }

    public record ParticipacionSorteoExportada(
            Long pujaId,
            LocalDate fecha,
            BigDecimal puntos,
            Long sorteoId,
            String articulo) {
    }

    public record SubastaCreadaExportada(
            Long subastaId,
            String articulo,
            String descripcion,
            String categoria,
            LocalDate fechaInicial,
            Instant fechaFinal,
            BigDecimal precioInicial,
            BigDecimal precioFinal,
            boolean subastaNormal) {
    }
}
