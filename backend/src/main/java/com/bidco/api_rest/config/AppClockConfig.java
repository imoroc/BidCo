package com.bidco.api_rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj único de la aplicación. Todo lo que depende de "ahora" o de "hoy" debe usar este reloj,
 * para que el resultado no dependa de la zona horaria del servidor donde se despliegue.
 *
 * <p>Modelo de fechas:
 * <ul>
 *   <li>El cierre de una subasta o sorteo ({@code fechaFinal} / {@code fechaFin}) es un
 *       {@link java.time.Instant} (columna {@code timestamptz}): un momento exacto.</li>
 *   <li>Se puede participar mientras {@code ahora < cierre}; se puede cerrar a partir de
 *       {@code ahora >= cierre}.</li>
 *   <li>Las fechas de inicio ({@code fechaInicial} / {@code fechaInicio}) y la de cada puja siguen
 *       siendo {@code LocalDate}: un día natural calculado con este reloj.</li>
 *   <li>Al crear, el cliente puede enviar {@code YYYY-MM-DD} (el cierre es la medianoche siguiente
 *       en la zona de este reloj) o un instante ISO-8601 con zona. Ver
 *       {@link com.bidco.api_rest.util.FechasCierre}.</li>
 *   <li>La zona se define con la propiedad {@code app.timezone} (por defecto {@code Europe/Madrid}).
 *       El frontend usa la misma en {@code src/app/utils/fechas.ts} ({@code ZONA_HORARIA}) para
 *       interpretar las fechas sin hora; si se cambia aquí, hay que cambiarla allí.</li>
 * </ul>
 */
@Configuration
public class AppClockConfig {

    @Bean
    public Clock appClock(@Value("${app.timezone:Europe/Madrid}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
