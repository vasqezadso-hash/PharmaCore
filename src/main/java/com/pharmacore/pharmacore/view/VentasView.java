package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Clientes;
import com.pharmacore.pharmacore.model.DetallesVentas;
import com.pharmacore.pharmacore.model.Devoluciones;
import com.pharmacore.pharmacore.model.Domicilios;
import com.pharmacore.pharmacore.model.Empleados;
import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.model.Usuarios;
import com.pharmacore.pharmacore.model.Ventas;
import com.pharmacore.pharmacore.repository.*;
import com.pharmacore.pharmacore.service.InventarioService;
import com.pharmacore.pharmacore.service.FinanzasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.pharmacore.pharmacore.service.FacturaPdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
public class VentasView {

    private static final Logger log = LoggerFactory.getLogger(VentasView.class);

    @Autowired
    private VentasRepository ventasRepository;

    @Autowired
    private DetallesVentasRepository detallesVentasRepository;

    @Autowired
    private DevolucionesRepository devolucionesRepository;

    @Autowired
    private ClientesRepository clientesRepository;

    @Autowired
    private EmpleadosRepository empleadosRepository;

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private LotesRepository lotesRepository;

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private FacturaPdfService facturaPdfService;

    @Autowired
    private DomiciliosRepository domiciliosRepository;

    @Autowired
    private FinanzasService finanzasService;

    @GetMapping("/view/ventas/nueva")
    public String nuevaVenta(Model model, Authentication authentication) {
        // El empleado que vende ya no se elige de una lista: se toma del usuario que
        // inició sesión, para que una venta siempre quede registrada a nombre de quien
        // la hizo de verdad (antes cualquiera podía facturar "como" otro empleado
        // eligiéndolo del combo). Acá solo lo busco para mostrar su nombre en pantalla;
        // el valor que realmente se guarda se vuelve a resolver en guardar(), por las
        // dudas, en vez de confiar en lo que venga del formulario.
        Usuarios usuarioActual = usuariosRepository.findByUsernameIgnoreCase(authentication.getName());
        if (usuarioActual == null || usuarioActual.getIdEmpleado() == null) {
            throw new IllegalStateException("Tu usuario no tiene un empleado asociado; no puedes registrar ventas.");
        }
        Empleados empleadoActual = empleadosRepository.findById(usuarioActual.getIdEmpleado())
                .orElseThrow(() -> new IllegalStateException("No se encontró el empleado asociado a tu usuario."));

        Ventas venta = new Ventas();
        venta.setIdEmpleado(empleadoActual.getId_empleado());
        model.addAttribute("venta", venta);
        model.addAttribute("nombreEmpleadoActual", empleadoActual.getNombre_completo());
        cargarListasApoyo(model);
        return "ventas/pos";
    }

    @GetMapping("/view/dispensacion/historial")
    public String historial(Model model) {
        model.addAttribute("ventas", ventasRepository.findAll());
        cargarNombres(model);
        return "ventas/historial";
    }

    @GetMapping("/view/ventas/detalle/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Ventas venta = ventasRepository.findById(id).orElse(null);
        if (venta == null) {
            return "redirect:/view/dispensacion/historial";
        }
        List<DetallesVentas> detalles = detallesVentasRepository.findByVenta_IdVenta(id);
        List<Devoluciones> devolucionesVenta = devolucionesRepository.findByIdVenta(id);
        Domicilios domicilio = domiciliosRepository.findByIdVenta(id).orElse(null);

        Map<Integer, Integer> devueltasPorProducto = devolucionesVenta.stream()
                .collect(Collectors.groupingBy(Devoluciones::getIdProducto, Collectors.summingInt(Devoluciones::getCantidad)));

        int totalUnidadesVendidas = detalles.stream().mapToInt(DetallesVentas::getCantidad).sum();
        int totalUnidadesDevueltas = devolucionesVenta.stream().mapToInt(Devoluciones::getCantidad).sum();

        model.addAttribute("venta", venta);
        model.addAttribute("detalles", detalles);
        model.addAttribute("devolucionesVenta", devolucionesVenta);
        model.addAttribute("devueltasPorProducto", devueltasPorProducto);
        model.addAttribute("totalUnidadesVendidas", totalUnidadesVendidas);
        model.addAttribute("totalUnidadesDevueltas", totalUnidadesDevueltas);
        model.addAttribute("motivosDevolucion", Devoluciones.MOTIVOS);
        model.addAttribute("estadosProductoDevolucion", Devoluciones.ESTADOS_PRODUCTO);
        model.addAttribute("domicilio", domicilio);

        cargarNombres(model);
        model.addAttribute("nombresUsuarios", usuariosRepository.findAll().stream()
                .collect(Collectors.toMap(Usuarios::getIdUsuario, Usuarios::getUsername)));
        return "ventas/ventaDetalle";
    }

