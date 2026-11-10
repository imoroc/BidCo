package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.PujaSorteo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PujaSorteoRepository extends JpaRepository<PujaSorteo, Long> {
    @Query("SELECT COUNT(ps) FROM PujaSorteo ps WHERE ps.pujador.usuarioID = :usuarioID")
    int countParticipatedDrawsByUsuarioId(Long usuarioID);

    @Query("SELECT ps FROM PujaSorteo ps WHERE ps.pujador.usuarioID = :usuarioID ORDER BY ps.fecha DESC, ps.pujaID DESC")
    List<PujaSorteo> findByUsuarioIdOrdenadas(@Param("usuarioID") Long usuarioID);

    List<PujaSorteo> findBySorteoSorteoID(Long sorteoId);
    
}
