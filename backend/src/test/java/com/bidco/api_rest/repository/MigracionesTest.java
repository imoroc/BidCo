package com.bidco.api_rest.repository;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    @Test
    void creaLosIndicesDeLasConsultasHabituales() {
        Number indices = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'idx\\_%'")
                .getSingleResult();
        assertEquals(11, indices.intValue());
    }

    @Test
    void creaLasRestriccionesDeIntegridad() {
        Number restricciones = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname LIKE 'ck\\_%'")
                .getSingleResult();
        assertEquals(5, restricciones.intValue());
    }

    @Test
    void creaLasTablasDeSesionesDeSpringSession() {
        Number tablas = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name IN ('spring_session', 'spring_session_attributes')")
                .getSingleResult();
        assertEquals(2, tablas.intValue());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void laSesionDeAdministracionSePersisteYSeRecuperaConElEsquemaMigrado() {
        SessionRepository<Session> sesiones = (SessionRepository) new JdbcIndexedSessionRepository(
                jdbc, new TransactionTemplate(transacciones));

        Session creada = sesiones.createSession();
        creada.setAttribute("adminId", 7L);
        sesiones.save(creada);

        Session leida = sesiones.findById(creada.getId());
        assertNotNull(leida);
        assertEquals(7L, (Long) leida.getAttribute("adminId"));

        sesiones.deleteById(creada.getId());
        assertNull(sesiones.findById(creada.getId()));
    }
}
