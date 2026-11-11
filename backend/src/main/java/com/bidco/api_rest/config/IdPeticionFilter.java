package com.bidco.api_rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Da a cada petición un identificador que aparece en todos sus registros (clave {@code idPeticion} del
 * MDC) y en la cabecera de respuesta {@code X-Request-Id}, de modo que un error visto por una persona
 * se pueda localizar en los logs. Si el cliente o el proxy ya envían un identificador razonable se
 * conserva; si no, se genera uno. Un valor con caracteres raros se descarta para evitar inyectar
 * saltos de línea en los registros.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class IdPeticionFilter extends OncePerRequestFilter {

    public static final String CABECERA = "X-Request-Id";
    public static final String CLAVE_MDC = "idPeticion";
    private static final Pattern VALIDO = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String recibido = request.getHeader(CABECERA);
        String id = recibido != null && VALIDO.matcher(recibido).matches()
                ? recibido
                : UUID.randomUUID().toString();
        MDC.put(CLAVE_MDC, id);
        response.setHeader(CABECERA, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CLAVE_MDC);
        }
    }
}
