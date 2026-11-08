package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.PujaSorteo;
import com.bidco.api_rest.model.Sorteo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SorteoRepository extends JpaRepository<Sorteo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sorteo s WHERE s.sorteoID = :id")
    Optional<Sorteo> findByIdForUpdate(@Param("id") Long id);
    @Query(value = "select P.* from puja_sorteo P inner join sorteo S on P.sorteo_id=S.sorteo_id where S.sorteo_id = ?1 and P.puntos >= ALL ( select P1.puntos from puja_sorteo P1 inner join sorteo S1 on P1.sorteo_id=S1.sorteo_id where S1.sorteo_id = ?1)",nativeQuery = true )
    Optional<PujaSorteo> findGanadorSorteoBySorteoId(Long id);

    @Query(value = "select P.puntos from puja_sorteo P inner join sorteo S on P.sorteo_id=S.sorteo_id where S.sorteo_id = ?1 and P.puntos >= ALL ( select P1.puntos from puja_sorteo P1 inner join sorteo S1 on P1.sorteo_id=S1.sorteo_id where S1.sorteo_id = ?1)",nativeQuery = true )
    Optional<Integer> findImporteMayorBySorteoId(Long id);
    
    List<Sorteo> findAllByOrderByFechaInicioDesc();

    @Query("SELECT COUNT(s) FROM Sorteo s WHERE s.ganador = :usuarioID")
    int countWonDrawsByUsuarioId(Long usuarioID);

    @Query("SELECT s FROM Sorteo s WHERE s.puntosNecesarios >= :minPrice AND s.puntosNecesarios <= :maxPrice AND (:todasCategorias = TRUE OR LOWER(s.categoria) IN :categorias) ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFin END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFin END DESC, s.fechaInicio DESC")
    List<Sorteo> findByFiltro(int minPrice, int maxPrice, String[] categorias, boolean todasCategorias, String dateOrder);
}
