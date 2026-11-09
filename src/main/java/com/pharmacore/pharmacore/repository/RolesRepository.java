package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;

public interface RolesRepository extends JpaRepository<Roles, Integer> {

    Optional<Roles> findByNombreIgnoreCase(String nombre);

}