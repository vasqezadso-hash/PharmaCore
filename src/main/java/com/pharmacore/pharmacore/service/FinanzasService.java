package com.pharmacore.pharmacore.service;

import com.pharmacore.pharmacore.model.*;
import com.pharmacore.pharmacore.repository.MovimientosInventarioRepository;
import com.pharmacore.pharmacore.repository.TransaccionesFinancierasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FinanzasService {

    @Autowired
    private TransaccionesFinancierasRepository transaccionesRepository;

    @Autowired
    private MovimientosInventarioRepository movimientosInventarioRepository;

    @Autowired
    private InventarioService inventarioService;

    /**
     * Registra el ingreso de dinero por cada venta realizada.
     * Vincula la transaccion con todos los movimientos de inventario de salida generados.
     */
    @Transactional
    public TransaccionesFinancieras registrarIngresoVenta(Ventas venta, Integer idUsuario, List<MovimientosInventario> movimientos) {
        if (venta == null || venta.getTotal() == null || venta.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Integer primerIdMovimiento = (movimientos != null && !movimientos.isEmpty()) ? movimientos.get(0).getIdMovimiento() : null;

        TransaccionesFinancieras tf = TransaccionesFinancieras.builder()
                .tipoTransaccion(TransaccionesFinancieras.TIPO_INGRESO)
                .concepto(TransaccionesFinancieras.CONCEPTO_VENTA)
                .monto(venta.getTotal())
                .metodoPago(venta.getMetodoPago())
                .fechaTransaccion(venta.getFechaVenta() != null ? venta.getFechaVenta() : LocalDateTime.now())
                .idUsuario(idUsuario != null ? idUsuario : 1)
                .idVenta(venta.getIdVenta())
                .idMovimientoInventario(primerIdMovimiento)
                .descripcion("Ingreso por venta Factura " + venta.getNumeroFactura())
                .referencia(venta.getNumeroFactura())
                .build();

        TransaccionesFinancieras guardada = transaccionesRepository.save(tf);

        if (movimientos != null && !movimientos.isEmpty()) {
            for (MovimientosInventario m : movimientos) {
                if (m != null && m.getIdMovimiento() != null) {
                    inventarioService.vincularTransaccion(m.getIdMovimiento(), guardada.getIdTransaccion());
                }
            }
        }

        return guardada;
    }

    /**
     * Registra la salida de dinero por compras a proveedores.
     * Vincula la transaccion con los movimientos de entrada de mercancia generados.
     */
    @Transactional
    public TransaccionesFinancieras registrarEgresoCompra(Compras compra, Integer idUsuario, List<MovimientosInventario> movimientos) {
        if (compra == null || compra.getTotal() == null || compra.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Integer primerIdMovimiento = (movimientos != null && !movimientos.isEmpty()) ? movimientos.get(0).getIdMovimiento() : null;

        String ref = (compra.getNumeroFacturaProveedor() != null && !compra.getNumeroFacturaProveedor().isBlank())
                ? compra.getNumeroFacturaProveedor()
                : ("OC-" + compra.getIdCompra());

        TransaccionesFinancieras tf = TransaccionesFinancieras.builder()
                .tipoTransaccion(TransaccionesFinancieras.TIPO_EGRESO)
                .concepto(TransaccionesFinancieras.CONCEPTO_COMPRA)
                .monto(compra.getTotal())
                .metodoPago(compra.getFormaPago())
                .fechaTransaccion(LocalDateTime.now())
                .idUsuario(idUsuario != null ? idUsuario : 1)
                .idCompra(compra.getIdCompra())
                .idMovimientoInventario(primerIdMovimiento)
                .descripcion("Salida de dinero por compra #" + compra.getIdCompra() + " (Proveedor: #" + compra.getIdProveedor() + ")")
                .referencia(ref)
                .build();

        TransaccionesFinancieras guardada = transaccionesRepository.save(tf);

        if (movimientos != null && !movimientos.isEmpty()) {
            for (MovimientosInventario m : movimientos) {
                if (m != null && m.getIdMovimiento() != null) {
                    inventarioService.vincularTransaccion(m.getIdMovimiento(), guardada.getIdTransaccion());
                }
            }
        }

        return guardada;
    }

    /**
     * Registra el egreso por concepto de devolucion en efectivo/pago al cliente.
     */
    @Transactional
    public TransaccionesFinancieras registrarEgresoDevolucion(Devoluciones devolucion, BigDecimal montoReembolso,
                                                               String metodoPago, Integer idUsuario,
                                                               MovimientosInventario movimientoReingreso) {
        if (devolucion == null || montoReembolso == null || montoReembolso.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Integer idMov = movimientoReingreso != null ? movimientoReingreso.getIdMovimiento() : null;

        TransaccionesFinancieras tf = TransaccionesFinancieras.builder()
                .tipoTransaccion(TransaccionesFinancieras.TIPO_EGRESO)
                .concepto(TransaccionesFinancieras.CONCEPTO_DEVOLUCION)
                .monto(montoReembolso)
                .metodoPago(metodoPago != null ? metodoPago : "EFECTIVO")
                .fechaTransaccion(devolucion.getFechaDevolucion() != null ? devolucion.getFechaDevolucion() : LocalDateTime.now())
                .idUsuario(idUsuario != null ? idUsuario : 1)
                .idDevolucion(devolucion.getIdDevolucion())
                .idVenta(devolucion.getIdVenta())
                .idMovimientoInventario(idMov)
                .descripcion("Reembolso por devolucion de " + devolucion.getCantidad() + " unidad(es) (Motivo: " + devolucion.getMotivo() + ")")
                .referencia("DEV-" + devolucion.getIdDevolucion())
                .build();

        TransaccionesFinancieras guardada = transaccionesRepository.save(tf);

        if (idMov != null) {
            inventarioService.vincularTransaccion(idMov, guardada.getIdTransaccion());
        }

        return guardada;
    }

    /**
     * Registra el egreso por anulacion de venta completa.
     */
    @Transactional
    public TransaccionesFinancieras registrarEgresoAnulacionVenta(Ventas venta, BigDecimal montoReembolso,
                                                                   Integer idUsuario, List<MovimientosInventario> movimientos) {
        if (venta == null || montoReembolso == null || montoReembolso.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Integer primerIdMovimiento = (movimientos != null && !movimientos.isEmpty()) ? movimientos.get(0).getIdMovimiento() : null;

        TransaccionesFinancieras tf = TransaccionesFinancieras.builder()
                .tipoTransaccion(TransaccionesFinancieras.TIPO_EGRESO)
                .concepto(TransaccionesFinancieras.CONCEPTO_ANULACION_VENTA)
                .monto(montoReembolso)
                .metodoPago(venta.getMetodoPago())
                .fechaTransaccion(LocalDateTime.now())
                .idUsuario(idUsuario != null ? idUsuario : 1)
                .idVenta(venta.getIdVenta())
                .idMovimientoInventario(primerIdMovimiento)
                .descripcion("Reembolso por anulacion total de Venta Factura " + venta.getNumeroFactura())
                .referencia(venta.getNumeroFactura())
                .build();

        TransaccionesFinancieras guardada = transaccionesRepository.save(tf);

        if (movimientos != null && !movimientos.isEmpty()) {
            for (MovimientosInventario m : movimientos) {
                if (m != null && m.getIdMovimiento() != null) {
                    inventarioService.vincularTransaccion(m.getIdMovimiento(), guardada.getIdTransaccion());
                }
            }
        }

        return guardada;
    }

    /**
     * Registra un ajuste directo de caja (ingreso o egreso manual).
     */
    @Transactional
    public TransaccionesFinancieras registrarAjusteManual(String tipo, String concepto, BigDecimal monto,
                                                           String metodoPago, String descripcion, Integer idUsuario) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        if (!TransaccionesFinancieras.TIPO_INGRESO.equals(tipo) && !TransaccionesFinancieras.TIPO_EGRESO.equals(tipo)) {
            throw new IllegalArgumentException("El tipo de transaccion debe ser INGRESO o EGRESO.");
        }

        TransaccionesFinancieras tf = TransaccionesFinancieras.builder()
                .tipoTransaccion(tipo)
                .concepto(concepto != null && !concepto.isBlank() ? concepto : (TransaccionesFinancieras.TIPO_INGRESO.equals(tipo) ? TransaccionesFinancieras.CONCEPTO_AJUSTE_INGRESO : TransaccionesFinancieras.CONCEPTO_AJUSTE_EGRESO))
                .monto(monto)
                .metodoPago(metodoPago != null && !metodoPago.isBlank() ? metodoPago : "EFECTIVO")
                .fechaTransaccion(LocalDateTime.now())
                .idUsuario(idUsuario != null ? idUsuario : 1)
                .descripcion(descripcion != null && !descripcion.isBlank() ? descripcion : "Ajuste manual de caja")
                .referencia("AJU-" + (System.currentTimeMillis() % 1000000))
                .build();

        return transaccionesRepository.save(tf);
    }
}
