package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Proveedores;
import com.pharmacore.pharmacore.repository.ProveedoresRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/proveedores")
public class ProveedoresController {

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @GetMapping
    public List<Proveedores> getAll() {
        return proveedoresRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proveedores> getById(@PathVariable Integer id) {
        return proveedoresRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Proveedores> create(@RequestBody Proveedores proveedores) {
        Proveedores guardado = proveedoresRepository.save(proveedores);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Proveedores> update(@PathVariable Integer id, @RequestBody Proveedores proveedores) {
        if (!proveedoresRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        proveedores.setId_proveedor(id);
        return ResponseEntity.ok(proveedoresRepository.save(proveedores));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!proveedoresRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        proveedoresRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
