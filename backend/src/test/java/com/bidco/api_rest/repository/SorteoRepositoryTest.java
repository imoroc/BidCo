package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.PujaSorteo;
import com.bidco.api_rest.model.Sorteo;
import com.bidco.api_rest.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consultas de {@link SorteoRepository} contra un PostgreSQL real (filtro con {@code LOWER(...) IN},
 * {@code ORDER BY CASE} y subconsultas {@code EXISTS}). Necesita Docker.
 */
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SorteoRepositoryTest {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedadesDeLaBase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);
    private static final Instant AHORA = Instant.parse("2026-11-10T10:00:00Z");

    @Autowired
    private SorteoRepository sorteos;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private PujaSorteoRepository participaciones;

    private Usuario participante;

    @BeforeEach
    void preparar() {
        participante = usuarios.save(new Usuario(null, "ana", "ana@example.com", "x".repeat(60),
                new BigDecimal("100"), 500, "España", "Madrid", "28001", "Calle Mayor", 1, null));
    }

    private Sorteo sorteo(String nombre, String categoria, int puntos, Instant fin) {
        return sorteos.save(new Sorteo(null, nombre, "Descripción", HOY.minusDays(10), fin, puntos,
                null, "img", categoria));
    }

    private void participar(Sorteo sorteo) {
        participaciones.save(new PujaSorteo(null, sorteo, new BigDecimal("50"), HOY, participante));
    }

    private List<String> nombres(List<Sorteo> resultado) {
        return resultado.stream().map(Sorteo::getNombreArticulo).toList();
    }

    @Test
    void elFiltroRespetaElRangoDePuntosYLaCategoria() {
        sorteo("barato", "tecnologia", 10, AHORA.plusSeconds(3600));
        sorteo("medio", "tecnologia", 100, AHORA.plusSeconds(3600));
        sorteo("otra", "moda", 100, AHORA.plusSeconds(3600));

        List<Sorteo> resultado = sorteos.findByFiltro(50, 200, new String[]{"tecnologia"}, false, "asc",
                Pageable.unpaged());

        assertEquals(List.of("medio"), nombres(resultado));
    }

    @Test
    void sinCategoriasConcretasDevuelveTodas() {
        sorteo("uno", "tecnologia", 10, AHORA.plusSeconds(3600));
        sorteo("dos", "moda", 10, AHORA.plusSeconds(7200));

        List<Sorteo> resultado = sorteos.findByFiltro(0, 1000, new String[]{""}, true, "asc",
                Pageable.unpaged());

        assertEquals(2, resultado.size());
    }

    @Test
    void ordenaPorFechaDeFinSegunElParametro() {
        sorteo("pronto", "moda", 10, AHORA.plusSeconds(3600));
        sorteo("tarde", "moda", 10, AHORA.plusSeconds(7200));

        assertEquals(List.of("pronto", "tarde"), nombres(sorteos.findByFiltro(0, 1000, new String[]{""}, true,
                "asc", Pageable.unpaged())));
        assertEquals(List.of("tarde", "pronto"), nombres(sorteos.findByFiltro(0, 1000, new String[]{""}, true,
                "desc", Pageable.unpaged())));
    }

    @Test
    void paginaLosResultadosFiltrados() {
        for (int i = 1; i <= 5; i++) {
            sorteo("sorteo" + i, "moda", 10, AHORA.plusSeconds(3600L * i));
        }

        List<Sorteo> segunda = sorteos.findByFiltro(0, 1000, new String[]{""}, true, "asc", PageRequest.of(1, 2));

        assertEquals(List.of("sorteo3", "sorteo4"), nombres(segunda));
    }

    @Test
    void losVencidosPendientesSoloIncluyenLosQueTienenParticipacionesYNoTienenGanador() {
        Sorteo conParticipacion = sorteo("con", "moda", 10, AHORA.minusSeconds(60));
        participar(conParticipacion);
        sorteo("sinParticipaciones", "moda", 10, AHORA.minusSeconds(60));
        Sorteo yaCerrado = sorteo("cerrado", "moda", 10, AHORA.minusSeconds(60));
        participar(yaCerrado);
        yaCerrado.setGanador(participante.getUsuarioID());
        sorteos.save(yaCerrado);
        participar(sorteo("abierto", "moda", 10, AHORA.plusSeconds(60)));

        List<Long> ids = sorteos.findIdsVencidosConParticipacionesSinGanador(AHORA);

        assertEquals(List.of(conParticipacion.getSorteoID()), ids);
    }

    @Test
    void cuentaLosSorteosGanadosPorUnUsuario() {
        Sorteo ganado = sorteo("ganado", "moda", 10, AHORA.minusSeconds(60));
        ganado.setGanador(participante.getUsuarioID());
        sorteos.save(ganado);
        sorteo("otro", "moda", 10, AHORA.minusSeconds(60));

        assertEquals(1, sorteos.countWonDrawsByUsuarioId(participante.getUsuarioID()));
    }

    @Test
    void cuentaLasParticipacionesPendientesSinDuplicarPorVariasParticipaciones() {
        Sorteo abierto = sorteo("abierto", "moda", 10, AHORA.plusSeconds(60));
        participar(abierto);
        participar(abierto);
        Sorteo cerrado = sorteo("cerrado", "moda", 10, AHORA.minusSeconds(60));
        participar(cerrado);
        cerrado.setGanador(participante.getUsuarioID());
        sorteos.save(cerrado);

        assertEquals(1, sorteos.countPendientesConParticipacionDe(participante.getUsuarioID()));
    }

    @Test
    void bloqueaYDevuelveUnSorteoPorId() {
        Sorteo guardado = sorteo("uno", "moda", 10, AHORA.plusSeconds(60));

        assertTrue(sorteos.findByIdForUpdate(guardado.getSorteoID()).isPresent());
        assertTrue(sorteos.findByIdForUpdate(-1L).isEmpty());
    }
}
