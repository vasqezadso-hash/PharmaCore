package com.pharmacore.pharmacore.repository;

import com.pharmacore.pharmacore.model.Usuarios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuariosRepository extends JpaRepository<Usuarios, Long> {

    // Cambiado de findByid_usuario a findByIdUsuario para que coincida con el atributo en camelCase
    Usuarios findByIdEmpleado(Long idEmpleado);

    // Se usa para resolver el usuario logueado (Authentication.getName()) en Devoluciones,
    // donde se necesita id_usuario_regente sin que el formulario tenga que volver a pedirlo.
    Usuarios findByUsernameIgnoreCase(String username);

}