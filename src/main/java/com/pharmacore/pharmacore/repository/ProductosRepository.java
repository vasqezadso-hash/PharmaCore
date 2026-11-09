package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Productos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductosRepository extends JpaRepository<Productos, Integer> {

    Optional<Productos> findByCodigoBarras(String codigoBarras);

    List<Productos> findByEstado(Productos.EstadoProducto estado);

    List<Productos> findByNombreComercialContainingIgnoreCaseOrNombreGenericoContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(
            String nombreComercial, String nombreGenerico, String codigoBarras);

    List<Productos> findByStockTotalLessThanEqual(Integer stockMinimoComparado);
}
