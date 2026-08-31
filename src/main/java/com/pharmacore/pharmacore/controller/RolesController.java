package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Roles;
import com.pharmacore.pharmacore.repository.RolesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolesController {

    @Autowired
    private RolesRepository rolesRepository;

    @GetMapping
    public List<Roles> getAll() {
        return rolesRepository.findAll();
    }

    @GetMapping("/{id}")
    public Roles getById(@PathVariable Integer id) { // Cambiado de Long a Integer
        return rolesRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Roles create(@RequestBody Roles rol) {
        return rolesRepository.save(rol);
    }

    @PutMapping("/{id}")
    public Roles update(@PathVariable Integer id, @RequestBody Roles rol) { // Cambiado de Long a Integer
        rol.setIdRol(id); // Asegúrate de usar el método camelCase si tu getter/setter se generó así, o id_rol según corresponda
        return rolesRepository.save(rol);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) { // Cambiado de Long a Integer
        rolesRepository.deleteById(id);
    }
}