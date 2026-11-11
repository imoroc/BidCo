package com.bidco.api_rest.service.contract;

import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;

import java.math.BigDecimal;

/**
 * Avisos por correo sobre el resultado de una subasta. Solo se envían a cuentas con el correo
 * verificado y nunca interrumpen la operación que los origina: un fallo del proveedor se registra y se ignora.
 */
public interface AvisoSubastaService {

    /** Avisa a quien lideraba una subasta normal de que otra puja lo ha superado. */
    void pujaSuperada(Usuario superado, Subasta subasta, BigDecimal importeNuevo);

    /** Avisa al creador de que la administración ha retirado su subasta y le indica el motivo. */
    void subastaRetiradaAlCreador(Usuario creador, Subasta subasta, String motivo);

    /** Avisa a quien lideraba una subasta retirada de que se le ha devuelto el importe retenido. */
    void subastaRetiradaAlPujador(Usuario pujador, Subasta subasta, BigDecimal devuelto);

    /** Avisa al ganador de que ha ganado la subasta con el importe indicado. */
    void subastaGanada(Usuario ganador, Subasta subasta, BigDecimal importe);
}
