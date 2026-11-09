package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Devoluciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucionesRepository extends JpaRepository<Devoluciones, Integer> {
    List<Devoluciones> findAllByOrderByFechaDevolucionDesc();

    List<Devoluciones> findByIdVenta(Long idVenta);

    List<Devoluciones> findByIdVentaAndIdProducto(Long idVenta, Integer idProducto);
}
