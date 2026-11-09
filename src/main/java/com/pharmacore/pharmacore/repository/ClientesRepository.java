package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Clientes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientesRepository extends JpaRepository<Clientes, Long> {
    Optional<Clientes> findByNumeroDocumento(String numeroDocumento);
}
