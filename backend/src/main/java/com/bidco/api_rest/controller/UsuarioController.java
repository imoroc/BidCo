package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.usuario.LoginDTO;
import com.bidco.api_rest.dto.usuario.PerfilPublicoDTO;
import com.bidco.api_rest.dto.usuario.PrivacidadDTO;
import com.bidco.api_rest.dto.usuario.StatsDTO;
import com.bidco.api_rest.dto.usuario.UsuarioCreateDTO;
import com.bidco.api_rest.dto.usuario.UsuarioResponseDTO;
import com.bidco.api_rest.dto.usuario.UsuarioUpdateDTO;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.service.contract.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public UsuarioResponseDTO registrarUsuario(@Valid @RequestBody UsuarioCreateDTO usuarioCreateDTO) {
        return usuarioService.registrarUsuario(usuarioCreateDTO);
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = usuarioService.login(loginDTO);
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
            @RequestBody Map<String, String> passwords,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        usuarioService.updatePassword(id, passwords.get("currentPassword"), passwords.get("newPassword"));
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
            @RequestBody PrivacidadDTO privacidadUpdateDTO,
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
            @RequestBody Map<String, BigDecimal> body,
            @RequestAttribute("authenticatedUserId") Long authenticatedUserId) {
        requireOwner(id, authenticatedUserId);
        BigDecimal cantidad = body.get("cantidad");
        return ResponseEntity.ok(usuarioService.recargarSaldo(id, cantidad));
    }

    private void requireOwner(Long resourceUserId, Long authenticatedUserId) {
        if (authenticatedUserId == null || !resourceUserId.equals(authenticatedUserId)) {
            throw new ForbiddenException("No puedes acceder ni modificar los datos de otro usuario");
        }
    }
}
