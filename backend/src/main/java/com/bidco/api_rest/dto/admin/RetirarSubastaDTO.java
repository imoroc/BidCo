package com.bidco.api_rest.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo de una retirada por moderación; se guarda en el registro y se envía al creador. */
public class RetirarSubastaDTO {

    @NotBlank
    @Size(max = 500)
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
