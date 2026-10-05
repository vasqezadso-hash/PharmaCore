package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "lotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lotes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Integer idLote;

    @NotNull(message = "El producto es obligatorio")
    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "numero_lote", nullable = false, length = 50)
    private String numeroLote;

    @Column(name = "fecha_fabricacion")
    private LocalDate fechaFabricacion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "cantidad_actual", nullable = false)
    private Integer cantidadActual;

    @Builder.Default
    @Column(name = "estado")
    private String estado = "DISPONIBLE";

    @Transient
    public String getEstadoCalculado() {
        if ("RETIRADO".equals(estado) || "DEVUELTO".equals(estado)) {
            return estado;
        }
        if (cantidadActual != null && cantidadActual <= 0) {
            return "AGOTADO";
        }
        if (fechaVencimiento == null) {
            return estado;
        }
        java.time.LocalDate hoy = java.time.LocalDate.now();
        if (fechaVencimiento.isBefore(hoy)) {
            return "VENCIDO";
        }
        if (!fechaVencimiento.isAfter(hoy.plusDays(30))) {
            return "PROXIMO_A_VENCER";
        }
        return "DISPONIBLE";
    }

    @Transient
    public boolean isVencido() {
        return fechaVencimiento != null && fechaVencimiento.isBefore(java.time.LocalDate.now());
    }
}
