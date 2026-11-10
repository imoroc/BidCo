package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.Sorteo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SorteoRepository extends JpaRepository<Sorteo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sorteo s WHERE s.sorteoID = :id")
    Optional<Sorteo> findByIdForUpdate(@Param("id") Long id);

    List<Sorteo> findAllByOrderByFechaInicioDesc();

    /** Sorteos ya vencidos con participaciones y todavía sin ganador asignado. */
    @Query("SELECT s.sorteoID FROM Sorteo s WHERE s.fechaFin < :hoy AND s.ganador IS NULL "
            + "AND EXISTS (SELECT 1 FROM PujaSorteo p WHERE p.sorteo = s) ORDER BY s.fechaFin ASC")
    List<Long> findIdsVencidosConParticipacionesSinGanador(@Param("hoy") LocalDate hoy);

    @Query("SELECT COUNT(s) FROM Sorteo s WHERE s.ganador = :usuarioID")
    int countWonDrawsByUsuarioId(Long usuarioID);

    @Query("SELECT s FROM Sorteo s WHERE s.puntosNecesarios >= :minPrice AND s.puntosNecesarios <= :maxPrice AND (:todasCategorias = TRUE OR LOWER(s.categoria) IN :categorias) ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFin END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFin END DESC, s.fechaInicio DESC")
    List<Sorteo> findByFiltro(int minPrice, int maxPrice, String[] categorias, boolean todasCategorias, String dateOrder);
}
