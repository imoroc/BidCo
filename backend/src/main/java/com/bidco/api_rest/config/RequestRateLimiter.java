package com.bidco.api_rest.config;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador de peticiones por clave en ventanas fijas. Lo usa {@link RateLimitFilter} para frenar
 * el abuso de los endpoints de escritura y el rastreo masivo de los listados públicos.
 *
 * <p>Limitaciones, las mismas que {@link LoginRateLimiter}: el estado vive en memoria, así que cada
 * instancia cuenta por separado y se reinicia al redesplegar. Si se escala a más de una instancia,
 * habría que mover el contador a un almacén compartido. Si se llenara la tabla de claves, las
 * claves nuevas no se contabilizan (se prefiere no bloquear a usuarios legítimos antes que
 * agotar la memoria).
 */
@Component
public class RequestRateLimiter {

    private static final int MAX_TRACKED_KEYS = 50_000;

    private record Ventana(int peticiones, Instant fin) {}

    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();
    private final Clock clock;

    public RequestRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /**
     * Registra una petición para la clave dada.
     *
     * @return 0 si se permite; si no, los segundos que faltan para que se libere el cupo.
     */
    public long tryAcquire(String key, int maximo, Duration ventana) {
        Instant ahora = clock.instant();
        if (ventanas.size() > MAX_TRACKED_KEYS) {
            ventanas.entrySet().removeIf(entrada -> !ahora.isBefore(entrada.getValue().fin()));
            if (ventanas.size() > MAX_TRACKED_KEYS && !ventanas.containsKey(key)) {
                return 0;
            }
        }
        long[] espera = {0};
        ventanas.compute(key, (clave, actual) -> {
            if (actual == null || !ahora.isBefore(actual.fin())) {
                return new Ventana(1, ahora.plus(ventana));
            }
            if (actual.peticiones() >= maximo) {
                long millis = Duration.between(ahora, actual.fin()).toMillis();
                espera[0] = Math.max(1, (millis + 999) / 1000);
                return actual;
            }
            return new Ventana(actual.peticiones() + 1, actual.fin());
        });
        return espera[0];
    }
}
