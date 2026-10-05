package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuariosRepository extends JpaRepository<Usuarios, Long> {

    Usuarios findByIdEmpleado(Long idEmpleado);

    Usuarios findByUsernameIgnoreCase(String username);

}