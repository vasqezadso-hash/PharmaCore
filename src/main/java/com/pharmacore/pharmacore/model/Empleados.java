package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity

//se trae la tabla empleados
@Table(name = "empleados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

//se cra la clase para poder traer los datos
public class Empleados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_empleado;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombre_completo;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipo_documento;

    @NotBlank(message = "El número de documento es obligatorio")
    private String numero_documento;

    private String telefono;

    private String direccion;

    private String correo;

    @NotBlank(message = "El cargo es obligatorio")
    private String cargo;

    @NotNull(message = "El salario es obligatorio")
    private BigDecimal salario;

    private String estado;

    // Este campo es especial: en la tabla empleados NO existe ninguna columna
    // para el usuario (la relación va al revés, en usuarios.id_empleado).
    // Le puse @Transient para decirle a Hibernate "este campo no lo busques
    // ni lo guardes en la base de datos, solo lo uso en memoria mientras
    // armo el formulario". Lo lleno a mano en EmpleadosView cuando necesito
    // mostrar o editar los datos del usuario junto con los del empleado.
    @Transient
    private Usuarios usuario;
}