    private void cargarNombres(Model model) {
        model.addAttribute("nombresClientes", clientesRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getId_cliente(), c -> c.getNombreCompleto())));
        model.addAttribute("nombresEmpleados", empleadosRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getId_empleado(), e -> e.getNombre_completo())));
        model.addAttribute("nombresProductos", productosRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getIdProducto(), p -> p.getNombreComercial())));
        model.addAttribute("nombresLotes", lotesRepository.findAll().stream()
                .collect(Collectors.toMap(l -> l.getIdLote(), l -> l.getNumeroLote())));
    }

    @Transactional
    @PostMapping("/view/ventas/nueva/guardar")
    public String guardar(@ModelAttribute Ventas venta,
                           @RequestParam(required = false) List<String> idProductos,
                           @RequestParam(required = false) List<String> formatos,
                           @RequestParam(required = false) List<String> idLotes,
                           @RequestParam(required = false) List<String> cantidades,
                           @RequestParam(required = false) List<String> preciosUnitarios,
                           @RequestParam(required = false, defaultValue = "false") boolean esDomicilio,
                           @RequestParam(required = false) String direccionDomicilio,
                           @RequestParam(required = false) String telefonoDomicilio,
                           @RequestParam(required = false) String nombreDestinatarioDomicilio,
                           @RequestParam(required = false) String documentoClienteDomicilio,
                           @RequestParam(required = false) String nombreDomiciliario,
                           @RequestParam(required = false) BigDecimal costoDomicilio,
                           @RequestParam(required = false) String observacionesDomicilio,
                           Authentication authentication,
                           RedirectAttributes ra) {
        try {

            // No confío en el idEmpleado que venga del formulario (ya ni se manda, pero
            // aunque alguien lo agregara a mano con las herramientas del navegador, acá
            // lo vuelvo a pisar con el empleado real de la sesión). Así, pase lo que pase
            // en el HTML, la venta siempre queda a nombre de quien efectivamente la hizo.
            Usuarios usuarioActual = usuariosRepository.findByUsernameIgnoreCase(authentication.getName());
            if (usuarioActual == null || usuarioActual.getIdEmpleado() == null) {
                throw new IllegalStateException("Tu usuario no tiene un empleado asociado; no puedes registrar ventas.");
            }
            venta.setIdEmpleado(usuarioActual.getIdEmpleado());

            Long idClienteDomicilioFinal = null;
            if (esDomicilio) {
                if (direccionDomicilio == null || direccionDomicilio.trim().isBlank()
                        || telefonoDomicilio == null || telefonoDomicilio.trim().isBlank()) {
                    throw new IllegalStateException("Para ventas con entrega a domicilio, la dirección y el teléfono de contacto son obligatorios.");
                }

                if (venta.getIdCliente() != null) {
                    idClienteDomicilioFinal = venta.getIdCliente();
                    Clientes clienteExistente = clientesRepository.findById(idClienteDomicilioFinal).orElse(null);
                    if (clienteExistente != null) {
                        boolean actualizar = false;
                        if ((clienteExistente.getDireccion() == null || clienteExistente.getDireccion().isBlank())
                                && !direccionDomicilio.trim().isBlank()) {
                            clienteExistente.setDireccion(direccionDomicilio.trim());
                            actualizar = true;
                        }
                        if ((clienteExistente.getTelefono() == null || clienteExistente.getTelefono().isBlank())
                                && !telefonoDomicilio.trim().isBlank()) {
                            clienteExistente.setTelefono(telefonoDomicilio.trim());
                            actualizar = true;
                        }
                        if (actualizar) {
                            clientesRepository.save(clienteExistente);
                        }
                    }
                } else {
                    // Cliente no registrado: buscar por documento si fue ingresado, o crear un cliente de forma ágil
                    Clientes clienteParaDomicilio = null;
                    if (documentoClienteDomicilio != null && !documentoClienteDomicilio.trim().isBlank()) {
                        clienteParaDomicilio = clientesRepository.findByNumeroDocumento(documentoClienteDomicilio.trim()).orElse(null);
                    }
                    if (clienteParaDomicilio == null) {
                        String numDoc = (documentoClienteDomicilio != null && !documentoClienteDomicilio.trim().isBlank())
                                ? documentoClienteDomicilio.trim()
                                : ("DOM-" + (System.currentTimeMillis() % 100000000));
                        String nomCli = (nombreDestinatarioDomicilio != null && !nombreDestinatarioDomicilio.trim().isBlank())
                                ? nombreDestinatarioDomicilio.trim()
                                : "Cliente Domicilio";

                        clienteParaDomicilio = Clientes.builder()
                                .nombreCompleto(nomCli)
                                .tipoDocumento("CC")
                                .numeroDocumento(numDoc)
                                .telefono(telefonoDomicilio.trim())
                                .direccion(direccionDomicilio.trim())
                                .build();
                        clienteParaDomicilio = clientesRepository.save(clienteParaDomicilio);
                    }
                    idClienteDomicilioFinal = clienteParaDomicilio.getId_cliente();
                    venta.setIdCliente(idClienteDomicilioFinal);
                }
            }

            List<AsignacionVentaLote> asignaciones = new ArrayList<>();
            Map<Integer, Integer> stockVirtualLotes = new HashMap<>();

            if (idProductos != null) {
                for (int i = 0; i < idProductos.size(); i++) {
                    Integer idProducto = parsearInteger(idProductos.get(i));
                    String formato = (formatos != null && formatos.size() > i && formatos.get(i) != null && !formatos.get(i).isBlank())
                            ? formatos.get(i).trim().toUpperCase() : "CAJA";
                    Integer cantidad = parsearInteger(cantidades != null && cantidades.size() > i ? cantidades.get(i) : null);
                    BigDecimal precio = parsearBigDecimal(preciosUnitarios != null && preciosUnitarios.size() > i ? preciosUnitarios.get(i) : null);

                    if (idProducto == null || cantidad == null || cantidad <= 0 || precio == null) {
                        continue;
                    }

                    Productos producto = productosRepository.findById(idProducto).orElse(null);
                    if (producto == null) {
                        throw new IllegalStateException("El producto seleccionado ya no existe.");
                    }

                    // FACTOR DE CONVERSIÓN POR FORMATO (CAJA, BLÍSTER O UNIDAD)
                    int factorUnidadesBase = producto.calcularUnidadesBase(formato, 1);
                    int totalUnidadesBaseADescontar = producto.calcularUnidadesBase(formato, cantidad);

                    // ASIGNACIÓN AUTOMÁTICA DE LOTES (PEFO / FEFO: Primero en Vencer, Primero en Salir)
                    // 1. Obtener lotes del producto ordenados por fecha de vencimiento ascendente
                    List<com.pharmacore.pharmacore.model.Lotes> lotesProducto = lotesRepository
                            .findByIdProductoOrderByFechaVencimientoAsc(idProducto);

                    // 2. Filtrar solo los lotes aptos: no retirados, no devueltos, no vencidos y con stock disponible
                    List<com.pharmacore.pharmacore.model.Lotes> lotesAptos = lotesProducto.stream()
                            .filter(l -> !"RETIRADO".equals(l.getEstado()) && !"DEVUELTO".equals(l.getEstado()))
                            .filter(l -> !l.isVencido())
                            .filter(l -> {
                                int disponible = stockVirtualLotes.computeIfAbsent(
                                        l.getIdLote(),
                                        k -> (l.getCantidadActual() != null ? l.getCantidadActual() : 0)
                                );
                                return disponible > 0;
                            })
                            .collect(Collectors.toList());

                    int stockTotalLotes = lotesAptos.stream()
                            .mapToInt(l -> stockVirtualLotes.get(l.getIdLote()))
                            .sum();

                    if (stockTotalLotes < totalUnidadesBaseADescontar) {
                        String nombreFormato = "caja(s)";
                        if ("BLISTER".equalsIgnoreCase(formato)) nombreFormato = "blíster(es)";
                        else if ("UNIDAD".equalsIgnoreCase(formato)) nombreFormato = "unidad(es)";

                        if (stockTotalLotes == 0) {
                            throw new IllegalStateException("No hay existencias en lotes vigentes para '"
                                    + producto.getNombreComercial()
                                    + "'. Verifique que el producto cuente con remesas/lotes activos y no vencidos.");
                        } else {
                            throw new IllegalStateException("Stock insuficiente en lotes vigentes para '"
                                    + producto.getNombreComercial()
                                    + "'. Solicitado: " + cantidad + " " + nombreFormato + " (" + totalUnidadesBaseADescontar + " unidades base)"
                                    + ", disponible en lotes activos: " + stockTotalLotes + " unidades.");
                        }
                    }

                    // 3. Descontar en cascada del lote más próximo a vencer al más lejano
                    int pendienteBase = totalUnidadesBaseADescontar;
                    int restanteComercial = cantidad;
                    for (com.pharmacore.pharmacore.model.Lotes lote : lotesAptos) {
                        if (pendienteBase <= 0) break;
                        int disponibleEnLote = stockVirtualLotes.get(lote.getIdLote());
                        if (disponibleEnLote <= 0) continue;

                        int baseADescontar = Math.min(disponibleEnLote, pendienteBase);
                        stockVirtualLotes.put(lote.getIdLote(), disponibleEnLote - baseADescontar);
                        pendienteBase -= baseADescontar;

                        int cantComercial;
                        if (pendienteBase == 0) {
                            cantComercial = Math.max(1, restanteComercial);
                        } else {
                            cantComercial = Math.max(1, baseADescontar / factorUnidadesBase);
                            restanteComercial = Math.max(0, restanteComercial - cantComercial);
                        }

                        asignaciones.add(new AsignacionVentaLote(
                                idProducto,
                                lote.getIdLote(),
                                formato,
                                cantComercial,
                                baseADescontar,
                                precio
                        ));
                    }
                }
            }

            if (asignaciones.isEmpty()) {
                ra.addFlashAttribute("error", "Agregue al menos un medicamento con producto, cantidad y precio completos antes de registrar la venta.");
                return "redirect:/view/ventas/nueva";
            }

            Usuarios usuario = usuarioActual;

            if (venta.getNumeroFactura() == null || venta.getNumeroFactura().isBlank()) {
                venta.setNumeroFactura("FARM-" + System.currentTimeMillis());
            }

            if (venta.getFechaVenta() == null) {
                venta.setFechaVenta(LocalDateTime.now());
            }

            BigDecimal subtotalVenta = BigDecimal.ZERO;
            for (AsignacionVentaLote asig : asignaciones) {
                subtotalVenta = subtotalVenta.add(asig.precioUnitario.multiply(BigDecimal.valueOf(asig.cantidadVendida)));
            }
            BigDecimal descuento = venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO;
            BigDecimal iva = venta.getImpuestoIva() != null ? venta.getImpuestoIva() : BigDecimal.ZERO;

            venta.setSubtotal(subtotalVenta);
            venta.setTotal(subtotalVenta.subtract(descuento).add(iva));

            Ventas ventaGuardada = ventasRepository.save(venta);

            List<com.pharmacore.pharmacore.model.MovimientosInventario> movimientosVenta = new ArrayList<>();
            for (AsignacionVentaLote asig : asignaciones) {
                DetallesVentas detalle = new DetallesVentas();
                detalle.setVenta(ventaGuardada);
                detalle.setIdProducto(asig.idProducto);
                detalle.setIdLote(asig.idLote);
                detalle.setFormato(asig.formato);
                detalle.setCantidad(asig.cantidadVendida);
                detalle.setPrecioUnitario(asig.precioUnitario);
                detalle.setSubtotal(asig.precioUnitario.multiply(BigDecimal.valueOf(asig.cantidadVendida)));
                detallesVentasRepository.save(detalle);

                com.pharmacore.pharmacore.model.MovimientosInventario mov = inventarioService.registrarMovimiento(
                        "SALIDA_VENTA",
                        asig.idProducto,
                        asig.idLote,
                        asig.unidadesBaseDescontar,
                        usuario.getIdUsuario().intValue(),
                        "Venta " + ventaGuardada.getNumeroFactura() + " (" + asig.cantidadVendida + " " + asig.formato + ")"
                );
                if (mov != null) {
                    movimientosVenta.add(mov);
                }
            }

            // Registrar entrada de dinero por la venta y enlazar la trazabilidad con los movimientos de inventario
            finanzasService.registrarIngresoVenta(ventaGuardada, usuario.getIdUsuario().intValue(), movimientosVenta);

            if (esDomicilio && idClienteDomicilioFinal != null) {
                String dirFinal = direccionDomicilio.trim();
                if (observacionesDomicilio != null && !observacionesDomicilio.trim().isBlank()) {
                    String notas = " (Nota: " + observacionesDomicilio.trim() + ")";
                    if (dirFinal.length() + notas.length() <= 200) {
                        dirFinal += notas;
                    }
                }
                Domicilios dom = Domicilios.builder()
                        .idVenta(ventaGuardada.getIdVenta())
                        .idCliente(idClienteDomicilioFinal)
                        .direccionEntrega(dirFinal)
                        .telefonoContacto(telefonoDomicilio.trim())
                        .nombreDomiciliario(nombreDomiciliario != null && !nombreDomiciliario.trim().isBlank() ? nombreDomiciliario.trim() : null)
                        .costoDomicilio(costoDomicilio != null ? costoDomicilio : BigDecimal.ZERO)
                        .estado("PENDIENTE")
                        .build();
                domiciliosRepository.save(dom);
                ra.addFlashAttribute("mensaje", "Venta registrada con orden de domicilio: " + ventaGuardada.getNumeroFactura() + " (Destino: " + dirFinal + ")");
            } else {
                ra.addFlashAttribute("mensaje", "Venta registrada correctamente: " + ventaGuardada.getNumeroFactura());
            }
            return "redirect:/view/ventas/nueva?exito=true";
        } catch (Exception e) {
            log.error("Error al registrar venta: {}", e.getMessage(), e);
            ra.addFlashAttribute("error", "Ocurrió un error al registrar la venta: " + e.getMessage());
            return "redirect:/view/ventas/nueva";
        }
    }

    private Integer parsearInteger(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Integer.valueOf(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parsearBigDecimal(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return new BigDecimal(val.trim().replace(",", "."));
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/view/ventas/detalle/{id}/factura-pdf")
    public ResponseEntity<byte[]> facturaPdf(@PathVariable Long id) {
        Ventas venta = ventasRepository.findById(id).orElse(null);
        if (venta == null) {
            return ResponseEntity.notFound().build();
        }
        List<DetallesVentas> detalles = detallesVentasRepository.findByVenta_IdVenta(id);

        Map<Integer, String> nombresProductos = productosRepository.findAll().stream()
                .collect(Collectors.toMap(Productos::getIdProducto, Productos::getNombreComercial));
        Map<Integer, String> nombresLotes = lotesRepository.findAll().stream()
                .collect(Collectors.toMap(com.pharmacore.pharmacore.model.Lotes::getIdLote,
                        com.pharmacore.pharmacore.model.Lotes::getNumeroLote));

        String nombreCliente = venta.getIdCliente() != null
                ? clientesRepository.findById(venta.getIdCliente()).map(c -> c.getNombreCompleto()).orElse(null)
                : null;
        String nombreEmpleado = empleadosRepository.findById(venta.getIdEmpleado())
                .map(e -> e.getNombre_completo()).orElse(null);

        try {
            byte[] pdf = facturaPdfService.generarFacturaPdf(venta, detalles, nombresProductos, nombresLotes, nombreCliente, nombreEmpleado);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename("factura-" + venta.getNumeroFactura() + ".pdf").build());
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    @Transactional
    @PostMapping("/view/ventas/detalle/{idVenta}/devolver-item")
    public String devolverItem(@PathVariable Long idVenta,
                               @RequestParam Long idDetalleVenta,
                               @RequestParam Integer cantidadADevolver,
                               @RequestParam String motivo,
                               @RequestParam String estadoProducto,
                               @RequestParam(required = false) String observaciones,
                               Authentication authentication,
                               RedirectAttributes ra) {
        Ventas venta = ventasRepository.findById(idVenta).orElse(null);
        if (venta == null) {
            return "redirect:/view/dispensacion/historial";
        }

        if ("ANULADA".equals(venta.getEstado()) || "DEVUELTA".equals(venta.getEstado())) {
            ra.addFlashAttribute("error", "No se pueden realizar devoluciones sobre una venta que ya esta en estado " + venta.getEstado() + ".");
            return "redirect:/view/ventas/detalle/" + idVenta;
        }

        DetallesVentas detalle = detallesVentasRepository.findById(idDetalleVenta).orElse(null);
        if (detalle == null || !detalle.getVenta().getIdVenta().equals(idVenta)) {
            ra.addFlashAttribute("error", "El producto seleccionado no pertenece a esta venta.");
            return "redirect:/view/ventas/detalle/" + idVenta;
        }

        if (cantidadADevolver == null || cantidadADevolver <= 0) {
            ra.addFlashAttribute("error", "La cantidad a devolver debe ser mayor a cero.");
            return "redirect:/view/ventas/detalle/" + idVenta;
        }

        // Calcular unidades ya devueltas de este producto en esta factura
        List<Devoluciones> devolucionesExistentes = devolucionesRepository.findByIdVentaAndIdProducto(idVenta, detalle.getIdProducto());
        int yaDevueltas = devolucionesExistentes.stream().mapToInt(Devoluciones::getCantidad).sum();
        int disponible = detalle.getCantidad() - yaDevueltas;

        if (cantidadADevolver > disponible) {
            ra.addFlashAttribute("error", "La cantidad a devolver (" + cantidadADevolver + ") supera la cantidad disponible de esta linea (" + disponible + ").");
            return "redirect:/view/ventas/detalle/" + idVenta;
        }

        try {
            Usuarios usuario = usuariosRepository.findByUsernameIgnoreCase(authentication.getName());
            if (usuario == null) {
                throw new IllegalStateException("No se pudo identificar al usuario que realiza la devolucion.");
            }

            // Registrar devolucion individual
            Devoluciones devolucion = Devoluciones.builder()
                    .tipoDevolucion("CLIENTE")
                    .idVenta(idVenta)
                    .idProducto(detalle.getIdProducto())
                    .cantidad(cantidadADevolver)
                    .motivo(motivo)
                    .estadoProducto(estadoProducto)
                    .fechaDevolucion(LocalDateTime.now())
                    .idUsuarioRegente(usuario.getIdUsuario())
                    .observaciones(observaciones)
                    .build();

            devolucionesRepository.saveAndFlush(devolucion);

            com.pharmacore.pharmacore.model.MovimientosInventario movReingreso = null;
            // Reingreso a inventario si el producto se recibe apto
            if ("APTO_PARA_REINGRESO".equals(estadoProducto)) {
                Productos prodDev = productosRepository.findById(detalle.getIdProducto()).orElse(null);
                int factorDev = prodDev != null ? prodDev.calcularUnidadesBase(detalle.getFormato(), 1) : 1;
                int unidadesBaseReingreso = cantidadADevolver * factorDev;

                movReingreso = inventarioService.registrarMovimiento(
                        "DEVOLUCION_CLIENTE",
                        detalle.getIdProducto(),
                        detalle.getIdLote(),
                        unidadesBaseReingreso,
                        usuario.getIdUsuario().intValue(),
                        "Devolucion parcial de venta " + venta.getNumeroFactura() + " (" + cantidadADevolver + " " + (detalle.getFormato() != null ? detalle.getFormato() : "UNIDAD") + ")"
                );
            }

            // Registrar egreso financiero por concepto de reembolso al cliente y vincular trazabilidad
            BigDecimal montoReembolso = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidadADevolver));
            finanzasService.registrarEgresoDevolucion(devolucion, montoReembolso, venta.getMetodoPago(), usuario.getIdUsuario().intValue(), movReingreso);

            // Si todas las unidades de toda la factura fueron devueltas, marcar venta como DEVUELTA
            List<DetallesVentas> todosDetalles = detallesVentasRepository.findByVenta_IdVenta(idVenta);
            int totalVendidas = todosDetalles.stream().mapToInt(DetallesVentas::getCantidad).sum();
            int totalDevueltas = devolucionesRepository.findByIdVenta(idVenta).stream().mapToInt(Devoluciones::getCantidad).sum();

            if (totalDevueltas >= totalVendidas) {
                venta.setEstado("DEVUELTA");
                ventasRepository.save(venta);
            }

            Productos p = productosRepository.findById(detalle.getIdProducto()).orElse(null);
            String nombreProd = p != null ? p.getNombreComercial() : "Producto #" + detalle.getIdProducto();

            ra.addFlashAttribute("mensaje", "Devolucion de " + cantidadADevolver + " unidad(es) de " + nombreProd + " registrada con exito."
                    + ("APTO_PARA_REINGRESO".equals(estadoProducto) ? " Las existencias fueron reintegradas al lote correspondiente." : " El producto no fue reintegrado al inventario comercial segun la disposicion indicada."));

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al procesar la devolucion: " + e.getMessage());
        }

        return "redirect:/view/ventas/detalle/" + idVenta;
    }

    @Transactional
    @PostMapping("/view/ventas/anular/{id}")
    public String anular(@PathVariable Long id, RedirectAttributes ra) {
        Ventas venta = ventasRepository.findById(id).orElse(null);
        if (venta == null) {
            return "redirect:/view/dispensacion/historial";
        }
        if (!"PAGADA".equals(venta.getEstado())) {
            ra.addFlashAttribute("error", "Solo se pueden anular ventas que estén en estado PAGADA.");
            return "redirect:/view/ventas/detalle/" + id;
        }

        try {
            Usuarios usuario = usuariosRepository.findByIdEmpleado(venta.getIdEmpleado());
            if (usuario == null) {
                throw new IllegalStateException("El empleado de la venta no tiene un usuario del sistema asociado.");
            }

            List<DetallesVentas> detalles = detallesVentasRepository.findByVenta_IdVenta(id);
            List<Devoluciones> devolucionesPrevias = devolucionesRepository.findByIdVenta(id);
            Map<Integer, Integer> yaDevueltasPorProducto = devolucionesPrevias.stream()
                    .filter(d -> "APTO_PARA_REINGRESO".equals(d.getEstadoProducto()))
                    .collect(Collectors.groupingBy(Devoluciones::getIdProducto, Collectors.summingInt(Devoluciones::getCantidad)));

            List<com.pharmacore.pharmacore.model.MovimientosInventario> movsAnulacion = new ArrayList<>();
            BigDecimal montoReembolsarAnulacion = BigDecimal.ZERO;
            for (DetallesVentas d : detalles) {
                int yaDevuelto = yaDevueltasPorProducto.getOrDefault(d.getIdProducto(), 0);
                int cantidadAReintegrar = Math.max(0, d.getCantidad() - yaDevuelto);
                if (cantidadAReintegrar > 0) {
                    Productos prodAnul = productosRepository.findById(d.getIdProducto()).orElse(null);
                    int factorAnul = prodAnul != null ? prodAnul.calcularUnidadesBase(d.getFormato(), 1) : 1;
                    int unidadesBaseAnulacion = cantidadAReintegrar * factorAnul;

                    com.pharmacore.pharmacore.model.MovimientosInventario mov = inventarioService.registrarMovimiento(
                            "DEVOLUCION_CLIENTE",
                            d.getIdProducto(),
                            d.getIdLote(),
                            unidadesBaseAnulacion,
                            usuario.getIdUsuario().intValue(),
                            "Anulación de venta " + venta.getNumeroFactura() + " (" + cantidadAReintegrar + " " + (d.getFormato() != null ? d.getFormato() : "UNIDAD") + ")"
                    );
                    if (mov != null) movsAnulacion.add(mov);
                    montoReembolsarAnulacion = montoReembolsarAnulacion.add(d.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidadAReintegrar)));
                }
            }

            if (montoReembolsarAnulacion.compareTo(BigDecimal.ZERO) > 0) {
                finanzasService.registrarEgresoAnulacionVenta(venta, montoReembolsarAnulacion, usuario.getIdUsuario().intValue(), movsAnulacion);
            }

            venta.setEstado("ANULADA");
            ventasRepository.save(venta);
            ra.addFlashAttribute("mensaje", "Venta anulada: el inventario restante fue devuelto a los lotes originales.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo anular la venta: " + e.getMessage());
        }
        return "redirect:/view/ventas/detalle/" + id;
    }

    private void cargarListasApoyo(Model model) {
        List<Clientes> listaClientes = clientesRepository.findAll();
        model.addAttribute("clientes", listaClientes);
        model.addAttribute("empleados", empleadosRepository.findAll());
        model.addAttribute("productos", productosRepository.findAll());

        // Diccionario de clientes para autocompletar dirección y contacto en el POS
        Map<Long, Map<String, String>> clientesJs = listaClientes.stream()
                .collect(Collectors.toMap(
                        Clientes::getId_cliente,
                        c -> Map.of(
                                "nombre", c.getNombreCompleto() != null ? c.getNombreCompleto() : "",
                                "documento", c.getNumeroDocumento() != null ? c.getNumeroDocumento() : "",
                                "direccion", c.getDireccion() != null ? c.getDireccion() : "",
                                "telefono", c.getTelefono() != null ? c.getTelefono() : ""
                        )
                ));
        model.addAttribute("clientesJs", clientesJs);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Map<String, Object>> lotesParaJs = lotesRepository.findAll().stream()
                .filter(l -> !l.isVencido())
                .filter(l -> l.getCantidadActual() != null && l.getCantidadActual() > 0)
                .map(l -> Map.<String, Object>of(
                        "idLote", l.getIdLote(),
                        "idProducto", l.getIdProducto(),
                        "numeroLote", l.getNumeroLote(),
                        "fechaVencimiento", l.getFechaVencimiento() != null ? l.getFechaVencimiento().format(fmt) : ""
                ))
                .collect(Collectors.toList());
        model.addAttribute("lotes", lotesParaJs);
    }

    private static class AsignacionVentaLote {
        final Integer idProducto;
        final Integer idLote;
        final String formato;
        final Integer cantidadVendida;
        final Integer unidadesBaseDescontar;
        final BigDecimal precioUnitario;

        AsignacionVentaLote(Integer idProducto, Integer idLote, String formato, Integer cantidadVendida,
                            Integer unidadesBaseDescontar, BigDecimal precioUnitario) {
            this.idProducto = idProducto;
            this.idLote = idLote;
            this.formato = formato;
            this.cantidadVendida = cantidadVendida;
            this.unidadesBaseDescontar = unidadesBaseDescontar;
            this.precioUnitario = precioUnitario;
        }
    }
}
