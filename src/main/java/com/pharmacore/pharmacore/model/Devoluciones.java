package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "devoluciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Devoluciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_devolucion")
    private Integer idDevolucion;

    @NotNull(message = "El tipo de devolución es obligatorio")
    @Column(name = "tipo_devolucion", nullable = false, length = 20)
    private String tipoDevolucion; // CLIENTE | PROVEEDOR

    @Column(name = "id_venta")
    private Long idVenta;

    @Column(name = "id_compra")
    private Integer idCompra;

    @NotNull(message = "El producto es obligatorio")
    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Builder.Default
    @Column(name = "formato", length = 20, nullable = false)
    private String formato = "UNIDAD";

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a cero")
    @Column(nullable = false)
    private Integer cantidad;

    @NotNull(message = "El motivo de la devolución es obligatorio")
    @Column(nullable = false, length = 30)
    private String motivo;

    @NotNull(message = "Debe indicar el estado en el que queda el producto devuelto")
    @Column(name = "estado_producto", nullable = false, length = 30)
    private String estadoProducto;

    @Builder.Default
    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion = LocalDateTime.now();

    @Column(name = "id_usuario_regente", nullable = false)
    private Long idUsuarioRegente;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    public static final String[] TIPOS = {"CLIENTE", "PROVEEDOR"};

    public static final String[] MOTIVOS = {
            "PRODUCTO_DEFECTUOSO", "ERROR_ENTREGA", "PROXIMO_A_VENCER",
            "VENCIDO", "RETIRO_MERCADO", "EMPAQUE_DANADO"
    };

    public static final String[] ESTADOS_PRODUCTO = {
            "APTO_PARA_REINGRESO", "DESECHADO", "DEVUELTO_A_FABRICA"
    };
}
