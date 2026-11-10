package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.Subasta;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Repository
public interface SubastaRepository extends JpaRepository<Subasta, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subasta s WHERE s.subastaID = :id")
    Optional<Subasta> findByIdForUpdate(@Param("id") Long id);

    List<Subasta> findBySubastaNormalOrderByFechaInicialDesc(boolean subastaNormal);

    /** Subastas ya vencidas que recibieron pujas y todavía no tienen ganador asignado. */
    @Query("SELECT s.subastaID FROM Subasta s WHERE s.fechaFinal <= :ahora AND s.ganador IS NULL "
            + "AND EXISTS (SELECT 1 FROM Puja p WHERE p.subasta = s) ORDER BY s.fechaFinal ASC")
    List<Long> findIdsVencidasConPujasSinGanador(@Param("ahora") Instant ahora);
    
    @Query("SELECT s FROM Subasta s WHERE s.creador.id = :id ORDER BY s.fechaInicial DESC")
    List<Subasta> findByCreadorId(@Param("id") Long id);

    @Query("SELECT COUNT(s) FROM Subasta s WHERE s.creador.usuarioID = :usuarioID")
    int countCreatedBidsByUsuarioId(Long usuarioID);

    // Subastas sin cerrar de un creador: aún en plazo, o vencidas con pujas a la espera de ganador.
    @Query("SELECT COUNT(s) FROM Subasta s WHERE s.creador.usuarioID = :usuarioID AND s.ganador IS NULL "
            + "AND (s.fechaFinal > :ahora OR EXISTS (SELECT 1 FROM Puja p WHERE p.subasta = s))")
    long countPendientesPorCreador(@Param("usuarioID") Long usuarioID, @Param("ahora") Instant ahora);

    // Subastas sin cerrar en las que el usuario tiene alguna puja.
    @Query("SELECT COUNT(DISTINCT s) FROM Subasta s JOIN s.pujas p WHERE p.pujador.usuarioID = :usuarioID "
            + "AND s.ganador IS NULL")
    long countPendientesConPujasDe(@Param("usuarioID") Long usuarioID);

    @Query("SELECT s FROM Subasta s WHERE s.subastaNormal = TRUE AND (CASE WHEN s.precioFinal > 0 THEN s.precioFinal ELSE s.precioInicial END) BETWEEN :minPrice AND :maxPrice AND (:todasCategorias = TRUE OR LOWER(s.categoria) IN :categorias) ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroNormal(int minPrice, int maxPrice, String[] categorias, boolean todasCategorias, String dateOrder, Pageable pageable);

    @Query("SELECT s FROM Subasta s WHERE s.subastaNormal = FALSE AND (CASE WHEN s.precioFinal > 0 THEN s.precioFinal ELSE s.precioInicial END) BETWEEN :minPrice AND :maxPrice AND (:todasCategorias = TRUE OR LOWER(s.categoria) IN :categorias) ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroCiega(int minPrice, int maxPrice, String[] categorias, boolean todasCategorias, String dateOrder, Pageable pageable);

    @Query("SELECT s FROM Subasta s WHERE s.creador.id = :id AND (CASE WHEN s.precioFinal > 0 THEN s.precioFinal ELSE s.precioInicial END) BETWEEN :minPrice AND :maxPrice AND (:todasCategorias = TRUE OR LOWER(s.categoria) IN :categorias) ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroMisSubastas(int id, int minPrice, int maxPrice, String[] categorias, boolean todasCategorias, String dateOrder, Pageable pageable);

    @Query("SELECT s FROM Subasta s JOIN s.pujas p ON s.subastaID = p.subasta.subastaID WHERE p.pujador.id = :id AND (CASE WHEN s.precioFinal > 0 THEN s.precioFinal ELSE s.precioInicial END) BETWEEN :minPrice AND :maxPrice AND (:todasCategorias = TRUE OR LOWER(s.categoria) IN :categorias) ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroMisPujas(int id, int minPrice, int maxPrice, String[] categorias, boolean todasCategorias, String dateOrder, Pageable pageable);

    @Query("SELECT s FROM Subasta s WHERE LOWER(s.nombreArticulo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND (CASE WHEN s.precioFinal > 0 THEN s.precioFinal ELSE s.precioInicial END) BETWEEN :minPrice AND :maxPrice AND (:todasCategorias = TRUE OR LOWER(s.categoria) IN :categorias) ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroNombre(String searchTerm, int minPrice, int maxPrice, String[] categorias, boolean todasCategorias, String dateOrder, Pageable pageable);
}
