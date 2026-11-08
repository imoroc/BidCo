package com.bidco.api_rest.controller;

import com.bidco.api_rest.config.LoginRateLimiter;
import com.bidco.api_rest.dto.trabajador.LoginTrabajadorDTO;
import com.bidco.api_rest.dto.trabajador.TrabajadorCreateDTO;
import com.bidco.api_rest.dto.trabajador.TrabajadorResponseDTO;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.service.contract.TrabajadorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trabajadores")
public class TrabajadorController {

    private final TrabajadorService trabajadorService;
    private final LoginRateLimiter loginRateLimiter;

    @Autowired
    public TrabajadorController(TrabajadorService trabajadorService, LoginRateLimiter loginRateLimiter) {
        this.trabajadorService = trabajadorService;
        this.loginRateLimiter = loginRateLimiter;
    }

     // Registrar trabajador (POST)
    @PostMapping
    public TrabajadorResponseDTO registrarTrabajador(@Valid @RequestBody TrabajadorCreateDTO trabajadorCreateDTO) {
        return trabajadorService.registrarTrabajador(trabajadorCreateDTO);
    }

    // Actualizar trabajador (PUT)
    @PutMapping
    public TrabajadorResponseDTO actualizarTrabajador(
            @Valid @RequestBody TrabajadorCreateDTO trabajadorCreateDTO,
            @RequestAttribute("authenticatedSubject") String authenticatedSubject) {
        Long solicitanteId;
        try {
            solicitanteId = Long.valueOf(authenticatedSubject);
        } catch (NumberFormatException exception) {
            throw new ForbiddenException("Identidad no válida");
        }
        return trabajadorService.actualizarTrabajador(solicitanteId, trabajadorCreateDTO);
    }

    // Buscar trabajador por ID (GET)
    @GetMapping("/{id}")
    public TrabajadorResponseDTO buscarTrabajadorPorId(@PathVariable Long id) {
        return trabajadorService.buscarTrabajadorPorId(id);
    }

    // Buscar trabajador por nombre de usuario (GET)
    @GetMapping("/buscar")
    public TrabajadorResponseDTO buscarTrabajadorPorNombreUsuario(@RequestParam String nombreUsuario) {
        return trabajadorService.buscarTrabajadorPorNombreUsuario(nombreUsuario);
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginTrabajadorDTO loginDTO, HttpServletRequest request) {
        String key = loginRateLimiter.key("worker", loginDTO.getIdentificador(), request.getRemoteAddr());
        loginRateLimiter.assertAllowed(key);
        String token;
        try {
            token = trabajadorService.login(loginDTO);
        } catch (AuthenticationFailedException exception) {
            loginRateLimiter.recordFailure(key);
            throw exception;
        }
        loginRateLimiter.reset(key);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return response;
    }
}
