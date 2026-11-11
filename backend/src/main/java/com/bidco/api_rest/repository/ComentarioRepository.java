package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.Comentario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
    @Query(value = "SELECT * FROM comentario C WHERE C.subasta_id = ?1", nativeQuery = true)
    List<Comentario> findComentariosBySubastaId(Long id);

    @Modifying
    @Query("DELETE FROM Comentario c WHERE c.subasta.subastaID = :subastaId")
    void eliminarPorSubasta(@Param("subastaId") Long subastaId);
}