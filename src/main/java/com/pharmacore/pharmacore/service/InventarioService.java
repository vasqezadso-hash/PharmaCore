package com.pharmacore.pharmacore.service;

import com.pharmacore.pharmacore.model.Lotes;
import com.pharmacore.pharmacore.model.MovimientosInventario;
import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.repository.LotesRepository;
import com.pharmacore.pharmacore.repository.MovimientosInventarioRepository;
import com.pharmacore.pharmacore.repository.ProductosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

// Esta clase es el corazón del manejo de inventario del proyecto.
// La idea principal es que NINGÚN otro módulo (Ventas, Compras, Devoluciones)
// le cambie el stock a un producto directamente. Todos tienen que pasar por
// este servicio, para que siempre quede un registro de qué pasó y por qué
// (eso es lo que en la sustentación llamo el "Kardex": el historial de
// movimientos de inventario).
@Service
public class InventarioService {

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private LotesRepository lotesRepository;

    @Autowired
    private MovimientosInventarioRepository movimientosRepository;

    // Tipos que SUMAN al stock (entra mercancía)
    private static final Set<String> TIPOS_ENTRADA = Set.of("ENTRADA_COMPRA", "DEVOLUCION_CLIENTE");

    // Tipos que RESTAN del stock (sale mercancía)
    private static final Set<String> TIPOS_SALIDA = Set.of("SALIDA_VENTA", "AJUSTE_PERDIDA", "AJUSTE_DANIO", "DEVOLUCION_PROVEEDOR");

    // Este es el único método que puede mover inventario en todo el sistema.
    // Compras lo llama cuando llega mercancía, Ventas cuando se factura,
    // y Devoluciones cuando corresponde reponer o sacar stock.
    // Hacerlo así evita que cada módulo calcule el stock a su manera y que
    // se desincronice el número que se ve en pantalla con lo que pasó de verdad.
    public MovimientosInventario registrarMovimiento(String tipoMovimiento, Integer idProducto, Integer idLote,
                                                       Integer cantidad, Integer idUsuario, String motivo) {

        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad del movimiento debe ser mayor a cero.");
        }
        if (!TIPOS_ENTRADA.contains(tipoMovimiento) && !TIPOS_SALIDA.contains(tipoMovimiento)) {
            throw new IllegalArgumentException("Tipo de movimiento no reconocido: " + tipoMovimiento);
        }

        Productos producto = productosRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("El producto indicado no existe."));

        int existenciaAnterior = producto.getStockTotal() != null ? producto.getStockTotal() : 0;
        boolean esEntrada = TIPOS_ENTRADA.contains(tipoMovimiento);
        int nuevaExistencia = esEntrada ? existenciaAnterior + cantidad : existenciaAnterior - cantidad;

        // No dejo que el stock quede en negativo bajo ninguna circunstancia.
        // Si alguien intenta vender más de lo que hay disponible, se corta acá
        // con un mensaje claro, antes de guardar nada en la base de datos.
        if (nuevaExistencia < 0) {
            throw new IllegalStateException("La operación dejaría el stock de \"" + producto.getNombreComercial()
                    + "\" en negativo (disponible: " + existenciaAnterior + ", solicitado: " + cantidad + ").");
        }

        // Si el movimiento está atado a un lote específico, se actualiza también su cantidad_actual
        if (idLote != null) {
            Lotes lote = lotesRepository.findById(idLote)
                    .orElseThrow(() -> new IllegalArgumentException("El lote indicado no existe."));
            int cantidadLoteAnterior = lote.getCantidadActual() != null ? lote.getCantidadActual() : 0;
            int nuevaCantidadLote = esEntrada ? cantidadLoteAnterior + cantidad : cantidadLoteAnterior - cantidad;
            if (nuevaCantidadLote < 0) {
                throw new IllegalStateException("La operación dejaría el lote \"" + lote.getNumeroLote() + "\" en negativo.");
            }
            lote.setCantidadActual(nuevaCantidadLote);
            lotesRepository.save(lote);
        }

        producto.setStockTotal(nuevaExistencia);
        productosRepository.save(producto);

        // Acá queda el registro histórico del movimiento (el Kardex). Guardo
        // la existencia ANTES y DESPUÉS del movimiento, no solo la cantidad,
        // para que después se pueda auditar exactamente qué pasó en cada paso.
        MovimientosInventario movimiento = MovimientosInventario.builder()
                .tipoMovimiento(tipoMovimiento)
                .idProducto(idProducto)
                .idLote(idLote)
                .cantidad(cantidad)
                .existenciaAnterior(existenciaAnterior)
                .nuevaExistencia(nuevaExistencia)
                .idUsuario(idUsuario)
                .motivo(motivo)
                .build();

        return movimientosRepository.save(movimiento);
    }

    @Transactional
    public void vincularTransaccion(Integer idMovimiento, Integer idTransaccion) {
        if (idMovimiento == null || idTransaccion == null) return;
        movimientosRepository.findById(idMovimiento).ifPresent(m -> {
            m.setIdTransaccion(idTransaccion);
            movimientosRepository.save(m);
        });
    }

    @Transactional
    public void vincularTransaccionMultiple(Iterable<Integer> idsMovimientos, Integer idTransaccion) {
        if (idsMovimientos == null || idTransaccion == null) return;
        for (Integer idMov : idsMovimientos) {
            vincularTransaccion(idMov, idTransaccion);
        }
    }
}
