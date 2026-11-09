package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Productos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @NotBlank(message = "El código de barras es obligatorio")
    @Column(name = "codigo_barras", unique = true, nullable = false, length = 50)
    private String codigoBarras;

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "nombre_generico", length = 150)
    private String nombreGenerico;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "presentacion", length = 100)
    private String presentacion;

    @Builder.Default
    @Column(name = "unidades_por_caja")
    private Integer unidadesPorCaja = 1;

    @Builder.Default
    @Column(name = "blisteres_por_caja")
    private Integer blisteresPorCaja = 1;

    @Column(name = "concentracion", length = 50)
    private String concentracion;

    @Column(name = "laboratorio", length = 100)
    private String laboratorio;

    @NotBlank(message = "El registro INVIMA es obligatorio")
    @Column(name = "registro_invima", nullable = false, length = 50)
    private String registroInvima;

    @NotNull(message = "La categoría es obligatoria")
    @ManyToOne
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categorias categoria;

    @NotNull(message = "El proveedor es obligatorio")
    @ManyToOne
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedores proveedor;

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio de compra debe ser mayor a 0")
    @Column(name = "precio_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompra;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio de venta debe ser mayor a 0")
    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Builder.Default
    @Column(name = "stock_total")
    private Integer stockTotal = 0;

    @Builder.Default
    @Column(name = "stock_minimo")
    private Integer stockMinimo = 10;

    @Column(name = "ubicacion_estante", length = 50)
    private String ubicacionEstante;

    @Builder.Default
    @Column(name = "requiere_formula")
    private Boolean requiereFormula = false;

    @Builder.Default
    @Column(name = "es_venta_libre")
    private Boolean esVentaLibre = true;

    @Builder.Default
    @Column(name = "es_controlado")
    private Boolean esControlado = false;

    @Builder.Default
    @Column(name = "requiere_refrigeracion")
    private Boolean requiereRefrigeracion = false;

    @Column(name = "restricciones_venta", length = 200)
    private String restriccionesVenta;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoProducto estado = EstadoProducto.ACTIVO;

    public enum EstadoProducto {
        ACTIVO,
        INACTIVO,
        DESCONTINUADO
    }

    /** Útil para el dashboard/inventario más adelante: true si el stock ya tocó (o pasó) el mínimo. */
    public boolean isStockBajo() {
        return stockTotal != null && stockMinimo != null && stockTotal <= stockMinimo;
    }

    /**
     * Calcula cuántas unidades individuales hay contenidas en un blíster.
     */
    public int getUnidadesPorBlister() {
        int uCaja = (unidadesPorCaja != null && unidadesPorCaja > 0) ? unidadesPorCaja : 1;
        int bCaja = (blisteresPorCaja != null && blisteresPorCaja > 0) ? blisteresPorCaja : 1;
        if (bCaja > 1 && uCaja > 1) {
            return Math.max(1, Math.round((float) uCaja / bCaja));
        }
        return 1;
    }

    /**
     * Convierte la cantidad vendida según el formato (CAJA, BLISTER, UNIDAD)
     * a unidades base de inventario para el descuento exacto de los lotes.
     */
    public int calcularUnidadesBase(String formato, int cantidad) {
        if (cantidad <= 0) return 0;
        if ("BLISTER".equalsIgnoreCase(formato)) {
            return cantidad * getUnidadesPorBlister();
        } else if ("UNIDAD".equalsIgnoreCase(formato)) {
            return cantidad;
        }
        // Por defecto: CAJA
        int uCaja = (unidadesPorCaja != null && unidadesPorCaja > 0) ? unidadesPorCaja : 1;
        return cantidad * uCaja;
    }
}
