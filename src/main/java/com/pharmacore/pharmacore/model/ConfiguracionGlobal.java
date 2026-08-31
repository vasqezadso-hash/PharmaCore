package com.pharmacore.pharmacore.model; // Ajusta según tu paquete base

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "configuracion_global")
public class ConfiguracionGlobal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_farmacia", nullable = false, length = 150)
    private String nombreFarmacia;

    @Column(nullable = false, length = 50)
    private String nit;

    @Column(length = 30)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Column(name = "iva_general", precision = 5, scale = 2)
    private BigDecimal ivaGeneral;

    @Column(length = 10)
    private String moneda;

    @Column(name = "tiempo_alerta_vencimiento_dias")
    private Integer tiempoAlertaVencimientoDias;

    // Constructores por defecto
    public ConfiguracionGlobal() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreFarmacia() { return nombreFarmacia; }
    public void setNombreFarmacia(String nombreFarmacia) { this.nombreFarmacia = nombreFarmacia; }

    public String getNit() { return nit; }
    public void setNit(String nit) { this.nit = nit; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public BigDecimal getIvaGeneral() { return ivaGeneral; }
    public void setIvaGeneral(BigDecimal ivaGeneral) { this.ivaGeneral = ivaGeneral; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }

    public Integer getTiempoAlertaVencimientoDias() { return tiempoAlertaVencimientoDias; }
    public void setTiempoAlertaVencimientoDias(Integer tiempoAlertaVencimientoDias) { this.tiempoAlertaVencimientoDias = tiempoAlertaVencimientoDias; }
}