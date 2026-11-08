package com.bidco.api_rest.controller;

import com.bidco.api_rest.dto.admin.SorteoCreateDTO;
import com.bidco.api_rest.dto.admin.AdminLoginDTO;
import com.bidco.api_rest.service.contract.AdminService;
import com.bidco.api_rest.config.LoginRateLimiter;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final LoginRateLimiter loginRateLimiter;

    public AdminController(AdminService adminService, LoginRateLimiter loginRateLimiter) {
        this.adminService = adminService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @RequestBody AdminLoginDTO dto,
            HttpServletRequest request,
            HttpSession session
    ) {
        String key = loginRateLimiter.key("admin", dto == null ? null : dto.getUsername(), request.getRemoteAddr());
        loginRateLimiter.assertAllowed(key);
        try {
            adminService.login(dto, session);
        } catch (AuthenticationFailedException exception) {
            loginRateLimiter.recordFailure(key);
            throw exception;
        }
        loginRateLimiter.reset(key);
        // Nuevo identificador de sesión tras autenticarse (evita la fijación de sesión).
        request.changeSessionId();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sorteos")
    public ResponseEntity<Void> addSorteo(
        @RequestPart("sorteo") SorteoCreateDTO sorteoCreateDTO,
        @RequestPart(value = "imagen", required = false) MultipartFile imagen,
        HttpSession session
    ) {
        sorteoCreateDTO.setImagen(imagen);
        adminService.createSorteo(sorteoCreateDTO, session);
        return ResponseEntity.ok().build();
    }

}