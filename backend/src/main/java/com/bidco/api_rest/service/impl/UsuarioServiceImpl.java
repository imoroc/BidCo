package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.config.JwtUtil;
import com.bidco.api_rest.exception.ContrasenaActualIncorrectaException;
import com.bidco.api_rest.dto.usuario.LoginDTO;
import com.bidco.api_rest.dto.usuario.PerfilPublicoDTO;
import com.bidco.api_rest.dto.usuario.PrivacidadDTO;
import com.bidco.api_rest.dto.usuario.StatsDTO;
import com.bidco.api_rest.dto.usuario.UsuarioCreateDTO;
import com.bidco.api_rest.dto.usuario.UsuarioResponseDTO;
import com.bidco.api_rest.dto.usuario.UsuarioUpdateDTO;
import com.bidco.api_rest.mapper.UsuarioMapper;
import com.bidco.api_rest.model.Usuario;
import com.bidco.api_rest.repository.PujaRepository;
import com.bidco.api_rest.repository.PujaSorteoRepository;
import com.bidco.api_rest.repository.SorteoRepository;
import com.bidco.api_rest.repository.SubastaRepository;
import com.bidco.api_rest.repository.TokenRecuperacionRepository;
import com.bidco.api_rest.repository.UsuarioRepository;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.service.contract.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final BigDecimal MAX_RECARGA = new BigDecimal("10000");

    private final UsuarioRepository usuarioRepository;
    private final PujaRepository pujaRepository;
    private final SubastaRepository subastaRepository;
    private final PujaSorteoRepository pujaSorteoRepository;
    private final UsuarioMapper usuarioMapper;
    private final SorteoRepository sorteoRepository;
    private final JwtUtil jwtUtil;
    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final Clock clock;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            UsuarioMapper usuarioMapper,
            PujaRepository pujaRepository,
            SubastaRepository subastaRepository,
            PujaSorteoRepository pujaSorteoRepository,
            SorteoRepository sorteoRepository,
            JwtUtil jwtUtil,
            TokenRecuperacionRepository tokenRecuperacionRepository,
            Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.pujaRepository = pujaRepository;
        this.subastaRepository = subastaRepository;
        this.pujaSorteoRepository = pujaSorteoRepository;
        this.sorteoRepository = sorteoRepository;
        this.jwtUtil = jwtUtil;
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
        this.clock = clock;
    }

    @Override
    public UsuarioResponseDTO registrarUsuario(UsuarioCreateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }
        if (usuarioRepository.findByNombreUsuario(dto.getNombreUsuario()).isPresent()) {
            throw new IllegalStateException("El nombre de usuario ya está en uso");
        }
        if (usuarioRepository.findByCorreoElectronico(dto.getCorreoElectronico()).isPresent()) {
            throw new IllegalStateException("El correo electrónico ya está registrado");
        }

        Usuario usuario = usuarioMapper.usuarioCreateDTOToUsuario(dto);
        usuario.setContrasena(passwordEncoder.encode(dto.getContraseña()));
        // El saldo y los puntos iniciales son reglas del servidor, nunca datos del cliente.
        usuario.setSaldo(BigDecimal.ZERO);
        usuario.setPuntos(10);
        return usuarioMapper.usuarioToUsuarioResponseDTO(usuarioRepository.save(usuario));
    }

    @Override
    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        usuarioRepository.findByNombreUsuario(dto.getNombreUsuario())
                .filter(otro -> !otro.getUsuarioID().equals(id))
                .ifPresent(otro -> { throw new IllegalStateException("El nombre de usuario ya está en uso"); });
        usuarioRepository.findByCorreoElectronico(dto.getCorreoElectronico())
                .filter(otro -> !otro.getUsuarioID().equals(id))
                .ifPresent(otro -> { throw new IllegalStateException("El correo electrónico ya está registrado"); });

        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setCorreoElectronico(dto.getCorreoElectronico());
        usuario.setCiudad(dto.getCiudad());
        usuario.setCodigoPostal(dto.getCodigoPostal());
        usuario.setCalle(dto.getCalle());
        usuario.setNumeroPiso(dto.getNumeroPiso());
        usuario.setLetraPiso(dto.getLetraPiso());

        return usuarioMapper.usuarioToUsuarioResponseDTO(usuarioRepository.save(usuario));
    }

    @Override
    public UsuarioResponseDTO buscarUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario con ID " + id + " no encontrado"));
        return usuarioMapper.usuarioToUsuarioResponseDTO(usuario);
    }

    @Override
    public UsuarioResponseDTO buscarUsuarioPorNombreUsuario(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return usuarioMapper.usuarioToUsuarioResponseDTO(usuario);
    }

    @Override
    public PerfilPublicoDTO obtenerPerfilPublico(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return toPublicProfile(usuario);
    }

    @Override
    public PerfilPublicoDTO buscarPerfilPublicoPorNombreUsuario(String nombreUsuario) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        return toPublicProfile(usuario);
    }

    private PerfilPublicoDTO toPublicProfile(Usuario usuario) {
        return new PerfilPublicoDTO(usuario.getNombreUsuario(), Boolean.TRUE.equals(usuario.getPrivacidadAnonimoPujas()));
    }

    @Override
    public String login(LoginDTO loginDTO) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByNombreUsuario(loginDTO.getIdentificador());
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByCorreoElectronico(loginDTO.getIdentificador());
        }
        if (usuarioOpt.isEmpty()) {
            throw new AuthenticationFailedException("Credenciales incorrectas");
        }

        Usuario usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(loginDTO.getContrasena(), usuario.getContrasena())) {
            throw new AuthenticationFailedException("Credenciales incorrectas");
        }
        return jwtUtil.generateToken(usuario.getUsuarioID().toString(), "USER");
    }

    @Override
    public void updatePassword(Long id, String currentPassword, String newPassword) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        if (currentPassword == null || !passwordEncoder.matches(currentPassword, usuario.getContrasena())) {
            throw new ContrasenaActualIncorrectaException("Contraseña actual incorrecta");
        }
        if (newPassword == null || newPassword.length() < 10 || newPassword.length() > 100) {
            throw new IllegalArgumentException("La nueva contraseña debe tener entre 10 y 100 caracteres");
        }

        usuario.setContrasena(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
    }

    @Override
    public PrivacidadDTO getPrivacidad(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        PrivacidadDTO dto = new PrivacidadDTO();
        dto.setPrivacidadAnonimoPujas(usuario.getPrivacidadAnonimoPujas());
        dto.setPrivacidadEstadisticas(usuario.getPrivacidadEstadisticas());
        dto.setPrivacidadPerfilVisible(usuario.getPrivacidadPerfilVisible());
        return dto;
    }

    @Override
    public UsuarioResponseDTO actualizarPrivacidad(Long id, PrivacidadDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        usuario.setPrivacidadAnonimoPujas(dto.getPrivacidadAnonimoPujas());
        usuario.setPrivacidadEstadisticas(dto.getPrivacidadEstadisticas());
        usuario.setPrivacidadPerfilVisible(dto.getPrivacidadPerfilVisible());
        return usuarioMapper.usuarioToUsuarioResponseDTO(usuarioRepository.save(usuario));
    }

    @Override
    public StatsDTO getStats(Long usuarioId) {
        return new StatsDTO(
                pujaRepository.countParticipatedBidsByUsuarioId(usuarioId),
                pujaRepository.countWonBidsByUsuarioId(usuarioId),
                subastaRepository.countCreatedBidsByUsuarioId(usuarioId),
                pujaSorteoRepository.countParticipatedDrawsByUsuarioId(usuarioId),
                sorteoRepository.countWonDrawsByUsuarioId(usuarioId),
                0
        );
    }

    @Override
    @Transactional
    public BigDecimal recargarSaldo(Long usuarioId, BigDecimal cantidad) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (cantidad.compareTo(MAX_RECARGA) > 0) {
            throw new IllegalArgumentException("La cantidad máxima por recarga es 10.000€");
        }

        // Con bloqueo de fila para no perder recargas frente a pujas u otras recargas simultáneas.
        Usuario usuario = usuarioRepository.findByIdForUpdate(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));
        usuario.setSaldo(usuario.getSaldo().add(cantidad));
        usuarioRepository.save(usuario);
        return usuario.getSaldo();
    }

    /**
     * Elimina la cuenta anonimizándola: se borran los datos personales y se conserva la fila para no romper
     * las pujas, subastas y sorteos ya cerrados. No se permite mientras haya algo sin cerrar en lo que el
     * usuario participa, porque su resultado dependería de una persona que ya no existe.
     */
    @Override
    @Transactional
    public void eliminarCuenta(Long id, String contrasena) {
        Usuario usuario = usuarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        if (contrasena == null || !passwordEncoder.matches(contrasena, usuario.getContrasena())) {
            throw new ContrasenaActualIncorrectaException("Contraseña incorrecta");
        }

        long pendientes = subastaRepository.countPendientesPorCreador(id, clock.instant())
                + subastaRepository.countPendientesConPujasDe(id)
                + sorteoRepository.countPendientesConParticipacionDe(id);
        if (pendientes > 0) {
            throw new IllegalStateException(
                    "No puedes eliminar la cuenta mientras tengas subastas o sorteos sin cerrar");
        }

        // El nombre y el correo son únicos y obligatorios: se sustituyen por valores derivados del id.
        // La contraseña pasa a un hash de un valor aleatorio que nadie conoce, así que no se puede entrar.
        usuario.setNombreUsuario("usuario-eliminado-" + id);
        usuario.setCorreoElectronico("eliminado-" + id + "@anonimo.invalid");
        usuario.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setSaldo(BigDecimal.ZERO);
        usuario.setPuntos(0);
        usuario.setPais("-");
        usuario.setCiudad("-");
        usuario.setCodigoPostal("-");
        usuario.setCalle("-");
        usuario.setNumeroPiso(0);
        usuario.setLetraPiso(null);
        usuario.setPrivacidadAnonimoPujas(true);
        usuario.setPrivacidadEstadisticas(false);
        usuario.setPrivacidadPerfilVisible(false);
        usuarioRepository.save(usuario);

        tokenRecuperacionRepository.eliminarPorUsuario(id);
    }
}
