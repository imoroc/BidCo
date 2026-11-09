package com.bidco.api_rest.service;

import com.bidco.api_rest.config.JwtUtil;
import com.bidco.api_rest.dto.trabajador.LoginTrabajadorDTO;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.mapper.TrabajadorMapper;
import com.bidco.api_rest.model.Trabajador;
import com.bidco.api_rest.repository.TrabajadorRepository;
import com.bidco.api_rest.service.impl.TrabajadorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TrabajadorServiceImplTest {

    private TrabajadorRepository trabajadorRepository;
    private JwtUtil jwtUtil;
    private TrabajadorServiceImpl service;
    private String clave;

    @BeforeEach
    void setUp() {
        trabajadorRepository = mock(TrabajadorRepository.class);
        jwtUtil = mock(JwtUtil.class);
        service = new TrabajadorServiceImpl(trabajadorRepository, mock(TrabajadorMapper.class), jwtUtil);
        clave = "p".repeat(12);
    }

    private Trabajador trabajador(boolean experto) {
        Trabajador t = new Trabajador();
        t.setTrabajadorID(7L);
        t.setExperto(experto);
        t.setContraseña(new BCryptPasswordEncoder().encode(clave));
        return t;
    }

    private LoginTrabajadorDTO login(String contraseña, boolean rol) {
        LoginTrabajadorDTO dto = new LoginTrabajadorDTO();
        dto.setIdentificador("ana");
        dto.setContraseña(contraseña);
        dto.setRol(rol);
        return dto;
    }

    @Test
    void loginCorrectoDevuelveToken() {
        when(trabajadorRepository.findByNombreUsuario("ana")).thenReturn(Optional.of(trabajador(true)));
        when(jwtUtil.generateToken("7", "EXPERT")).thenReturn("token");

        assertEquals("token", service.login(login(clave, true)));
    }

    @Test
    void loginConCuentaInexistenteFallaConElMismoMensaje() {
        when(trabajadorRepository.findByNombreUsuario(anyString())).thenReturn(Optional.empty());
        when(trabajadorRepository.findByCorreoElectronico(anyString())).thenReturn(Optional.empty());

        AuthenticationFailedException e = assertThrows(AuthenticationFailedException.class,
                () -> service.login(login(clave, false)));
        assertEquals("Credenciales incorrectas", e.getMessage());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void loginConContraseñaIncorrectaFalla() {
        when(trabajadorRepository.findByNombreUsuario("ana")).thenReturn(Optional.of(trabajador(false)));

        assertThrows(AuthenticationFailedException.class, () -> service.login(login("q".repeat(12), false)));
    }

    @Test
    void loginConRolDistintoAlDeLaCuentaFalla() {
        when(trabajadorRepository.findByNombreUsuario("ana")).thenReturn(Optional.of(trabajador(false)));

        assertThrows(AuthenticationFailedException.class, () -> service.login(login(clave, true)));
    }
}
