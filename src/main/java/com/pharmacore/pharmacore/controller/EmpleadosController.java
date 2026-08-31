package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Empleados;
import com.pharmacore.pharmacore.repository.EmpleadosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadosController {

    @Autowired
    private EmpleadosRepository empleadosRepository;

    @GetMapping
    public List<Empleados> getAll() {
        return empleadosRepository.findAll();
    }

    @GetMapping("/{id}")
    public Empleados getById(@PathVariable Long id) {
        return empleadosRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Empleados create(@RequestBody Empleados empleados) {
        return empleadosRepository.save(empleados);
    }

    @PutMapping("/{id}")
    public Empleados update(@PathVariable Long id, @RequestBody Empleados empleados) {
        empleados.setId_empleado(id);
        return empleadosRepository.save(empleados);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        empleadosRepository.deleteById(id);
    }
}