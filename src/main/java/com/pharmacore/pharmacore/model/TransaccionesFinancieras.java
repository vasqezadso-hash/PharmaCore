package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones_financieras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransaccionesFinancieras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Integer idTransaccion;

    @NotNull(message = "El tipo de transaccion es obligatorio")
    @Column(name = "tipo_transaccion", nullable = false, length = 20)
    private String tipoTransaccion; // INGRESO | EGRESO

    @NotNull(message = "El concepto es obligatorio")
    @Column(nullable = false, length = 50)
    private String concepto;

    @NotNull(message = "El monto es obligatorio")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @Builder.Default
    @Column(name = "fecha_transaccion")
    private LocalDateTime fechaTransaccion = LocalDateTime.now();

    @NotNull(message = "El usuario responsable es obligatorio")
    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_venta")
    private Long idVenta;

    @Column(name = "id_compra")
    private Integer idCompra;

    @Column(name = "id_devolucion")
    private Integer idDevolucion;

    @Column(name = "id_movimiento_inventario")
    private Integer idMovimientoInventario;

    @Column(length = 255)
    private String descripcion;

    @Column(length = 100)
    private String referencia;

    public static final String TIPO_INGRESO = "INGRESO";
    public static final String TIPO_EGRESO = "EGRESO";

    public static final String CONCEPTO_VENTA = "VENTA";
    public static final String CONCEPTO_COMPRA = "COMPRA_PROVEEDOR";
    public static final String CONCEPTO_DEVOLUCION = "DEVOLUCION_VENTA";
    public static final String CONCEPTO_ANULACION_VENTA = "ANULACION_VENTA";
    public static final String CONCEPTO_AJUSTE_INGRESO = "AJUSTE_INGRESO";
    public static final String CONCEPTO_AJUSTE_EGRESO = "AJUSTE_EGRESO";
}
