package com.bidco.api_rest.controller;

import com.bidco.api_rest.config.LoginRateLimiter;
import com.bidco.api_rest.dto.usuario.RecuperarContrasenaDTO;
import com.bidco.api_rest.dto.usuario.RestablecerContrasenaDTO;
import com.bidco.api_rest.service.contract.RecuperacionContrasenaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class RecuperacionContrasenaController {

    private static final String MENSAJE_SOLICITUD =
            "Si existe una cuenta con ese correo, recibirás un enlace para restablecer la contraseña.";

    private final RecuperacionContrasenaService service;
    private final LoginRateLimiter rateLimiter;

    public RecuperacionContrasenaController(RecuperacionContrasenaService service, LoginRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<Map<String, String>> solicitar(
            @Valid @RequestBody RecuperarContrasenaDTO dto, HttpServletRequest request) {
        // Cada solicitud cuenta como intento, por correo y por IP, para evitar el envío masivo de correos.
        String porCorreo = rateLimiter.key("recuperar", dto.getCorreo(), request.getRemoteAddr());
        String porIp = rateLimiter.key("recuperar-ip", "", request.getRemoteAddr());
        rateLimiter.assertAllowed(porCorreo);
        rateLimiter.assertAllowed(porIp);
        rateLimiter.recordFailure(porCorreo);
        rateLimiter.recordFailure(porIp);

        service.solicitar(dto.getCorreo());
        return ResponseEntity.accepted().body(Map.of("mensaje", MENSAJE_SOLICITUD));
    }

    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<Map<String, String>> restablecer(
            @Valid @RequestBody RestablecerContrasenaDTO dto, HttpServletRequest request) {
        String clave = rateLimiter.key("restablecer", "", request.getRemoteAddr());
        rateLimiter.assertAllowed(clave);
        try {
            service.restablecer(dto.getToken(), dto.getContrasena());
        } catch (IllegalArgumentException e) {
            rateLimiter.recordFailure(clave);
            throw e;
        }
        rateLimiter.reset(clave);
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada. Ya puedes iniciar sesión."));
    }
}
