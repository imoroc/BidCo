package com.bidco.api_rest.service.impl;

import com.bidco.api_rest.config.JwtUtil;
import com.bidco.api_rest.dto.trabajador.LoginTrabajadorDTO;
import com.bidco.api_rest.dto.trabajador.TrabajadorCreateDTO;
import com.bidco.api_rest.dto.trabajador.TrabajadorResponseDTO;
import com.bidco.api_rest.mapper.TrabajadorMapper;
import com.bidco.api_rest.model.Trabajador;
import com.bidco.api_rest.repository.TrabajadorRepository;
import com.bidco.api_rest.exception.AuthenticationFailedException;
import com.bidco.api_rest.exception.ForbiddenException;
import com.bidco.api_rest.service.contract.TrabajadorService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TrabajadorServiceImpl implements TrabajadorService {

    private final TrabajadorRepository trabajadorRepository;
    private final TrabajadorMapper trabajadorMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public TrabajadorServiceImpl(TrabajadorRepository trabajadorRepository, TrabajadorMapper trabajadorMapper, JwtUtil jwtUtil) {
        this.trabajadorRepository = trabajadorRepository;
        this.trabajadorMapper = trabajadorMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public TrabajadorResponseDTO registrarTrabajador(TrabajadorCreateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("El trabajador no puede ser nulo");
        }
        Trabajador trabajador = trabajadorMapper.trabajadorCreateDTOToTrabajador(dto);
        trabajador.setContraseña(passwordEncoder.encode(dto.getContraseña()));
        return trabajadorMapper.trabajadorToTrabajadorResponseDTO(trabajadorRepository.save(trabajador));
    }

    @Override
    public TrabajadorResponseDTO actualizarTrabajador(Long solicitanteId, TrabajadorCreateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("El trabajador no puede ser nulo");
        }
        Trabajador trabajador = trabajadorRepository.findByNombreUsuario(dto.getNombreUsuario())
                .orElseThrow(() -> new EntityNotFoundException("Trabajador no encontrado"));

        // Cada trabajador solo puede modificar su propia cuenta.
        if (solicitanteId == null || !solicitanteId.equals(trabajador.getTrabajadorID())) {
            throw new ForbiddenException("No puedes modificar los datos de otro trabajador");
        }

        trabajador.setCorreoElectronico(dto.getCorreoElectronico());
        if (dto.getContraseña() != null && !dto.getContraseña().isBlank()) {
            trabajador.setContraseña(passwordEncoder.encode(dto.getContraseña()));
        }

        return trabajadorMapper.trabajadorToTrabajadorResponseDTO(trabajadorRepository.save(trabajador));
    }

    @Override
    public TrabajadorResponseDTO buscarTrabajadorPorId(Long id) {
        Trabajador trabajador = trabajadorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trabajador no encontrado"));
        return trabajadorMapper.trabajadorToTrabajadorResponseDTO(trabajador);
    }

    @Override
    public TrabajadorResponseDTO buscarTrabajadorPorNombreUsuario(String nombreUsuario) {
        Trabajador trabajador = trabajadorRepository.findByNombreUsuario(nombreUsuario)
                .orElseThrow(() -> new EntityNotFoundException("Trabajador no encontrado"));
        return trabajadorMapper.trabajadorToTrabajadorResponseDTO(trabajador);
    }

    @Override
    public String login(LoginTrabajadorDTO dto) {
        Optional<Trabajador> trabajadorOpt = trabajadorRepository.findByNombreUsuario(dto.getIdentificador());
        if (trabajadorOpt.isEmpty()) {
            trabajadorOpt = trabajadorRepository.findByCorreoElectronico(dto.getIdentificador());
        }
        if (trabajadorOpt.isEmpty()) {
            throw new AuthenticationFailedException("Credenciales incorrectas");
        }

        Trabajador trabajador = trabajadorOpt.get();
        if (!passwordEncoder.matches(dto.getContraseña(), trabajador.getContraseña()) || trabajador.isExperto() != dto.getRol()) {
            throw new AuthenticationFailedException("Credenciales incorrectas");
        }

        String role = trabajador.isExperto() ? "EXPERT" : "WORKER";
        return jwtUtil.generateToken(trabajador.getTrabajadorID().toString(), role);
    }
}
