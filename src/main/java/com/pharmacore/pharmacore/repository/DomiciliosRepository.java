package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Domicilios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomiciliosRepository extends JpaRepository<Domicilios, Integer> {
    boolean existsByIdVenta(Long idVenta);
    Optional<Domicilios> findByIdVenta(Long idVenta);
    List<Domicilios> findAllByOrderByIdDomicilioDesc();
}
