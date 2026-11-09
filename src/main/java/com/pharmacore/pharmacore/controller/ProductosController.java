package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.repository.ProductosRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductosController {

    @Autowired
    private ProductosRepository productosRepository;

    @GetMapping
    public List<Productos> getAll() {
        return productosRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Productos> getById(@PathVariable Integer id) {
        return productosRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // /api/productos/buscar?q=acetaminofen
    @GetMapping("/buscar")
    public List<Productos> buscar(@RequestParam("q") String texto) {
        return productosRepository
                .findByNombreComercialContainingIgnoreCaseOrNombreGenericoContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(
                        texto, texto, texto);
    }

    // /api/productos/stock-bajo
    @GetMapping("/stock-bajo")
    public List<Productos> stockBajo() {
        return productosRepository.findAll().stream()
                .filter(Productos::isStockBajo)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody Productos producto) {
        String error = validarDuplicados(producto);
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }
        Productos guardado = productosRepository.save(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody Productos producto) {
        if (!productosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        producto.setIdProducto(id);
        String error = validarDuplicados(producto);
        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }
        return ResponseEntity.ok(productosRepository.save(producto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!productosRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productosRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Evita chocar con el UNIQUE de codigo_barras antes de llegar a la base de datos
    private String validarDuplicados(Productos producto) {
        var porCodigoBarras = productosRepository.findByCodigoBarras(producto.getCodigoBarras());
        if (porCodigoBarras.isPresent() && !porCodigoBarras.get().getIdProducto().equals(producto.getIdProducto())) {
            return "Ya existe un producto con ese código de barras.";
        }
        return null;
    }
}
