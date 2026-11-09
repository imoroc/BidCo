package com.bidco.api_rest.service;

import com.bidco.api_rest.dto.admin.AdminLoginDTO;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.model.Trabajador;
import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.TrabajadorRepository;
import com.bidco.api_rest.service.contract.CloudinaryService;
import com.bidco.api_rest.service.impl.AdminServiceImpl;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceImplTest {

    private TrabajadorRepository trabajadorRepository;
    private HttpSession session;
    private AdminServiceImpl service;
    private String clave;

    @BeforeEach
    void setUp() {
        trabajadorRepository = mock(TrabajadorRepository.class);
        session = mock(HttpSession.class);
        service = new AdminServiceImpl(trabajadorRepository, mock(SorteoRepository.class), mock(CloudinaryService.class));
        clave = "a".repeat(12);
    }

    private Trabajador trabajador(boolean experto) {
        Trabajador t = new Trabajador();
        t.setTrabajadorID(3L);
        t.setExperto(experto);
        t.setContraseña(new BCryptPasswordEncoder().encode(clave));
        return t;
    }

    private AdminLoginDTO credenciales(String contraseña, boolean rol) {
        AdminLoginDTO dto = new AdminLoginDTO();
        dto.setUsername("admin");
        dto.setPassword(contraseña);
        dto.setRol(rol);
        return dto;
    }

    @Test
    void loginCorrectoGuardaElAdministradorEnLaSesion() {
        when(trabajadorRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(trabajador(false)));

        service.login(credenciales(clave, false), session);

        verify(session).setAttribute("adminId", 3L);
    }

    @Test
    void loginConRolDistintoDelRealFalla() {
        when(trabajadorRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(trabajador(false)));

        assertThrows(AuthenticationFailedException.class, () -> service.login(credenciales(clave, true), session));
        verifyNoInteractions(session);
    }

    @Test
    void loginConContraseñaIncorrectaFallaConElMismoMensaje() {
        when(trabajadorRepository.findByNombreUsuario("admin")).thenReturn(Optional.of(trabajador(true)));

        AuthenticationFailedException e = assertThrows(AuthenticationFailedException.class,
                () -> service.login(credenciales("b".repeat(12), true), session));
        assertEquals("Credenciales incorrectas", e.getMessage());
        verifyNoInteractions(session);
    }

    @Test
    void loginConCuentaInexistenteFallaConElMismoMensaje() {
        when(trabajadorRepository.findByNombreUsuario("admin")).thenReturn(Optional.empty());

        AuthenticationFailedException e = assertThrows(AuthenticationFailedException.class,
                () -> service.login(credenciales(clave, false), session));
        assertEquals("Credenciales incorrectas", e.getMessage());
        verifyNoInteractions(session);
    }

    @Test
    void loginSinDatosFalla() {
        assertThrows(AuthenticationFailedException.class, () -> service.login(null, session));
        assertThrows(AuthenticationFailedException.class, () -> service.login(credenciales(null, false), session));
    }
}
