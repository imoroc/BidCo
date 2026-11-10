package com.bidco.api_rest.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas; se puede desactivar con {@code CIERRE_ACTIVO=false}. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.cierre.activo", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
