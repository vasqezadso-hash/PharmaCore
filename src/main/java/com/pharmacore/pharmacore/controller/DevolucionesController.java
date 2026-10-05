package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Devoluciones;
import com.pharmacore.pharmacore.repository.DevolucionesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devoluciones")
public class DevolucionesController {

    @Autowired
    private DevolucionesRepository devolucionesRepository;

    @GetMapping
    public List<Devoluciones> getAll() {
        return devolucionesRepository.findAllByOrderByFechaDevolucionDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Devoluciones> getById(@PathVariable Integer id) {
        return devolucionesRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
