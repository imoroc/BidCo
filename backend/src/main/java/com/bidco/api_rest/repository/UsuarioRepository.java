package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.usuarioID = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT * FROM usuario U WHERE U.nombre_usuario = ?1", nativeQuery = true)
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    Optional<Usuario> findByCorreoElectronico(String correoElectronico);
}
