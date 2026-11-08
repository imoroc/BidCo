package com.bidco.api_rest.service;

import com.bidco.api_rest.config.JwtUtil;
import com.bidco.api_rest.dto.usuario.UsuarioCreateDTO;
import com.bidco.api_rest.mapper.UsuarioMapper;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.PujaSorteoRepository;
import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceImplTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        service = new UsuarioServiceImpl(
                usuarioRepository,
                mock(UsuarioMapper.class),
                mock(PujaRepository.class),
                mock(SubastaRepository.class),
                mock(PujaSorteoRepository.class),
                mock(SorteoRepository.class),
                mock(JwtUtil.class));
    }

    private UsuarioCreateDTO nuevoUsuario() {
        UsuarioCreateDTO dto = new UsuarioCreateDTO();
        dto.setNombreUsuario("ana");
        dto.setCorreoElectronico("ana@example.com");
        dto.setContraseña("una-contraseña-larga");
        return dto;
    }

    @Test
    void registrarConNombreDeUsuarioExistenteDevuelveConflicto() {
        when(usuarioRepository.findByNombreUsuario("ana")).thenReturn(Optional.of(new Usuario()));

        assertThrows(IllegalStateException.class, () -> service.registrarUsuario(nuevoUsuario()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarConCorreoExistenteDevuelveConflicto() {
        when(usuarioRepository.findByNombreUsuario("ana")).thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreoElectronico("ana@example.com")).thenReturn(Optional.of(new Usuario()));

        assertThrows(IllegalStateException.class, () -> service.registrarUsuario(nuevoUsuario()));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void recargarSaldoBloqueaLaFilaYSumaLaCantidad() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioID(1L);
        usuario.setSaldo(new BigDecimal("20"));
        when(usuarioRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(usuario));

        BigDecimal nuevoSaldo = service.recargarSaldo(1L, new BigDecimal("30"));

        assertEquals(0, new BigDecimal("50").compareTo(nuevoSaldo));
        verify(usuarioRepository).findByIdForUpdate(1L);
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void recargarSaldoRechazaCantidadesNoPositivas() {
        assertThrows(IllegalArgumentException.class, () -> service.recargarSaldo(1L, BigDecimal.ZERO));
        verifyNoInteractions(usuarioRepository);
    }
}
