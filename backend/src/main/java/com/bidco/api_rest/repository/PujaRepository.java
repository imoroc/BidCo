package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.Subasta;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PujaRepository extends JpaRepository<Puja, Long> {
    @Query("SELECT COUNT(p) FROM Puja p WHERE p.pujador.usuarioID = :usuarioID")
    int countParticipatedBidsByUsuarioId(Long usuarioID);
    
    @Query("SELECT COUNT(p) FROM Puja p WHERE p.pujador.usuarioID = :usuarioID AND p.ganadora = true")
    int countWonBidsByUsuarioId(Long usuarioID);

    @Query("SELECT p FROM Puja p WHERE p.subasta.subastaID = :subastaId ORDER BY p.importe DESC LIMIT 1")
    Puja findTopBySubastaIdOrderByImporteDesc(Long subastaId);

    @Query("SELECT p FROM Puja p WHERE p.pujador.usuarioID = :usuarioID ORDER BY p.fecha DESC, p.pujaID DESC")
    List<Puja> findByUsuarioIdOrdenadas(@Param("usuarioID") Long usuarioID);

    @Query("SELECT p.subasta FROM Puja p WHERE p.pujador.id = :usuarioId")
    List<Subasta> findSubastasByUsuarioId(@Param("usuarioId") Long usuarioId);
}