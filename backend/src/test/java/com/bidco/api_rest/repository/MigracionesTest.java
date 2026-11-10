package com.bidco.api_rest.repository;

import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Aplica las migraciones de Flyway sobre un PostgreSQL vacío y arranca JPA con
 * {@code ddl-auto=validate}: si el esquema versionado no coincide con las entidades, el contexto
 * no arranca. Necesita Docker.
 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MigracionesTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void propiedadesDeLaBase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private EntityManager em;

    @Test
    void creaLosIndicesDeLasConsultasHabituales() {
        Number indices = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'idx\\_%'")
                .getSingleResult();
        assertEquals(10, indices.intValue());
    }

    @Test
    void creaLasRestriccionesDeIntegridad() {
        Number restricciones = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname LIKE 'ck\\_%'")
                .getSingleResult();
        assertEquals(5, restricciones.intValue());
    }
}
