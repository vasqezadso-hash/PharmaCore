package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Un domicilio siempre nace de una venta ya facturada, por eso id_venta es
// único: una venta no puede tener dos domicilios. Este módulo no tiene forma
// de "eliminar" un domicilio, solo de cambiarle el estado (ver
// DomiciliosView.cambiarEstado). Lo hice así a propósito: si se pudiera
// borrar, se perdería el rastro de que esa venta tuvo un intento de entrega,
// aunque se haya cancelado. Es la misma idea que ya usa Ventas con "anular".
public class Domicilios {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_domicilio")
    private Integer idDomicilio;

    @NotNull(message = "Debe seleccionar la venta que se va a entregar a domicilio")
    @Column(name = "id_venta", nullable = false, unique = true)
    private Long idVenta;

    @NotNull(message = "El cliente es obligatorio")
    @Column(name = "id_cliente", nullable = false)
    private Long idCliente;

    @NotBlank(message = "La dirección de entrega es obligatoria")
    @Column(name = "direccion_entrega", nullable = false, length = 200)
    private String direccionEntrega;

    @NotBlank(message = "El teléfono de contacto es obligatorio")
    @Column(name = "telefono_contacto", nullable = false, length = 15)
    private String telefonoContacto;

    @Column(name = "nombre_domiciliario", length = 100)
    private String nombreDomiciliario;

    @Builder.Default
    @Column(name = "costo_domicilio", precision = 10, scale = 2)
    private BigDecimal costoDomicilio = BigDecimal.ZERO;

    @Builder.Default
    @Column(length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    @Column(name = "fecha_hora_entrega")
    private LocalDateTime fechaHoraEntrega;

    public static final String[] ESTADOS = {
            "PENDIENTE", "EN_PREPARACION", "EN_CAMINO", "ENTREGADO", "CANCELADO"
    };
}
