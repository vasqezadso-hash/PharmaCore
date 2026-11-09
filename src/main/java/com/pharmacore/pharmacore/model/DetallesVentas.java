package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "detalles_ventas")
public class DetallesVentas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta")
    private Long idDetalleVenta;

    @ManyToOne
    @JoinColumn(name = "id_venta", nullable = false)
    private Ventas venta;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_lote", nullable = false)
    private Integer idLote;

    @Column(name = "formato", length = 20)
    private String formato = "CAJA";

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;
}
