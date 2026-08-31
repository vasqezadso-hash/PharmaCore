package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clientes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_cliente;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    @NotBlank(message = "El número de documento es obligatorio")
    @Column(name = "numero_documento", nullable = false, unique = true, length = 20)
    private String numeroDocumento;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "correo", length = 100)
    private String correo;

    @Column(name = "fecha_nacimiento")
    private String fechaNacimiento; // O LocalDate si prefieres manejar fechas con Java Time

    @Column(name = "eps", length = 100)
    private String eps;

    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private String fechaRegistro;
}