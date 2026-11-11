package com.bidco.api_rest.service;

import com.bidco.api_rest.config.JwtUtil;
import com.bidco.api_rest.dto.usuario.ExportacionDatosDTO;
import com.bidco.api_rest.dto.usuario.UsuarioCreateDTO;
import com.bidco.api_rest.exception.ContrasenaActualIncorrectaException;
import com.bidco.api_rest.mapper.UsuarioMapper;
import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.PujaSorteo;
import com.bidco.api_rest.model.Sorteo;
import com.bidco.api_rest.model.Subasta;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.PujaSorteoRepository;
import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.TokenRecuperacionRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.service.contract.VerificacionCorreoService;
import com.bidco.api_rest.dto.usuario.UsuarioUpdateDTO;
import com.bidco.api_rest.service.impl.UsuarioServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceImplTest {

    private UsuarioRepository usuarioRepository;
    private PujaRepository pujaRepository;
    private PujaSorteoRepository pujaSorteoRepository;
    private SubastaRepository subastaRepository;
    private SorteoRepository sorteoRepository;
    private TokenRecuperacionRepository tokenRecuperacionRepository;
    private UsuarioMapper usuarioMapper;
    private VerificacionCorreoService verificacionCorreoService;
    private JwtUtil jwtUtil;
    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        pujaRepository = mock(PujaRepository.class);
        pujaSorteoRepository = mock(PujaSorteoRepository.class);
        subastaRepository = mock(SubastaRepository.class);
        sorteoRepository = mock(SorteoRepository.class);
        tokenRecuperacionRepository = mock(TokenRecuperacionRepository.class);
        usuarioMapper = mock(UsuarioMapper.class);
        verificacionCorreoService = mock(VerificacionCorreoService.class);
        jwtUtil = mock(JwtUtil.class);
        service = new UsuarioServiceImpl(
                usuarioRepository,
                usuarioMapper,
                pujaRepository,
                subastaRepository,
                pujaSorteoRepository,
                sorteoRepository,
                jwtUtil,
                tokenRecuperacionRepository,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC),
                verificacionCorreoService);
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

    private Usuario usuarioConClave(String clave) {
        Usuario usuario = new Usuario();
        usuario.setUsuarioID(7L);
        usuario.setNombreUsuario("ana");
        usuario.setCorreoElectronico("ana@example.com");
        usuario.setContrasena(new BCryptPasswordEncoder().encode(clave));
        usuario.setSaldo(new BigDecimal("40"));
        usuario.setPuntos(120);
        usuario.setPais("España");
        usuario.setCiudad("Madrid");
        usuario.setCodigoPostal("28001");
        usuario.setCalle("Calle Mayor");
        usuario.setNumeroPiso(3);
        usuario.setLetraPiso("B");
        return usuario;
    }

    @Test
    void eliminarCuentaConContrasenaIncorrectaNoModificaNada() {
        String clave = "k".repeat(12);
        when(usuarioRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(usuarioConClave(clave)));

        assertThrows(ContrasenaActualIncorrectaException.class,
                () -> service.eliminarCuenta(7L, "z".repeat(12)));

        verify(usuarioRepository, never()).save(any());
        verify(tokenRecuperacionRepository, never()).eliminarPorUsuario(any());
    }

    @Test
    void eliminarCuentaConSubastasPendientesSeRechaza() {
        String clave = "k".repeat(12);
        when(usuarioRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(usuarioConClave(clave)));
        when(subastaRepository.countPendientesPorCreador(eq(7L), any())).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.eliminarCuenta(7L, clave));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void eliminarCuentaConPujasPendientesSeRechaza() {
        String clave = "k".repeat(12);
        when(usuarioRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(usuarioConClave(clave)));
        when(subastaRepository.countPendientesConPujasDe(7L)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.eliminarCuenta(7L, clave));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void eliminarCuentaConSorteosPendientesSeRechaza() {
        String clave = "k".repeat(12);
        when(usuarioRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(usuarioConClave(clave)));
        when(sorteoRepository.countPendientesConParticipacionDe(7L)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.eliminarCuenta(7L, clave));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void eliminarCuentaAnonimizaLosDatosPersonalesYBorraLosTokens() {
        String clave = "k".repeat(12);
        Usuario usuario = usuarioConClave(clave);
        String hashAnterior = usuario.getContrasena();
        when(usuarioRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(usuario));

        service.eliminarCuenta(7L, clave);

        assertEquals("usuario-eliminado-7", usuario.getNombreUsuario());
        assertEquals("eliminado-7@anonimo.invalid", usuario.getCorreoElectronico());
        assertNotEquals(hashAnterior, usuario.getContrasena());
        assertFalse(new BCryptPasswordEncoder().matches(clave, usuario.getContrasena()));
        assertEquals(0, BigDecimal.ZERO.compareTo(usuario.getSaldo()));
        assertEquals(0, usuario.getPuntos());
        assertEquals("-", usuario.getCalle());
        assertNull(usuario.getLetraPiso());
        assertTrue(usuario.getPrivacidadAnonimoPujas());
        assertFalse(usuario.getPrivacidadPerfilVisible());
        assertEquals(1, usuario.getTokenVersion());
        verify(usuarioRepository).save(usuario);
        verify(tokenRecuperacionRepository).eliminarPorUsuario(7L);
    }

    @Test
    void exportarDatosDeUnUsuarioInexistenteFalla() {
        when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> service.exportarDatos(9L));
    }

    @Test
    void exportarDatosIncluyeElPerfilYLaActividadDelUsuario() {
        Usuario usuario = usuarioConClave("k".repeat(12));
        Instant fin = Instant.parse("2026-02-01T10:00:00Z");
        Subasta subasta = new Subasta(3L, LocalDate.of(2026, 1, 1), fin, new BigDecimal("10"), true,
                "Reloj", "Reloj antiguo", usuario, "img", "moda");
        Sorteo sorteo = new Sorteo(4L, "Cámara", "Cámara digital", LocalDate.of(2026, 1, 1), fin, 50,
                null, "img", "tecnologia");
        Puja puja = new Puja(11L, subasta, new BigDecimal("25"), LocalDate.of(2026, 1, 5), usuario);
        PujaSorteo participacion = new PujaSorteo(12L, sorteo, new BigDecimal("30"), LocalDate.of(2026, 1, 6), usuario);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(pujaRepository.findByUsuarioIdOrdenadas(7L)).thenReturn(List.of(puja));
        when(pujaSorteoRepository.findByUsuarioIdOrdenadas(7L)).thenReturn(List.of(participacion));
        when(subastaRepository.findByCreadorId(7L)).thenReturn(List.of(subasta));

        ExportacionDatosDTO datos = service.exportarDatos(7L);

        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), datos.generadoEn());
        assertEquals("ana", datos.perfil().nombreUsuario());
        assertEquals("ana@example.com", datos.perfil().correoElectronico());
        assertEquals(1, datos.pujas().size());
        assertEquals("Reloj", datos.pujas().get(0).articulo());
        assertEquals(3L, datos.pujas().get(0).subastaId());
        assertEquals(1, datos.participacionesEnSorteos().size());
        assertEquals("Cámara", datos.participacionesEnSorteos().get(0).articulo());
        assertEquals(1, datos.subastasCreadas().size());
        assertEquals(fin, datos.subastasCreadas().get(0).fechaFinal());
    }

    @Test
    void exportarDatosNoIncluyeLaContrasena() {
        Usuario usuario = usuarioConClave("k".repeat(12));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        ExportacionDatosDTO datos = service.exportarDatos(7L);

        assertFalse(datos.toString().contains(usuario.getContrasena()));
    }

    @Test
    void registrarConCorreoDisponibleCreaLaCuentaSinVerificarYEnviaElEnlace() {
        Usuario nuevo = new Usuario();
        when(usuarioMapper.usuarioCreateDTOToUsuario(any())).thenReturn(nuevo);
        when(usuarioRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(verificacionCorreoService.disponible()).thenReturn(true);

        service.registrarUsuario(nuevoUsuario());

        assertFalse(nuevo.isCorreoVerificado());
        verify(verificacionCorreoService).enviar(nuevo);
    }

    @Test
    void registrarSinCorreoDisponibleCreaLaCuentaYaVerificada() {
        Usuario nuevo = new Usuario();
        when(usuarioMapper.usuarioCreateDTOToUsuario(any())).thenReturn(nuevo);
        when(usuarioRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(verificacionCorreoService.disponible()).thenReturn(false);

        service.registrarUsuario(nuevoUsuario());

        assertTrue(nuevo.isCorreoVerificado());
        verify(verificacionCorreoService, never()).enviar(any());
    }

    private UsuarioUpdateDTO actualizacionConCorreo(String correo) {
        UsuarioUpdateDTO dto = new UsuarioUpdateDTO();
        dto.setNombreUsuario("ana");
        dto.setCorreoElectronico(correo);
        dto.setCiudad("Madrid");
        dto.setCodigoPostal("28001");
        dto.setCalle("Calle Mayor");
        dto.setNumeroPiso(3);
        dto.setLetraPiso("B");
        return dto;
    }

    @Test
    void cambiarDeCorreoExigeVerificarElNuevo() {
        Usuario usuario = usuarioConClave("k".repeat(12));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(verificacionCorreoService.disponible()).thenReturn(true);

        service.actualizarUsuario(7L, actualizacionConCorreo("otra@example.com"));

        assertFalse(usuario.isCorreoVerificado());
        verify(verificacionCorreoService).enviar(usuario);
    }

    @Test
    void conservarElMismoCorreoNoPideVerificarDeNuevo() {
        Usuario usuario = usuarioConClave("k".repeat(12));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(verificacionCorreoService.disponible()).thenReturn(true);

        service.actualizarUsuario(7L, actualizacionConCorreo("ANA@example.com"));

        assertTrue(usuario.isCorreoVerificado());
        verify(verificacionCorreoService, never()).enviar(any());
    }

    @Test
    void cambiarLaContrasenaCierraLasOtrasSesionesYDevuelveUnTokenValido() {
        String clave = "k".repeat(12);
        Usuario usuario = usuarioConClave(clave);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken("7", "USER", 1)).thenReturn("token-nuevo");

        String token = service.updatePassword(7L, clave, "n".repeat(12));

        assertEquals("token-nuevo", token);
        assertEquals(1, usuario.getTokenVersion());
        assertTrue(new BCryptPasswordEncoder().matches("n".repeat(12), usuario.getContrasena()));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarLaContrasenaConLaActualIncorrectaNoTocaLasSesiones() {
        Usuario usuario = usuarioConClave("k".repeat(12));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        assertThrows(ContrasenaActualIncorrectaException.class,
                () -> service.updatePassword(7L, "z".repeat(12), "n".repeat(12)));

        assertEquals(0, usuario.getTokenVersion());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void elLoginEmiteElTokenConLaVersionActualDeLaCuenta() {
        String clave = "k".repeat(12);
        Usuario usuario = usuarioConClave(clave);
        usuario.invalidarSesiones();
        usuario.invalidarSesiones();
        when(usuarioRepository.findByNombreUsuario("ana")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generateToken("7", "USER", 2)).thenReturn("token-v2");
        com.bidco.api_rest.dto.usuario.LoginDTO login = new com.bidco.api_rest.dto.usuario.LoginDTO();
        login.setIdentificador("ana");
        login.setContrasena(clave);

        assertEquals("token-v2", service.login(login));
    }
}
