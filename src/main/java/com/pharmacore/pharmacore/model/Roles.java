package com.pharmacore.pharmacore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(name = "nombre", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    // --- Permisos de acceso a módulos ---
    @Column(name = "acceso_pos", nullable = false)
    @Builder.Default
    private Boolean accesoPos = true;

    @Column(name = "acceso_ventas_historial", nullable = false)
    @Builder.Default
    private Boolean accesoVentasHistorial = true;

    @Column(name = "acceso_clientes", nullable = false)
    @Builder.Default
    private Boolean accesoClientes = true;

    @Column(name = "acceso_productos", nullable = false)
    @Builder.Default
    private Boolean accesoProductos = false;

    @Column(name = "acceso_categorias", nullable = false)
    @Builder.Default
    private Boolean accesoCategorias = false;

    @Column(name = "acceso_lotes", nullable = false)
    @Builder.Default
    private Boolean accesoLotes = false;

    @Column(name = "acceso_inventario", nullable = false)
    @Builder.Default
    private Boolean accesoInventario = false;

    @Column(name = "acceso_compras", nullable = false)
    @Builder.Default
    private Boolean accesoCompras = false;

    @Column(name = "acceso_proveedores", nullable = false)
    @Builder.Default
    private Boolean accesoProveedores = false;

    @Column(name = "acceso_devoluciones", nullable = false)
    @Builder.Default
    private Boolean accesoDevoluciones = false;

    @Column(name = "acceso_domicilios", nullable = false)
    @Builder.Default
    private Boolean accesoDomicilios = false;

    @Column(name = "acceso_finanzas", nullable = false)
    @Builder.Default
    private Boolean accesoFinanzas = false;

    @Column(name = "acceso_reportes", nullable = false)
    @Builder.Default
    private Boolean accesoReportes = false;

    @Column(name = "acceso_usuarios", nullable = false)
    @Builder.Default
    private Boolean accesoUsuarios = false;

    @OneToMany(mappedBy = "rol")
    private List<Usuarios> usuarios;

    /**
     * Devuelve el mapa de permisos en formato de códigos MOD_* para compatibilidad
     * con cualquier vista o condición de permisos.
     */
    public Map<String, Boolean> obtenerMapaPermisos() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put("MOD_POS", Boolean.TRUE.equals(accesoPos));
        map.put("MOD_VENTAS_HISTORIAL", Boolean.TRUE.equals(accesoVentasHistorial));
        map.put("MOD_CLIENTES", Boolean.TRUE.equals(accesoClientes));
        map.put("MOD_PRODUCTOS", Boolean.TRUE.equals(accesoProductos));
        map.put("MOD_CATEGORIAS", Boolean.TRUE.equals(accesoCategorias));
        map.put("MOD_LOTES", Boolean.TRUE.equals(accesoLotes));
        map.put("MOD_INVENTARIO", Boolean.TRUE.equals(accesoInventario));
        map.put("MOD_COMPRAS", Boolean.TRUE.equals(accesoCompras));
        map.put("MOD_PROVEEDORES", Boolean.TRUE.equals(accesoProveedores));
        map.put("MOD_DEVOLUCIONES", Boolean.TRUE.equals(accesoDevoluciones));
        map.put("MOD_DOMICILIOS", Boolean.TRUE.equals(accesoDomicilios));
        map.put("MOD_FINANZAS", Boolean.TRUE.equals(accesoFinanzas));
        map.put("MOD_REPORTES", Boolean.TRUE.equals(accesoReportes));
        map.put("MOD_USUARIOS", Boolean.TRUE.equals(accesoUsuarios));
        return map;
    }

    /**
     * Cuenta cuántos módulos funcionales tiene habilitados este rol.
     */
    public int getTotalPermisosHabilitados() {
        int count = 0;
        if (Boolean.TRUE.equals(accesoPos)) count++;
        if (Boolean.TRUE.equals(accesoVentasHistorial)) count++;
        if (Boolean.TRUE.equals(accesoClientes)) count++;
        if (Boolean.TRUE.equals(accesoProductos)) count++;
        if (Boolean.TRUE.equals(accesoCategorias)) count++;
        if (Boolean.TRUE.equals(accesoLotes)) count++;
        if (Boolean.TRUE.equals(accesoInventario)) count++;
        if (Boolean.TRUE.equals(accesoCompras)) count++;
        if (Boolean.TRUE.equals(accesoProveedores)) count++;
        if (Boolean.TRUE.equals(accesoDevoluciones)) count++;
        if (Boolean.TRUE.equals(accesoDomicilios)) count++;
        if (Boolean.TRUE.equals(accesoFinanzas)) count++;
        if (Boolean.TRUE.equals(accesoReportes)) count++;
        if (Boolean.TRUE.equals(accesoUsuarios)) count++;
        return count;
    }
}
