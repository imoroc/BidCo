package com.bidco.api_rest.controller;

import com.bidco.api_rest.config.LoginRateLimiter;
import com.bidco.api_rest.dto.usuario.CambiarContrasenaDTO;
import com.bidco.api_rest.dto.usuario.EliminarCuentaDTO;
import com.bidco.api_rest.dto.usuario.ExportacionDatosDTO;
import com.bidco.api_rest.dto.usuario.LoginDTO;
import com.bidco.api_rest.dto.usuario.PerfilPublicoDTO;
import com.bidco.api_rest.dto.usuario.PrivacidadDTO;
import com.bidco.api_rest.dto.usuario.RecargaSaldoDTO;
import com.bidco.api_rest.dto.usuario.StatsDTO;
import com.bidco.api_rest.dto.usuario.UsuarioCreateDTO;
import com.bidco.api_rest.dto.usuario.UsuarioResponseDTO;
import com.bidco.api_rest.dto.usuario.UsuarioUpdateDTO;
import com.bidco.api_rest.dto.usuario.VerificarCorreoDTO;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.exception.ContrasenaActualIncorrectaException;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.service.contract.UsuarioService;
import com.bidco.api_rest.service.contract.VerificacionCorreoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final LoginRateLimiter loginRateLimiter;
    private final VerificacionCorreoService verificacionCorreoService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService, LoginRateLimiter loginRateLimiter,
                             VerificacionCorreoService verificacionCorreoService) {
        this.usuarioService = usuarioService;
        this.loginRateLimiter = loginRateLimiter;
        this.verificacionCorreoService = verificacionCorreoService;
    }

    @PostMapping
    public UsuarioResponseDTO registrarUsuario(@Valid @RequestBody UsuarioCreateDTO usuarioCreateDTO) {
        return usuarioService.registrarUsuario(usuarioCreateDTO);
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String key = loginRateLimiter.key("user", loginDTO.getIdentificador(), request.getRemoteAddr());
        loginRateLimiter.assertAllowed(key);
        String token;
        try {
            token = usuarioService.login(loginDTO);
        } catch (AuthenticationFailedException exception) {
            loginRateLimiter.recordFailure(key);
            throw exception;
        }
        loginRateLimiter.reset(key);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return response;
    }

    @GetMapping("/{id}/publico")
    public PerfilPublicoDTO obtenerPerfilPublico(@PathVariable Long id) {
        return usuarioService.obtenerPerfilPublico(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateDTO usuarioUpdateDTO,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, usuarioUpdateDTO));
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarUsuarioPorId(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        return usuarioService.buscarUsuarioPorId(id);
    }

    @GetMapping("/buscar")
    public PerfilPublicoDTO buscarUsuarioPorNombreUsuario(@RequestParam String nombreUsuario) {
        return usuarioService.buscarPerfilPublicoPorNombreUsuario(nombreUsuario);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody CambiarContrasenaDTO passwords,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId,
            HttpServletRequest request) {
        requireOwner(id, authenticatedUserId);
        // Limita los intentos con la contraseña actual por si alguien usa un token robado.
        String key = loginRateLimiter.key("password", String.valueOf(id), request.getRemoteAddr());
        loginRateLimiter.assertAllowed(key);
        try {
            usuarioService.updatePassword(id, passwords.getCurrentPassword(), passwords.getNewPassword());
        } catch (ContrasenaActualIncorrectaException exception) {
            loginRateLimiter.recordFailure(key);
            throw exception;
        }
        loginRateLimiter.reset(key);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/privacidad")
    public ResponseEntity<PrivacidadDTO> obtenerPrivacidad(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        return ResponseEntity.ok(usuarioService.getPrivacidad(id));
    }

    @PutMapping("/{id}/privacidad")
    public ResponseEntity<UsuarioResponseDTO> actualizarPrivacidad(
            @PathVariable Long id,
            @Valid @RequestBody PrivacidadDTO privacidadUpdateDTO,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        return ResponseEntity.ok(usuarioService.actualizarPrivacidad(id, privacidadUpdateDTO));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<StatsDTO> getStats(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        return ResponseEntity.ok(usuarioService.getStats(id));
    }

    @PatchMapping("/{id}/recargar-saldo")
    public ResponseEntity<BigDecimal> recargarSaldo(
            @PathVariable Long id,
            @Valid @RequestBody RecargaSaldoDTO body,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        return ResponseEntity.ok(usuarioService.recargarSaldo(id, body.getCantidad()));
    }

    @PostMapping("/verificar-correo")
    public ResponseEntity<Void> verificarCorreo(@Valid @RequestBody VerificarCorreoDTO body) {
        verificacionCorreoService.verificar(body.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reenviar-verificacion")
    public ResponseEntity<Void> reenviarVerificacion(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        verificacionCorreoService.reenviar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/exportar")
    public ResponseEntity<ExportacionDatosDTO> exportarDatos(
            @PathVariable Long id,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bidco-mis-datos.json\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(usuarioService.exportarDatos(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCuenta(
            @PathVariable Long id,
            @Valid @RequestBody EliminarCuentaDTO body,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId,
            HttpServletRequest request) {
        requireOwner(id, authenticatedUserId);
        // Mismo límite de intentos que el cambio de contraseña: protege frente a un token robado.
        String key = loginRateLimiter.key("eliminar", String.valueOf(id), request.getRemoteAddr());
        loginRateLimiter.assertAllowed(key);
        try {
            usuarioService.eliminarCuenta(id, body.getContrasena());
        } catch (ContrasenaActualIncorrectaException exception) {
            loginRateLimiter.recordFailure(key);
            throw exception;
        }
        loginRateLimiter.reset(key);
        return ResponseEntity.noContent().build();
    }

    private void requireOwner(Long resourceUserId, Long authenticatedUserId) {
        if (authenticatedUserId == null || !resourceUserId.equals(authenticatedUserId)) {
            throw new ForbiddenException("No puedes acceder ni modificar los datos de otro usuario");
        }
    }
}
