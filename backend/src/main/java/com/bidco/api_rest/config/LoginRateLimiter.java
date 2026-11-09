package com.bidco.api_rest.config;

import com.bidco.api_rest.exception.TooManyRequestsException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita los intentos fallidos de inicio de sesión, de cambio de contraseña y de
 * recuperación de contraseña por cuenta e IP para frenar ataques de fuerza bruta.
 *
 * <p>Limitaciones conocidas, que conviene tener presentes al desplegar:
 * <ul>
 *   <li>El estado vive en memoria. Con varias instancias de la API cada una cuenta
 *       por separado, así que el límite efectivo sería de {@link #MAX_FAILURES} por instancia.</li>
 *   <li>Se pierde al reiniciar o redesplegar la aplicación, lo que reinicia los contadores.</li>
 *   <li>La clave incluye la dirección remota. La aplicación activa
 *       {@code server.forward-headers-strategy=framework} para recibir la IP real tras el
 *       proxy de la plataforma; si se despliega en otro entorno hay que mantenerlo, porque
 *       si no todos los clientes compartirían la misma clave.</li>
 * </ul>
 * Si se escala a más de una instancia, habría que mover el contador a un almacén
 * compartido (por ejemplo Redis o la propia base de datos).
 */
@Component
public class LoginRateLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Attempts(int failures, Instant windowStart) {}

    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public String key(String scope, String identifier, String remoteAddress) {
        String id = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        if (id.length() > 100) {
            id = id.substring(0, 100);
        }
        return scope + ":" + id + "@" + remoteAddress;
    }

    public void assertAllowed(String key) {
        Attempts current = attempts.get(key);
        if (current == null) {
            return;
        }
        if (isExpired(current)) {
            attempts.remove(key, current);
            return;
        }
        if (current.failures() >= MAX_FAILURES) {
            throw new TooManyRequestsException("Demasiados intentos fallidos. Inténtalo de nuevo en unos minutos.");
        }
    }

    public void recordFailure(String key) {
        if (attempts.size() > MAX_TRACKED_KEYS) {
            attempts.entrySet().removeIf(entry -> isExpired(entry.getValue()));
        }
        Instant now = clock.instant();
        attempts.merge(key, new Attempts(1, now), (old, ignored) ->
                isExpired(old) ? new Attempts(1, now) : new Attempts(old.failures() + 1, old.windowStart()));
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(Attempts attempts) {
        return clock.instant().isAfter(attempts.windowStart().plus(WINDOW));
    }
}
