package com.bidco.api_rest.dto;

import com.bidco.api_rest.dto.admin.AdminLoginDTO;
import com.bidco.api_rest.dto.usuario.CambiarContrasenaDTO;
import com.bidco.api_rest.dto.usuario.PrivacidadDTO;
import com.bidco.api_rest.dto.usuario.RecargaSaldoDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidacionDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private static String texto(int longitud) {
        return "x".repeat(longitud);
    }

    private int errores(Object dto) {
        return validador.validate(dto).size();
    }

    @Test
    void laPrivacidadNoAdmiteValoresNulos() {
        PrivacidadDTO dto = new PrivacidadDTO();
        assertEquals(3, errores(dto));

        dto.setPrivacidadAnonimoPujas(true);
        dto.setPrivacidadEstadisticas(false);
        dto.setPrivacidadPerfilVisible(true);
        assertEquals(0, errores(dto));
    }

    @Test
    void elCambioDeContrasenaExigeAmbasYUnaNuevaLarga() {
        CambiarContrasenaDTO dto = new CambiarContrasenaDTO();
        assertEquals(2, errores(dto));

        dto.setCurrentPassword(texto(6));
        dto.setNewPassword(texto(5));
        assertEquals(1, errores(dto));

        dto.setNewPassword(texto(20));
        assertEquals(0, errores(dto));
    }

    @Test
    void laRecargaExigeUnaCantidadPositivaConDosDecimales() {
        RecargaSaldoDTO dto = new RecargaSaldoDTO();
        assertTrue(errores(dto) > 0);

        for (String invalida : new String[]{"0", "-5", "10.123"}) {
            dto.setCantidad(new BigDecimal(invalida));
            assertTrue(errores(dto) > 0, invalida);
        }

        dto.setCantidad(new BigDecimal("25.50"));
        assertEquals(0, errores(dto));
    }

    @Test
    void elLoginDeAdminExigeUsuarioYContrasena() {
        AdminLoginDTO dto = new AdminLoginDTO();
        assertEquals(2, errores(dto));

        dto.setUsername("admin");
        dto.setPassword(texto(2).replace('x', ' '));
        assertEquals(1, errores(dto));

        dto.setPassword(texto(7));
        assertEquals(0, errores(dto));
    }
}
