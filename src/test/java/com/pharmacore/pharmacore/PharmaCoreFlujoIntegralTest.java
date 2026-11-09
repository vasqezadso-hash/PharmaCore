package com.pharmacore.pharmacore;

import com.pharmacore.pharmacore.model.*;
import com.pharmacore.pharmacore.repository.*;
import com.pharmacore.pharmacore.security.PharmaCoreUserDetailsService;
import com.pharmacore.pharmacore.service.FinanzasService;
import com.pharmacore.pharmacore.service.InventarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PharmaCoreFlujoIntegralTest {

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private CategoriasRepository categoriasRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private LotesRepository lotesRepository;

    @Autowired
    private MovimientosInventarioRepository movimientosInventarioRepository;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private FinanzasService finanzasService;

    @Autowired
    private TransaccionesFinancierasRepository transaccionesFinancierasRepository;

    @Autowired
    private VentasRepository ventasRepository;

    @Autowired
    private PharmaCoreUserDetailsService userDetailsService;

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Test
    @Order(1)
    @DisplayName("1. Prueba de Matriz de Roles y Permisos Dinámicos (14 módulos independientes)")
    void testMatrizRolesYPermisos() {
        // 1.1 Verificar ADMINISTRADOR
        Roles admin = rolesRepository.findByNombreIgnoreCase("ADMINISTRADOR").orElse(null);
        assertNotNull(admin, "El rol ADMINISTRADOR debe existir");
        Map<String, Boolean> permisosAdmin = admin.obtenerMapaPermisos();
        assertEquals(14, permisosAdmin.size(), "Debe haber exactamente 14 módulos evaluados");
        assertTrue(permisosAdmin.values().stream().allMatch(b -> b), "ADMINISTRADOR debe tener todos los 14 permisos en true");
        assertEquals(14, admin.getTotalPermisosHabilitados());

        // 1.2 Verificar FARMACEUTICO
        Roles farm = rolesRepository.findByNombreIgnoreCase("FARMACEUTICO").orElse(null);
        assertNotNull(farm, "El rol FARMACEUTICO debe existir");
        Map<String, Boolean> permisosFarm = farm.obtenerMapaPermisos();
        assertEquals(14, permisosFarm.size());
        assertTrue(permisosFarm.get("MOD_PRODUCTOS"), "FARMACEUTICO gestiona productos");
        assertTrue(permisosFarm.get("MOD_CATEGORIAS"), "FARMACEUTICO gestiona categorías");
        assertTrue(permisosFarm.get("MOD_LOTES"), "FARMACEUTICO gestiona lotes");
        assertTrue(permisosFarm.get("MOD_INVENTARIO"), "FARMACEUTICO gestiona inventario");
        assertTrue(permisosFarm.get("MOD_COMPRAS"), "FARMACEUTICO gestiona compras");
        assertFalse(permisosFarm.get("MOD_USUARIOS"), "FARMACEUTICO NO debe gestionar usuarios (solo Admin)");

        // 1.3 Verificar que el mapa de permisos de cualquier rol siempre cubre los 14 módulos
        Roles vend = rolesRepository.findByNombreIgnoreCase("VENDEDOR").orElse(null);
        assertNotNull(vend, "El rol VENDEDOR debe existir");
        Map<String, Boolean> permisosVend = vend.obtenerMapaPermisos();
        assertEquals(14, permisosVend.size());
        assertTrue(permisosVend.get("MOD_POS"), "VENDEDOR debe tener acceso a mostrador / POS");
        assertTrue(permisosVend.get("MOD_VENTAS_HISTORIAL"), "VENDEDOR debe consultar historial de ventas");
        assertTrue(permisosVend.get("MOD_CLIENTES"), "VENDEDOR debe ver/registrar clientes");
        assertFalse(permisosVend.get("MOD_CATEGORIAS"), "VENDEDOR no tiene acceso a categorías por defecto");

        // 1.4 Prueba de cambio dinámico: si se modifica un permiso, el mapa lo refleja de inmediato
        Roles rolPrueba = Roles.builder()
                .nombre("ROL_TEST_DINAMICO")
                .descripcion("Rol para prueba de dinamismo")
                .accesoPos(true)
                .accesoProductos(true)
                .accesoCategorias(false)
                .build();
        assertTrue(rolPrueba.obtenerMapaPermisos().get("MOD_PRODUCTOS"), "Tiene productos");
        assertFalse(rolPrueba.obtenerMapaPermisos().get("MOD_CATEGORIAS"), "Categorías debe ser independiente y estar en false");
        
        // Modificación en tiempo de ejecución
        rolPrueba.setAccesoCategorias(true);
        assertTrue(rolPrueba.obtenerMapaPermisos().get("MOD_CATEGORIAS"), "Al activar categorías, debe reflejarse de inmediato");
    }

    @Test
    @Order(2)
    @DisplayName("2. Prueba de Conversión de Formatos (Caja, Blíster y Pastilla suelta)")
    void testConversionFormatos() {
        // Producto con 20 unidades por caja y 2 blísteres (10 pastillas por blíster)
        Productos prod = Productos.builder()
                .unidadesPorCaja(20)
                .blisteresPorCaja(2)
                .build();

        assertEquals(10, prod.getUnidadesPorBlister(), "Cada blíster debe tener 20/2 = 10 pastillas");

        // Venta de 1 CAJA -> 20 pastillas
        assertEquals(20, prod.calcularUnidadesBase("CAJA", 1));

        // Venta de 3 CAJAS -> 60 pastillas
        assertEquals(60, prod.calcularUnidadesBase("CAJA", 3));

        // Venta de 1 BLÍSTER -> 10 pastillas
        assertEquals(10, prod.calcularUnidadesBase("BLISTER", 1));

        // Venta de 2 BLÍSTERES -> 20 pastillas
        assertEquals(20, prod.calcularUnidadesBase("BLISTER", 2));

        // Venta de 4 UNIDADES (pastillas sueltas) -> 4 pastillas
        assertEquals(4, prod.calcularUnidadesBase("UNIDAD", 4));

        // Formato nulo o por defecto calcula como caja
        assertEquals(20, prod.calcularUnidadesBase(null, 1));
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("3. Prueba de Descuento PEFO en Cascada y Registro en Kardex")
    void testDescuentoPefoYKardex() {
        Categorias cat = categoriasRepository.findAll().stream().findFirst().orElseThrow();
        Proveedores prov = proveedoresRepository.findAll().stream().findFirst().orElseThrow();

        // Crear un producto de prueba
        Productos productoTest = Productos.builder()
                .codigoBarras("TEST-" + System.currentTimeMillis())
                .nombreComercial("Medicamento PEFO Test")
                .laboratorio("Lab PharmaCore")
                .precioCompra(new BigDecimal("5000.00"))
                .precioVenta(new BigDecimal("8000.00"))
                .stockTotal(150)
                .stockMinimo(20)
                .unidadesPorCaja(10)
                .blisteresPorCaja(1)
                .registroInvima("INV-2026-TEST")
                .estado(Productos.EstadoProducto.ACTIVO)
                .categoria(cat)
                .proveedor(prov)
                .requiereFormula(false)
                .build();
        productoTest = productosRepository.save(productoTest);

        // Lote A: 50 unidades, vence PRONTO (en 10 días)
        Lotes loteA = Lotes.builder()
                .idProducto(productoTest.getIdProducto())
                .numeroLote("LOTE-PRONTO-" + System.currentTimeMillis())
                .cantidadInicial(50)
                .cantidadActual(50)
                .fechaFabricacion(LocalDate.now().minusMonths(6))
                .fechaVencimiento(LocalDate.now().plusDays(10))
                .estado("DISPONIBLE")
                .build();
        loteA = lotesRepository.save(loteA);

        // Lote B: 100 unidades, vence LEJOS (en 180 días)
        Lotes loteB = Lotes.builder()
                .idProducto(productoTest.getIdProducto())
                .numeroLote("LOTE-LEJOS-" + System.currentTimeMillis())
                .cantidadInicial(100)
                .cantidadActual(100)
                .fechaFabricacion(LocalDate.now().minusMonths(1))
                .fechaVencimiento(LocalDate.now().plusDays(180))
                .estado("DISPONIBLE")
                .build();
        loteB = lotesRepository.save(loteB);

        // Simular venta que requiere 65 unidades base (ej: 6 cajas + 5 pastillas = 65 unidades)
        int unidadesADescontar = 65;

        // 1. Obtener lotes ordenados por vencimiento ascendente (PEFO)
        List<Lotes> lotesPefo = lotesRepository.findByIdProductoOrderByFechaVencimientoAsc(productoTest.getIdProducto());
        assertEquals(2, lotesPefo.size());
        assertEquals(loteA.getIdLote(), lotesPefo.get(0).getIdLote(), "El lote A (vence primero) debe salir primero");

        // 2. Ejecutar la deducción en cascada PEFO
        int pendiente = unidadesADescontar;
        for (Lotes lote : lotesPefo) {
            if (pendiente <= 0) break;
            int enLote = lote.getCantidadActual();
            int aSacar = Math.min(enLote, pendiente);
            pendiente -= aSacar;

            // Registrar movimiento en el servicio de inventario (Kardex)
            inventarioService.registrarMovimiento(
                    "SALIDA_VENTA",
                    productoTest.getIdProducto(),
                    lote.getIdLote(),
                    aSacar,
                    1,
                    "Venta de prueba PEFO (" + aSacar + " unidades del lote " + lote.getNumeroLote() + ")"
            );
        }

        assertEquals(0, pendiente, "Todas las 65 unidades debieron descontarse");

        // 3. Verificar estado de los lotes
        Lotes lA = lotesRepository.findById(loteA.getIdLote()).orElseThrow();
        assertEquals(0, lA.getCantidadActual(), "Lote A debe quedar en 0 porque se le sacaron todas sus 50 unidades");

        Lotes lB = lotesRepository.findById(loteB.getIdLote()).orElseThrow();
        assertEquals(85, lB.getCantidadActual(), "Lote B debe quedar en 100 - 15 = 85 unidades");

        // 4. Verificar stock total del producto
        Productos prodActualizado = productosRepository.findById(productoTest.getIdProducto()).orElseThrow();
        assertEquals(85, prodActualizado.getStockTotal(), "Stock total del producto debe ser 150 - 65 = 85");

        // 5. Verificar que el Kardex registró los dos movimientos con existencias antes y después
        List<MovimientosInventario> kardex = movimientosInventarioRepository.findByIdProductoOrderByFechaMovimientoDesc(productoTest.getIdProducto());
        assertTrue(kardex.size() >= 2, "Deben existir al menos 2 registros en el Kardex");

        // Movimiento de 50 unidades (del lote que vence primero)
        MovimientosInventario mov50 = kardex.stream().filter(m -> m.getCantidad() == 50).findFirst().orElseThrow();
        assertEquals("SALIDA_VENTA", mov50.getTipoMovimiento());
        assertEquals(50, mov50.getCantidad());
        assertEquals(150, mov50.getExistenciaAnterior());
        assertEquals(100, mov50.getNuevaExistencia());

        // Movimiento de 15 unidades (del lote que vence después)
        MovimientosInventario mov15 = kardex.stream().filter(m -> m.getCantidad() == 15).findFirst().orElseThrow();
        assertEquals("SALIDA_VENTA", mov15.getTipoMovimiento());
        assertEquals(15, mov15.getCantidad());
        assertEquals(100, mov15.getExistenciaAnterior());
        assertEquals(85, mov15.getNuevaExistencia());
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("4. Prueba de Integración con Finanzas (Caja y Transacciones)")
    void testFinanzasIngresoVenta() {
        Ventas venta = new Ventas();
        venta.setNumeroFactura("FAC-TEST-" + System.currentTimeMillis());
        venta.setFechaVenta(LocalDateTime.now());
        venta.setSubtotal(new BigDecimal("50000.00"));
        venta.setTotal(new BigDecimal("50000.00"));
        venta.setMetodoPago("EFECTIVO");
        venta.setEstado("PAGADA");
        venta.setIdEmpleado(1L);
        venta = ventasRepository.save(venta);

        finanzasService.registrarIngresoVenta(venta, 1, Collections.emptyList());

        // Verificar que se creó la transacción financiera de ingreso
        Optional<TransaccionesFinancieras> opt = transaccionesFinancierasRepository.findByIdVenta(venta.getIdVenta());
        assertTrue(opt.isPresent(), "Debe existir un registro de transacción financiera para la venta");
        TransaccionesFinancieras tf = opt.get();
        assertEquals("INGRESO", tf.getTipoTransaccion());
        assertEquals("VENTA", tf.getConcepto());
        assertEquals(new BigDecimal("50000.00"), tf.getMonto());
        assertEquals("EFECTIVO", tf.getMetodoPago());
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("5. Prueba de Devolución con Reingreso al Kardex y Conversión de Formatos (Cajas a Unidades)")
    void testDevolucionesInventario() {
        // Producto existente en la base de datos con factor de empaque
        Productos prod = productosRepository.findById(1).orElseThrow();
        int stockInicial = prod.getStockTotal();
        int uCaja = (prod.getUnidadesPorCaja() != null && prod.getUnidadesPorCaja() > 0) ? prod.getUnidadesPorCaja() : 20;

        // 1. Devolución de 1 CAJA completa -> Debe calcular exactamente unidadesPorCaja pastillas
        int unidadesCaja = prod.calcularUnidadesBase("CAJA", 1);
        assertEquals(uCaja, unidadesCaja, "1 caja debe equivaler a sus unidadesPorCaja");

        // Reingreso al inventario de 1 CAJA
        inventarioService.registrarMovimiento(
                "DEVOLUCION_CLIENTE",
                prod.getIdProducto(),
                1,
                unidadesCaja,
                1,
                "Devolución de cliente: 1 CAJA (" + unidadesCaja + " pastillas)"
        );

        Productos prodTrasDevCaja = productosRepository.findById(prod.getIdProducto()).orElseThrow();
        assertEquals(stockInicial + unidadesCaja, prodTrasDevCaja.getStockTotal(), 
                "El stock debe subir exactamente en las pastillas que contiene la caja");
    }

    @Test
    @Order(6)
    @Transactional
    @DisplayName("6. Prueba de Recepción de Compras con Factor de Conversión de Cajas a Unidades")
    void testRecepcionComprasConversionCajas() {
        Categorias cat = categoriasRepository.findAll().stream().findFirst().orElseThrow();
        Proveedores prov = proveedoresRepository.findAll().stream().findFirst().orElseThrow();

        // Producto con 30 pastillas por caja
        Productos prodCajas = Productos.builder()
                .codigoBarras("COMPRA-" + System.currentTimeMillis())
                .nombreComercial("Antibiótico Cajas Test")
                .precioCompra(new BigDecimal("15000.00"))
                .precioVenta(new BigDecimal("22000.00"))
                .stockTotal(0)
                .stockMinimo(10)
                .unidadesPorCaja(30)
                .blisteresPorCaja(3)
                .registroInvima("INV-COMPRA-TEST")
                .estado(Productos.EstadoProducto.ACTIVO)
                .categoria(cat)
                .proveedor(prov)
                .requiereFormula(true)
                .build();
        prodCajas = productosRepository.save(prodCajas);

        // Se compran 5 CAJAS
        int cajasCompradas = 5;
        int unidadesEsperadas = cajasCompradas * prodCajas.getUnidadesPorCaja(); // 5 * 30 = 150 unidades base

        // Simular creación y recepción de lote según ComprasView
        Lotes loteCompra = Lotes.builder()
                .idProducto(prodCajas.getIdProducto())
                .numeroLote("LOT-COMPRA-" + System.currentTimeMillis())
                .fechaFabricacion(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusYears(2))
                .cantidadInicial(unidadesEsperadas)
                .cantidadActual(0)
                .estado("DISPONIBLE")
                .build();
        loteCompra = lotesRepository.save(loteCompra);

        // InventarioService registra la entrada en unidades base
        inventarioService.registrarMovimiento(
                "ENTRADA_COMPRA",
                prodCajas.getIdProducto(),
                loteCompra.getIdLote(),
                unidadesEsperadas,
                1,
                "Recepción de orden de compra (5 cajas = 150 unidades)"
        );

        // Verificar que el lote y el producto tienen 150 unidades base en stock
        Lotes loteRecargado = lotesRepository.findById(loteCompra.getIdLote()).orElseThrow();
        assertEquals(150, loteRecargado.getCantidadActual(), "El lote debe registrar 150 pastillas/unidades");

        Productos prodRecargado = productosRepository.findById(prodCajas.getIdProducto()).orElseThrow();
        assertEquals(150, prodRecargado.getStockTotal(), "El stock total del producto debe ser 150 unidades");
    }
}
