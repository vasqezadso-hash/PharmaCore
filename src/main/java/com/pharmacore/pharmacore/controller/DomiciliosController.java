package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Domicilios;
import com.pharmacore.pharmacore.repository.DomiciliosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/domicilios")
public class DomiciliosController {

    @Autowired
    private DomiciliosRepository domiciliosRepository;

    @GetMapping
    public List<Domicilios> getAll() {
        return domiciliosRepository.findAllByOrderByIdDomicilioDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Domicilios> getById(@PathVariable Integer id) {
        return domiciliosRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Domicilios domicilio) {
        if (domicilio.getIdVenta() == null) {
            return ResponseEntity.badRequest().body("Debe indicar la venta asociada al domicilio.");
        }
        if (domiciliosRepository.existsByIdVenta(domicilio.getIdVenta())) {
            return ResponseEntity.badRequest().body("Esa venta ya tiene un domicilio registrado.");
        }
        domicilio.setIdDomicilio(null);
        if (domicilio.getEstado() == null) {
            domicilio.setEstado("PENDIENTE");
        }
        Domicilios guardado = domiciliosRepository.save(domicilio);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody Domicilios cambios) {
        return domiciliosRepository.findById(id)
                .map(existente -> {
                    if (cambios.getDireccionEntrega() != null) {
                        existente.setDireccionEntrega(cambios.getDireccionEntrega());
                    }
                    if (cambios.getTelefonoContacto() != null) {
                        existente.setTelefonoContacto(cambios.getTelefonoContacto());
                    }
                    if (cambios.getNombreDomiciliario() != null) {
                        existente.setNombreDomiciliario(cambios.getNombreDomiciliario());
                    }
                    if (cambios.getCostoDomicilio() != null) {
                        existente.setCostoDomicilio(cambios.getCostoDomicilio());
                    }
                    if (cambios.getEstado() != null) {
                        existente.setEstado(cambios.getEstado());
                    }
                    return ResponseEntity.ok(domiciliosRepository.save(existente));
                })
                .orElse(ResponseEntity.notFound().build());
    }

}
