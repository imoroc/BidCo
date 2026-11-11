package com.bidco.api_rest.config;

import com.bidco.api_rest.exception.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    private RelojManual reloj;
    private LoginRateLimiter limitador;

    @BeforeEach
    void preparar() {
        reloj = new RelojManual();
        limitador = new LoginRateLimiter(reloj);
    }

    private void fallar(String clave, int veces) {
        for (int i = 0; i < veces; i++) {
            limitador.recordFailure(clave);
        }
    }

    @Test
    void laClaveNormalizaElIdentificadorYIncluyeLaDireccion() {
        assertEquals("login:ana@example.com@10.0.0.1",
                limitador.key("login", "  Ana@Example.com ", "10.0.0.1"));
        assertEquals("login:@10.0.0.1", limitador.key("login", null, "10.0.0.1"));
    }

    @Test
    void laClaveRecortaIdentificadoresMuyLargos() {
        String clave = limitador.key("login", "a".repeat(500), "10.0.0.1");

        assertEquals("login:".length() + 100 + "@10.0.0.1".length(), clave.length());
    }

    @Test
    void permiteMenosFallosDelMaximo() {
        String clave = limitador.key("login", "ana", "ip");
        fallar(clave, LoginRateLimiter.MAX_FAILURES - 1);

        assertDoesNotThrow(() -> limitador.assertAllowed(clave));
    }

    @Test
    void bloqueaAlLlegarAlMaximoDeFallos() {
        String clave = limitador.key("login", "ana", "ip");
        fallar(clave, LoginRateLimiter.MAX_FAILURES);

        assertThrows(TooManyRequestsException.class, () -> limitador.assertAllowed(clave));
    }

    @Test
    void elBloqueoNoAfectaAOtrasCuentasNiAOtrasDirecciones() {
        String bloqueada = limitador.key("login", "ana", "ip1");
        fallar(bloqueada, LoginRateLimiter.MAX_FAILURES);

        assertDoesNotThrow(() -> limitador.assertAllowed(limitador.key("login", "luis", "ip1")));
        assertDoesNotThrow(() -> limitador.assertAllowed(limitador.key("login", "ana", "ip2")));
    }

    @Test
    void unInicioDeSesionCorrectoReiniciaLosFallos() {
        String clave = limitador.key("login", "ana", "ip");
        fallar(clave, LoginRateLimiter.MAX_FAILURES);

        limitador.reset(clave);

        assertDoesNotThrow(() -> limitador.assertAllowed(clave));
    }

    @Test
    void elBloqueoCaducaAlTerminarLaVentana() {
        String clave = limitador.key("login", "ana", "ip");
        fallar(clave, LoginRateLimiter.MAX_FAILURES);

        reloj.avanzar(LoginRateLimiter.WINDOW.plusSeconds(1));

        assertDoesNotThrow(() -> limitador.assertAllowed(clave));
    }

    @Test
    void sigueBloqueadoJustoAntesDeQueTermineLaVentana() {
        String clave = limitador.key("login", "ana", "ip");
        fallar(clave, LoginRateLimiter.MAX_FAILURES);

        reloj.avanzar(LoginRateLimiter.WINDOW);

        assertThrows(TooManyRequestsException.class, () -> limitador.assertAllowed(clave));
    }

    @Test
    void unFalloTrasCaducarLaVentanaEmpiezaLaCuentaDeNuevo() {
        String clave = limitador.key("login", "ana", "ip");
        fallar(clave, LoginRateLimiter.MAX_FAILURES - 1);
        reloj.avanzar(LoginRateLimiter.WINDOW.plusSeconds(1));

        fallar(clave, 1);

        assertDoesNotThrow(() -> limitador.assertAllowed(clave));
    }
}
