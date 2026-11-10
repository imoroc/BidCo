package com.bidco.api_rest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS se resuelve en un filtro y no en Spring MVC: así lo aplican también las respuestas que
 * escriben los demás filtros (401, 403, 429). Si no, el navegador las vería como un error de red
 * y el frontend no podría leer el estado.
 */
@Configuration
public class CorsConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    /** Patrones adicionales (p. ej. los previews de Vercel, cuyo dominio cambia en cada despliegue). */
    @Value("${app.cors.allowed-origin-patterns:}")
    private String[] allowedOriginPatterns;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedOriginPatterns(Arrays.asList(allowedOriginPatterns));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", JwtFilter.ADMIN_CSRF_HEADER));
        // El navegador solo deja leer entre orígenes las cabeceras expuestas: Date permite ajustar la cuenta atrás.
        config.setExposedHeaders(List.of("Date"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.addUrlPatterns("/*");
        registration.setOrder(0);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<JwtFilter> jwtFilterRegistration() {
        FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(2);
        return registration;
    }
}
