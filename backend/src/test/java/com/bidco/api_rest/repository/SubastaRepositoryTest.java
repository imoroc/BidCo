package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Consultas de {@link SubastaRepository} contra un PostgreSQL real: usan {@code LOWER(...) IN},
 * {@code CASE WHEN} sobre el precio y {@code ORDER BY CASE} con parámetros, que una base en
 * memoria no reproduce fielmente. Necesita Docker.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubastaRepositoryTest {

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

    private static final String[] SIN_CATEGORIAS = {""};
    private static final LocalDate HOY = LocalDate.of(2026, 11, 10);

    @Autowired
    private SubastaRepository subastas;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private PujaRepository pujas;

    private Usuario creador;

    @BeforeEach
    void preparar() {
        creador = usuarios.save(new Usuario(null, "vendedor", "vendedor@example.com", "x".repeat(60),
                new BigDecimal("100"), 0, "España", "Madrid", "28001", "Calle Mayor", 1, null));
    }

    private Subasta subasta(String nombre, boolean normal, String categoria, String precio, LocalDate fin) {
        return subastas.save(new Subasta(null, HOY.minusDays(10), fin, new BigDecimal(precio), normal,
                nombre, "Descripción", creador, null, categoria));
    }

    private List<String> nombres(List<Subasta> resultado) {
        return resultado.stream().map(Subasta::getNombreArticulo).toList();
    }

    @Test
    void filtraLasNormalesPorCategoriaIgnorandoMayusculasYExcluyeLasCiegas() {
        subasta("portatil", true, "tecnologia", "50", HOY.plusDays(5));
        subasta("chaqueta", true, "moda", "60", HOY.plusDays(5));
        subasta("movil ciego", false, "tecnologia", "70", HOY.plusDays(5));

        List<Subasta> resultado = subastas.findByFiltroNormal(
                0, 1000, new String[]{"tecnologia"}, false, "asc");

        assertEquals(List.of("portatil"), nombres(resultado));
    }

    @Test
    void filtraPorRangoDePrecioUsandoElPrecioFinalSiLoHay() {
        subasta("barata", true, "moda", "20", HOY.plusDays(5));
        Subasta cara = subasta("cara", true, "moda", "20", HOY.plusDays(5));
        cara.setPrecioFinal(new BigDecimal("500"));
        subastas.save(cara);

        List<Subasta> resultado = subastas.findByFiltroNormal(
                0, 100, SIN_CATEGORIAS, true, "asc");

        assertEquals(List.of("barata"), nombres(resultado));
    }

    @Test
    void ordenaPorFechaFinalAscendenteODescendente() {
        subasta("tarde", true, "moda", "10", HOY.plusDays(30));
        subasta("pronto", true, "moda", "10", HOY.plusDays(2));

        assertEquals(List.of("pronto", "tarde"),
                nombres(subastas.findByFiltroNormal(0, 1000, SIN_CATEGORIAS, true, "asc")));
        assertEquals(List.of("tarde", "pronto"),
                nombres(subastas.findByFiltroNormal(0, 1000, SIN_CATEGORIAS, true, "desc")));
    }

    @Test
    void buscaPorNombreSinDistinguirMayusculas() {
        subasta("Teléfono Antiguo", true, "tecnologia", "30", HOY.plusDays(5));
        subasta("Lámpara", true, "hogar", "30", HOY.plusDays(5));

        List<Subasta> resultado = subastas.findByFiltroNombre(
                "TELÉF", 0, 1000, SIN_CATEGORIAS, true, "asc");

        assertEquals(List.of("Teléfono Antiguo"), nombres(resultado));
    }

    @Test
    void lasMisSubastasSoloDevuelvenLasDelCreador() {
        subasta("mia", true, "moda", "10", HOY.plusDays(5));
        Usuario otro = usuarios.save(new Usuario(null, "otro", "otro@example.com", "y".repeat(60),
                new BigDecimal("0"), 0, "España", "Madrid", "28002", "Calle Sol", 2, null));
        subastas.save(new Subasta(null, HOY, HOY.plusDays(5), new BigDecimal("10"), true,
                "ajena", "Descripción", otro, null, "moda"));

        List<Subasta> resultado = subastas.findByFiltroMisSubastas(
                creador.getUsuarioID().intValue(), 0, 1000, SIN_CATEGORIAS, true, "asc");

        assertEquals(List.of("mia"), nombres(resultado));
    }

    @Test
    void lasVencidasPendientesSonLasQueTienenPujasYNoTienenGanador() {
        Subasta conPujas = subasta("vencida con pujas", true, "moda", "10", HOY.minusDays(1));
        subasta("vencida sin pujas", true, "moda", "10", HOY.minusDays(1));
        Subasta cerrada = subasta("vencida cerrada", true, "moda", "10", HOY.minusDays(2));
        Subasta vigente = subasta("vigente con pujas", true, "moda", "10", HOY.plusDays(3));
        cerrada.setGanador(creador.getUsuarioID());
        subastas.save(cerrada);
        for (Subasta s : List.of(conPujas, cerrada, vigente)) {
            pujas.save(new Puja(null, s, new BigDecimal("15"), HOY.minusDays(3), creador));
        }

        List<Long> ids = subastas.findIdsVencidasConPujasSinGanador(HOY);

        assertEquals(List.of(conPujas.getSubastaID()), ids);
    }

    @Test
    void elBloqueoDeEscrituraDevuelveLaSubasta() {
        Subasta guardada = subasta("bloqueable", true, "moda", "10", HOY.plusDays(5));

        assertTrue(subastas.findByIdForUpdate(guardada.getSubastaID()).isPresent());
        assertTrue(subastas.findByIdForUpdate(-1L).isEmpty());
    }
}
