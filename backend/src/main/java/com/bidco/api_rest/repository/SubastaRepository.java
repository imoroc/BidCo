package com.bidco.api_rest.repository;

import com.bidco.api_rest.model.Puja;
import com.bidco.api_rest.model.Subasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

@Repository
public interface SubastaRepository extends JpaRepository<Subasta, Long> {

    @Query(value = "select P.* from puja P inner join subasta S on P.subasta_id=S.subasta_id where S.subasta_id = ?1 and P.importe >= ALL ( select P1.importe from puja P1 inner join subasta S1 on P1.subasta_id=S1.subasta_id where S1.subasta_id = ?1)",nativeQuery = true )
    Optional<Puja> findGanadorSubastaBySubastaId(Long id);

    @Query(value = "select P.importe from puja P inner join subasta S on P.subasta_id=S.subasta_id where S.subasta_id = ?1 and P.importe >= ALL ( select P1.importe from puja P1 inner join subasta S1 on P1.subasta_id=S1.subasta_id where S1.subasta_id = ?1)",nativeQuery = true )
    Optional<BigDecimal> findImporteMayorBySubastaId(Long id);

    List<Subasta> findBySubastaNormalOrderByFechaInicialDesc(boolean subastaNormal);
    
    @Query("SELECT s FROM Subasta s WHERE s.creador.id = :id ORDER BY s.fechaInicial DESC")
    List<Subasta> findByCreadorId(@Param("id") Long id);

    @Query("SELECT COUNT(s) FROM Subasta s WHERE s.creador.usuarioID = :usuarioID")
    int countCreatedBidsByUsuarioId(Long usuarioID);

    @Query("SELECT s FROM Subasta s WHERE s.subastaNormal = TRUE AND s.precioFinal >= :minPrice AND s.precioFinal <= :maxPrice AND s.categoria in :categorias ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroNormal(int minPrice, int maxPrice, String[] categorias, String dateOrder);

    @Query("SELECT s FROM Subasta s WHERE s.subastaNormal = FALSE AND s.precioFinal >= :minPrice AND s.precioFinal <= :maxPrice AND s.categoria in :categorias ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroCiega(int minPrice, int maxPrice, String[] categorias, String dateOrder);

    @Query("SELECT s FROM Subasta s WHERE s.creador.id = :id AND s.precioFinal >= :minPrice AND s.precioFinal <= :maxPrice AND s.categoria in :categorias ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroMisSubastas(int id, int minPrice, int maxPrice, String[] categorias, String dateOrder);

    @Query("SELECT s FROM Subasta s JOIN s.pujas p ON s.subastaID = p.subasta.subastaID WHERE p.pujador.id = :id AND s.precioFinal >= :minPrice AND s.precioFinal <= :maxPrice AND s.categoria in :categorias ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroMisPujas(int id, int minPrice, int maxPrice, String[] categorias, String dateOrder);

    @Query("SELECT s FROM Subasta s WHERE LOWER(s.nombreArticulo) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND s.precioFinal >= :minPrice AND s.precioFinal <= :maxPrice AND s.categoria in :categorias ORDER BY CASE WHEN :dateOrder = 'asc' THEN s.fechaFinal END ASC, CASE WHEN :dateOrder = 'desc' THEN s.fechaFinal END DESC, s.fechaInicial DESC")
    List<Subasta> findByFiltroNombre(String searchTerm, int minPrice, int maxPrice, String[] categorias, String dateOrder);
}
