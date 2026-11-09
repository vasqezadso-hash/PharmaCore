package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Historial de movimientos de inventario (Kardex).
 * Es un registro histórico: no se edita ni se elimina, solo se agregan filas nuevas
 * (por eso el REST y la vista no tienen PUT/DELETE, solo GET y POST).
 */
@Entity
@Table(name = "movimientos_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientosInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @NotNull(message = "El tipo de movimiento es obligatorio")
    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private String tipoMovimiento;

    @NotNull(message = "El producto es obligatorio")
    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_lote")
    private Integer idLote;

    @NotNull(message = "La cantidad es obligatoria")
    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "existencia_anterior", nullable = false)
    private Integer existenciaAnterior;

    @Column(name = "nueva_existencia", nullable = false)
    private Integer nuevaExistencia;

    @Builder.Default
    @Column(name = "fecha_movimiento")
    private LocalDateTime fechaMovimiento = LocalDateTime.now();

    @NotNull(message = "El usuario responsable es obligatorio")
    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(length = 255)
    private String motivo;

    @Column(name = "id_transaccion")
    private Integer idTransaccion;

    /** Tipos válidos, tal como quedaron en el ENUM de la tabla en MySQL. */
    public static final String[] TIPOS_VALIDOS = {
            "ENTRADA_COMPRA", "SALIDA_VENTA", "AJUSTE_PERDIDA",
            "AJUSTE_DANIO", "DEVOLUCION_CLIENTE", "DEVOLUCION_PROVEEDOR"
    };
}
