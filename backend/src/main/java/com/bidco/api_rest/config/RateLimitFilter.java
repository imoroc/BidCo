package com.bidco.api_rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Limita las peticiones a la API por dirección IP: un tope general para todas las rutas y topes más
 * estrictos para las que escriben datos o consumen recursos externos (registro, pujas, recarga de
 * saldo y subida de imágenes). Se ejecuta tras CORS y antes de la autenticación, de modo que el 429
 * también llega legible al navegador.
 *
 * <p>La IP se toma de {@code getRemoteAddr()}; con {@code server.forward-headers-strategy=framework}
 * es la del cliente real detrás del proxy de la plataforma.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration MINUTO = Duration.ofMinutes(1);
    private static final Duration HORA = Duration.ofHours(1);

    record Regla(String nombre, String metodo, Pattern ruta, int maximo, Duration ventana) {
        boolean aplica(String metodoPeticion, String rutaPeticion) {
            return metodo.equalsIgnoreCase(metodoPeticion) && ruta.matcher(rutaPeticion).matches();
        }
    }

    static final List<Regla> REGLAS = List.of(
            new Regla("registro", "POST", Pattern.compile("^/api/usuarios$"), 5, HORA),
            new Regla("pujas", "POST", Pattern.compile("^/api/pujas(-sorteo)?$"), 30, MINUTO),
            new Regla("recarga", "PATCH", Pattern.compile("^/api/usuarios/\\d+/recargar-saldo$"), 10, HORA),
            new Regla("imagenes", "POST", Pattern.compile("^/api/subastas$"), 10, HORA)
    );

    private final RequestRateLimiter limiter;
    private final boolean activo;
    private final int generalPorMinuto;

    public RateLimitFilter(RequestRateLimiter limiter, boolean activo, int generalPorMinuto) {
        this.limiter = limiter;
        this.activo = activo;
        this.generalPorMinuto = generalPorMinuto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !activo
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/api/ping".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String metodo = request.getMethod();
        String ruta = request.getRequestURI();

        long espera = limiter.tryAcquire("general:" + ip, generalPorMinuto, MINUTO);
        if (espera == 0) {
            for (Regla regla : REGLAS) {
                if (regla.aplica(metodo, ruta)) {
                    espera = limiter.tryAcquire(regla.nombre() + ":" + ip, regla.maximo(), regla.ventana());
                    break;
                }
            }
        }

        if (espera > 0) {
            rechazar(response, espera);
            return;
        }
        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response, long espera) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(espera));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":429,\"message\":\"Demasiadas peticiones. Inténtalo de nuevo en unos segundos.\"}");
    }
}
