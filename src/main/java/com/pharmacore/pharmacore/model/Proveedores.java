//by Jacob Mafla
package com.pharmacore.pharmacore.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "proveedores")
@Getter
@Setter
public class Proveedores {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_proveedor;

    @NotBlank(message = "la razon social es obligatoria")
    private String razon_social;

    @NotBlank(message = "el nit es obligatori")
    private String nit;

    @NotBlank(message = "la direccion es obligatoria")
    private String direccion;

    @NotBlank(message = "el telefono es obligatorio")
    private String telefono;

    //correo
    @NotBlank(message = "el correo es obligatorio")
    private String correo;

    //contacto
    @NotBlank(message = "el nombre_contacto es obligatorio")
    private String nombre_contacto;

    //tipo de producto
    @NotBlank(message = "los tipo_productos son obligatorios")
    private String tipo_productos;

    //estado
    @NotNull(message = "el estado es obligatorio")
    @Enumerated(EnumType.STRING) // <-- Cámbialo a STRING para que guarde el texto y no un número
    private EstadoProveedor estado;

    //declaramos el esstado como un ENUM
    public enum EstadoProveedor {
        ACTIVO,
        INACTIVO
    }
}