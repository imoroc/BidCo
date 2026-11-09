package com.bidco.api_rest.dto.usuario;

import jakarta.validation.constraints.NotNull;

public class PrivacidadDTO {
    @NotNull(message = "Indica si esta opción de privacidad está activada")
    private Boolean privacidadAnonimoPujas;
    @NotNull(message = "Indica si esta opción de privacidad está activada")
    private Boolean privacidadEstadisticas;
    @NotNull(message = "Indica si esta opción de privacidad está activada")
    private Boolean privacidadPerfilVisible;

    // getters y setters
    public Boolean getPrivacidadAnonimoPujas() { return privacidadAnonimoPujas; }
    public void setPrivacidadAnonimoPujas(Boolean v) { this.privacidadAnonimoPujas = v; }

    public Boolean getPrivacidadEstadisticas() { return privacidadEstadisticas; }
    public void setPrivacidadEstadisticas(Boolean v) { this.privacidadEstadisticas = v; }

    public Boolean getPrivacidadPerfilVisible() { return privacidadPerfilVisible; }
    public void setPrivacidadPerfilVisible(Boolean v) { this.privacidadPerfilVisible = v; }
}