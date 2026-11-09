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
 *
 * <p>Modelo de fechas:
 * <ul>
 *   <li>Las entidades usan {@code LocalDate}: un día natural, sin hora ni zona.</li>
 *   <li>{@code database/schema_postgres.sql} declara esas columnas como {@code TIMESTAMP}.
 *       Con un {@code LocalDate} se guardan a las 00:00 y al leerlas se descarta la hora.
 *       Si la base la crea Hibernate ({@code DDL_AUTO=update}) serán {@code DATE}; con
 *       {@code update} las columnas ya existentes no se modifican, así que el tipo real
 *       depende de cómo se creó la base.</li>
 *   <li>Una subasta o sorteo está activo desde el día de inicio hasta el día final,
 *       ambos incluidos. Se puede pujar hasta el final de ese día y solo se puede
 *       cerrar a partir del día siguiente ({@code hoy > fechaFinal}).</li>
 *   <li>"Hoy" se calcula siempre con este reloj (propiedad {@code app.timezone}, por defecto
 *       {@code Europe/Madrid}). La fecha que envía el cliente en una puja se ignora: los
 *       servicios de pujas la sustituyen por la del servidor.</li>
 *   <li>El frontend aplica el mismo criterio en {@code src/app/utils/fechas.ts}: el contador
 *       llega a cero a la medianoche de Madrid, con unos segundos de margen. Si se cambia
 *       {@code app.timezone}, hay que cambiar también {@code ZONA_HORARIA} allí.</li>
 * </ul>
 */
@Configuration
public class AppClockConfig {

    @Bean
    public Clock appClock(@Value("${app.timezone:Europe/Madrid}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
