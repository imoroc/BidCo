package com.bidco.api_rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    /** Orden 1: después de CORS (0) y antes del filtro JWT (2). */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RequestRateLimiter limiter,
            @Value("${app.rate-limit.enabled:true}") boolean activo,
            @Value("${app.rate-limit.general-per-minute:300}") int generalPorMinuto) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(new RateLimitFilter(limiter, activo, generalPorMinuto));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
