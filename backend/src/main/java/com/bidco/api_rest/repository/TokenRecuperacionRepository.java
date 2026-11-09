package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenRecuperacion t WHERE t.usuarioId = :usuarioId")
    void eliminarPorUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TokenRecuperacion t WHERE t.expiraEn < :limite")
    void eliminarCaducados(@Param("limite") Instant limite);
}
