package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Proveedores;
import com.pharmacore.pharmacore.model.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProveedoresRepository extends JpaRepository<Proveedores, Integer>
{

}