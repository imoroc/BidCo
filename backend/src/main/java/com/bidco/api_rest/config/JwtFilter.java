package com.bidco.api_rest.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "POST:/api/usuarios",
            "POST:/api/usuarios/login",
            "POST:/api/trabajadores/login",
            "POST:/api/admin/login",
            "GET:/api/ping",
            "GET:/api/subastas",
            "GET:/api/subastas/filtrar",
            "GET:/api/subastas/filtro-normal",
            "GET:/api/subastas/filtro-ciega",
            "GET:/api/subastas/filtro-nombre",
            "GET:/api/sorteos",
            "GET:/api/sorteos/filtro"
    );

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method) || isPublic(method, path)) {
            chain.doFilter(request, response);
            return;
        }

        if (isPublicResource(method, path)) {
            chain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api/admin/")) {
            if (!hasAdminSession(request)) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Sesión administrativa no válida");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // El registro de trabajadores se realiza desde el área administrativa con sesión.
        if ("POST".equalsIgnoreCase(method) && "/api/trabajadores".equals(path) && hasAdminSession(request)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ") || authHeader.length() <= 7) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token no proporcionado");
            return;
        }

        final Claims claims;
        try {
            claims = jwtUtil.getClaims(authHeader.substring(7));
        } catch (Exception exception) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o caducado");
            return;
        }

        String subject = claims.getSubject();
        String role = claims.get("role", String.class);
        if (subject == null || subject.isBlank() || role == null || role.isBlank()) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "El token no contiene una identidad o rol válido");
            return;
        }

        if (!hasRequiredRole(path, method, role)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "No tienes permisos para acceder a este recurso");
            return;
        }

        request.setAttribute("authenticatedSubject", subject);
        if ("USER".equals(role)) {
            try {
                request.setAttribute("authenticatedUserId", Long.valueOf(subject));
            } catch (NumberFormatException exception) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Identidad de usuario inválida");
                return;
            }
        }

        // No incluir la ejecución del controlador dentro del try de validación JWT:
        // los errores de negocio no deben convertirse en respuestas 401.
        chain.doFilter(request, response);
    }

    private boolean hasRequiredRole(String path, String method, String role) {
        if (path.equals("/api/usuarios") || path.startsWith("/api/usuarios/")) {
            return "USER".equals(role);
        }
        if (path.equals("/api/pujas") || path.startsWith("/api/pujas/")
                || path.equals("/api/pujas-sorteo") || path.startsWith("/api/pujas-sorteo/")) {
            return "USER".equals(role);
        }
        if (path.equals("/api/trabajadores") || path.startsWith("/api/trabajadores/")) {
            return "WORKER".equals(role) || "EXPERT".equals(role);
        }
        if (path.equals("/api/comentarios") || path.startsWith("/api/comentarios/")) {
            return "GET".equalsIgnoreCase(method) || "EXPERT".equals(role);
        }
        if (path.equals("/api/subastas") || path.startsWith("/api/subastas/")) {
            return "GET".equalsIgnoreCase(method) || "USER".equals(role);
        }
        if (path.equals("/api/sorteos") || path.startsWith("/api/sorteos/")) {
            return "GET".equalsIgnoreCase(method) || "USER".equals(role);
        }
        return true;
    }

    private boolean isPublic(String method, String path) {
        for (String route : PUBLIC_ROUTES) {
            String[] parts = route.split(":", 2);
            if (parts[0].equalsIgnoreCase(method) && parts[1].equals(path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicResource(String method, String path) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        return path.matches("/api/usuarios/\\d+/publico")
                || path.matches("/api/subastas/\\d+")
                || path.matches("/api/sorteos/\\d+")
                || path.matches("/api/pujas/mayor/\\d+")
                || path.matches("/api/comentarios/\\d+")
                || path.matches("/api/comentarios/subasta/\\d+")
                || path.equals("/swagger-ui.html")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/swagger-ui")
                || path.startsWith("/swagger-ui/");
    }

    private boolean hasAdminSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute("adminId") != null;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
