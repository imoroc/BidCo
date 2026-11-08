package com.bidco.api_rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj único de la aplicación. Las fechas de subastas y sorteos se guardan como
 * LocalDate, así que el "día" debe calcularse siempre en la misma zona horaria,
 * independientemente de la del servidor donde se despliegue.
 */
@Configuration
public class AppClockConfig {

    @Bean
    public Clock appClock(@Value("${app.timezone:Europe/Madrid}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
