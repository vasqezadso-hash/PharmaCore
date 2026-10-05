package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Lotes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotesRepository extends JpaRepository<Lotes, Integer> {

    List<Lotes> findByIdProductoOrderByFechaVencimientoAsc(Integer idProducto);
}
