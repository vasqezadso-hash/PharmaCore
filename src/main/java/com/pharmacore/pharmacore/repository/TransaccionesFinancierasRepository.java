package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.TransaccionesFinancieras;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionesFinancierasRepository extends JpaRepository<TransaccionesFinancieras, Integer> {

    List<TransaccionesFinancieras> findAllByOrderByFechaTransaccionDesc();

    List<TransaccionesFinancieras> findByTipoTransaccionOrderByFechaTransaccionDesc(String tipoTransaccion);

    List<TransaccionesFinancieras> findByConceptoOrderByFechaTransaccionDesc(String concepto);

    List<TransaccionesFinancieras> findByFechaTransaccionBetweenOrderByFechaTransaccionDesc(LocalDateTime start, LocalDateTime end);

    Optional<TransaccionesFinancieras> findByIdVenta(Long idVenta);

    Optional<TransaccionesFinancieras> findByIdCompra(Integer idCompra);

    Optional<TransaccionesFinancieras> findByIdDevolucion(Integer idDevolucion);

    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM TransaccionesFinancieras t WHERE t.tipoTransaccion = :tipo")
    BigDecimal sumMontoByTipo(@Param("tipo") String tipo);

    @Query("SELECT COALESCE(SUM(t.monto), 0) FROM TransaccionesFinancieras t WHERE t.tipoTransaccion = :tipo AND t.fechaTransaccion BETWEEN :inicio AND :fin")
    BigDecimal sumMontoByTipoAndFechaBetween(@Param("tipo") String tipo, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
