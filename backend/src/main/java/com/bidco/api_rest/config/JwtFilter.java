package com.bidco.api_rest.config;

import com.bidco.api_rest.repository.UsuarioRepository;
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
import java.util.Optional;

@Component
public class JwtFilter extends OncePerRequestFilter {

    /**
     * La sesión de administración viaja en una cookie (SameSite=None por estar el frontend en otro
     * origen). Para evitar CSRF, las peticiones que modifican datos deben llevar esta cabecera:
     * un formulario de otro sitio no puede añadirla y una petición cruzada con ella exige un
     * preflight de CORS, que solo se concede a los orígenes permitidos.
     */
    public static final String ADMIN_CSRF_HEADER = "X-Requested-With";
    private static final String ADMIN_CSRF_VALUE = "BidCo";

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "POST:/api/usuarios",
            "POST:/api/usuarios/login",
            "POST:/api/usuarios/recuperar-contrasena",
            "POST:/api/usuarios/restablecer-contrasena",
            "POST:/api/usuarios/verificar-correo",
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

    public JwtFilter(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
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
            if (!hasAdminCsrfHeader(request, method)) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Falta la cabecera de protección CSRF");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // El registro de trabajadores se realiza desde el área administrativa con sesión.
        if ("POST".equalsIgnoreCase(method) && "/api/trabajadores".equals(path) && hasAdminSession(request)) {
            if (!hasAdminCsrfHeader(request, method)) {
                sendError(response, HttpServletResponse.SC_FORBIDDEN, "Falta la cabecera de protección CSRF");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // Solo el área administrativa (con sesión) puede dar de alta trabajadores o expertos.
        if ("POST".equalsIgnoreCase(method) && "/api/trabajadores".equals(path)) {
            sendError(response, HttpServletResponse.SC_FORBIDDEN, "No tienes permisos para acceder a este recurso");
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
            final Long userId;
            try {
                userId = Long.valueOf(subject);
            } catch (NumberFormatException exception) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Identidad de usuario inválida");
                return;
            }
            // Un token sin versión (anterior a esta comprobación) cuenta como versión 0. Si la cuenta ya no
            // existe, o su versión ha subido desde que se emitió el token, la sesión está revocada.
            Integer versionDelToken = claims.get(JwtUtil.CLAIM_VERSION, Integer.class);
            Optional<Integer> versionActual = usuarioRepository.findTokenVersionById(userId);
            if (versionActual.isEmpty() || versionActual.get() != (versionDelToken == null ? 0 : versionDelToken)) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "La sesión ya no es válida. Inicia sesión de nuevo");
                return;
            }
            request.setAttribute("authenticatedUserId", userId);
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
            // Los sorteos se crean desde el área administrativa; los usuarios solo pueden consultarlos
            // y desencadenar el cierre de uno ya terminado.
            return "GET".equalsIgnoreCase(method)
                    || ("PUT".equalsIgnoreCase(method) && path.matches("/api/sorteos/final/\\d+") && "USER".equals(role));
        }
        // Denegar por defecto: una ruta nueva sin regla explícita no queda abierta a cualquier token.
        return false;
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
                || path.matches("/api/subastas/\\d+/eventos")
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

    private boolean hasAdminCsrfHeader(HttpServletRequest request, String method) {
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }
        return ADMIN_CSRF_VALUE.equals(request.getHeader(ADMIN_CSRF_HEADER));
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
