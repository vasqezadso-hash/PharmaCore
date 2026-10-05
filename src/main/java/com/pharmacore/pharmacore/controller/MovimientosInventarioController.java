package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.MovimientosInventario;
import com.pharmacore.pharmacore.repository.MovimientosInventarioRepository;
import com.pharmacore.pharmacore.service.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movimientos-inventario")
public class MovimientosInventarioController {

    @Autowired
    private MovimientosInventarioRepository movimientosRepository;

    @Autowired
    private InventarioService inventarioService;

    @GetMapping
    public List<MovimientosInventario> getAll() {
        return movimientosRepository.findAllByOrderByFechaMovimientoDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimientosInventario> getById(@PathVariable Integer id) {
        return movimientosRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/producto/{idProducto}")
    public List<MovimientosInventario> porProducto(@PathVariable Integer idProducto) {
        return movimientosRepository.findByIdProductoOrderByFechaMovimientoDesc(idProducto);
    }

    @Transactional
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            MovimientosInventario movimiento = inventarioService.registrarMovimiento(
                    (String) body.get("tipoMovimiento"),
                    (Integer) body.get("idProducto"),
                    (Integer) body.get("idLote"),
                    (Integer) body.get("cantidad"),
                    (Integer) body.get("idUsuario"),
                    (String) body.get("motivo")
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(movimiento);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
