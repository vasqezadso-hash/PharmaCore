package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Devoluciones;
import com.pharmacore.pharmacore.model.DetallesCompras;
import com.pharmacore.pharmacore.model.DetallesVentas;
import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.model.Usuarios;
import com.pharmacore.pharmacore.model.Ventas;
import com.pharmacore.pharmacore.repository.*;
import com.pharmacore.pharmacore.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
public class DevolucionesView {

    @Autowired
    private DevolucionesRepository devolucionesRepository;

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private VentasRepository ventasRepository;

    @Autowired
    private ComprasRepository comprasRepository;

    @Autowired
    private LotesRepository lotesRepository;

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private DetallesVentasRepository detallesVentasRepository;

    @Autowired
    private DetallesComprasRepository detallesComprasRepository;

    @Autowired
    private com.pharmacore.pharmacore.service.FinanzasService finanzasService;

    // LISTA
    @GetMapping("/view/devoluciones")
    public String lista(Model model) {
        model.addAttribute("devoluciones", devolucionesRepository.findAllByOrderByFechaDevolucionDesc());
        cargarNombres(model);
        return "devoluciones/devoluciones";
    }

    // FORMULARIO
    @GetMapping("/view/devoluciones/form")
    public String form(@RequestParam(required = false) Long idVenta, Model model) {
        Devoluciones devolucion = new Devoluciones();
        if (idVenta != null) {
            devolucion.setTipoDevolucion("CLIENTE");
            devolucion.setIdVenta(idVenta);
        }
        model.addAttribute("devolucion", devolucion);
        cargarListasApoyo(model);
        return "devoluciones/devolucionesForm";
    }

    // GUARDAR (Soporta una o múltiples líneas de devolución simultáneas)
    @Transactional
    @PostMapping("/view/devoluciones/guardar")
    public String guardar(@ModelAttribute("devolucion") Devoluciones devolucion,
                           BindingResult bindingResult,
                           @RequestParam(required = false) List<Integer> idProductos,
                           @RequestParam(required = false) List<String> formatos,
                           @RequestParam(required = false) List<Integer> cantidades,
                           @RequestParam(required = false) List<Integer> idLotes,
                           @RequestParam(required = false) List<String> motivos,
                           @RequestParam(required = false) List<String> estadosProductos,
                           @RequestParam(required = false) Integer idLote,
                           Authentication authentication,
                           RedirectAttributes ra) {

        // Control de permisos para devoluciones a proveedor
        boolean puedeGestionarProveedores = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR") || a.getAuthority().equals("ROLE_FARMACEUTICO"));
        if (!puedeGestionarProveedores && "PROVEEDOR".equals(devolucion.getTipoDevolucion())) {
            ra.addFlashAttribute("error", "Solo el Farmacéutico o el Administrador pueden registrar devoluciones a proveedor.");
            return "redirect:/view/devoluciones/form";
        }

        try {
            Usuarios usuario = usuariosRepository.findByUsernameIgnoreCase(authentication.getName());
            if (usuario == null) {
                throw new IllegalStateException("No se pudo identificar al usuario que registra la devolución.");
            }

            // Normalizar listas: si vino como formulario de una sola fila clásico, convertirlo a lista
            List<Integer> prodsFinales = new ArrayList<>();
            List<String> formatosFinales = new ArrayList<>();
            List<Integer> cantsFinales = new ArrayList<>();
            List<Integer> lotesFinales = new ArrayList<>();
            List<String> motivosFinales = new ArrayList<>();
            List<String> estadosFinales = new ArrayList<>();

            if (idProductos != null && !idProductos.isEmpty()) {
                prodsFinales = idProductos;
                formatosFinales = formatos != null ? formatos : new ArrayList<>();
                cantsFinales = cantidades != null ? cantidades : new ArrayList<>();
                lotesFinales = idLotes != null ? idLotes : new ArrayList<>();
                motivosFinales = motivos != null ? motivos : new ArrayList<>();
                estadosFinales = estadosProductos != null ? estadosProductos : new ArrayList<>();
            } else if (devolucion.getIdProducto() != null) {
                prodsFinales.add(devolucion.getIdProducto());
                formatosFinales.add(devolucion.getFormato() != null ? devolucion.getFormato() : "UNIDAD");
                cantsFinales.add(devolucion.getCantidad() != null ? devolucion.getCantidad() : 1);
                lotesFinales.add(idLote);
                motivosFinales.add(devolucion.getMotivo());
                estadosFinales.add(devolucion.getEstadoProducto());
            }

            if (prodsFinales.isEmpty()) {
                ra.addFlashAttribute("error", "Debe agregar al menos una línea de producto a devolver.");
                return "redirect:/view/devoluciones/form";
            }

            // Validar venta asociada si aplica
            Ventas ventaAsoc = null;
            if (devolucion.getIdVenta() != null) {
                ventaAsoc = ventasRepository.findById(devolucion.getIdVenta()).orElse(null);
                if (ventaAsoc != null) {
                    if ("ANULADA".equals(ventaAsoc.getEstado()) || "DEVUELTA".equals(ventaAsoc.getEstado())) {
                        ra.addFlashAttribute("error", "No se pueden realizar devoluciones sobre una venta en estado " + ventaAsoc.getEstado() + ".");
                        return "redirect:/view/devoluciones/form";
                    }
                }
            }

            int totalLineasProcesadas = 0;
            int totalUnidadesBaseReintegradas = 0;
            java.math.BigDecimal totalMontoReembolsado = java.math.BigDecimal.ZERO;
            Map<Integer, Integer> unidadesBaseEnBatchPorProducto = new HashMap<>();

            for (int i = 0; i < prodsFinales.size(); i++) {
                Integer idProd = prodsFinales.get(i);
                if (idProd == null) continue;

                String fmt = (formatosFinales.size() > i && formatosFinales.get(i) != null && !formatosFinales.get(i).isBlank())
                        ? formatosFinales.get(i).trim().toUpperCase() : "CAJA";
                Integer cant = (cantsFinales.size() > i && cantsFinales.get(i) != null) ? cantsFinales.get(i) : 1;
                if (cant <= 0) continue;

                Integer loteId = (lotesFinales.size() > i && lotesFinales.get(i) != null && lotesFinales.get(i) > 0)
                        ? lotesFinales.get(i) : null;
                String mot = (motivosFinales.size() > i && motivosFinales.get(i) != null && !motivosFinales.get(i).isBlank())
                        ? motivosFinales.get(i) : (devolucion.getMotivo() != null ? devolucion.getMotivo() : "PRODUCTO_DEFECTUOSO");
                String est = (estadosFinales.size() > i && estadosFinales.get(i) != null && !estadosFinales.get(i).isBlank())
                        ? estadosFinales.get(i) : (devolucion.getEstadoProducto() != null ? devolucion.getEstadoProducto() : "APTO_PARA_REINGRESO");

                Productos producto = productosRepository.findById(idProd).orElse(null);
                if (producto == null) continue;

                int factorFormato = producto.calcularUnidadesBase(fmt, 1);
                if (factorFormato <= 0) factorFormato = 1;
                int unidadesBaseDevueltas = cant * factorFormato;

                // Validación de cantidad contra la venta asociada
                if (ventaAsoc != null) {
                    List<DetallesVentas> lineasVenta = detallesVentasRepository.findByVenta_IdVenta(ventaAsoc.getIdVenta());
                    int unidadesBaseVendidas = lineasVenta.stream()
                            .filter(l -> l.getIdProducto().equals(idProd))
                            .mapToInt(l -> producto.calcularUnidadesBase(l.getFormato(), l.getCantidad()))
                            .sum();

                    if (unidadesBaseVendidas > 0) {
                        int unidadesBaseYaDevueltas = devolucionesRepository.findByIdVentaAndIdProducto(ventaAsoc.getIdVenta(), idProd)
                                .stream()
                                .mapToInt(d -> producto.calcularUnidadesBase(d.getFormato(), d.getCantidad()))
                                .sum();
                        int previasEnEsteBatch = unidadesBaseEnBatchPorProducto.getOrDefault(idProd, 0);
                        int unidadesBaseDisponibles = unidadesBaseVendidas - (unidadesBaseYaDevueltas + previasEnEsteBatch);
                        if (unidadesBaseDevueltas > unidadesBaseDisponibles) {
                            int maxEnFormato = factorFormato > 0 ? (unidadesBaseDisponibles / factorFormato) : 0;
                            throw new IllegalStateException("Para " + producto.getNombreComercial() + ": la cantidad a devolver (" + 
                                    cant + " " + fmt + " = " + unidadesBaseDevueltas + " u.) supera lo disponible de la venta (" + 
                                    unidadesBaseDisponibles + " u. restantes, máximo " + maxEnFormato + " " + fmt + ").");
                        }
                    }
                }

                unidadesBaseEnBatchPorProducto.put(idProd, unidadesBaseEnBatchPorProducto.getOrDefault(idProd, 0) + unidadesBaseDevueltas);

                // Guardar registro individual de la devolución
                Devoluciones devFila = Devoluciones.builder()
                        .tipoDevolucion(devolucion.getTipoDevolucion())
                        .idVenta(devolucion.getIdVenta())
                        .idCompra(devolucion.getIdCompra())
                        .idProducto(idProd)
                        .formato(fmt)
                        .cantidad(cant)
                        .motivo(mot)
                        .estadoProducto(est)
                        .fechaDevolucion(LocalDateTime.now())
                        .idUsuarioRegente(usuario.getIdUsuario())
                        .observaciones(devolucion.getObservaciones())
                        .build();
                devFila = devolucionesRepository.save(devFila);

                // Movimiento de inventario si afecta
                boolean afectaInventario = "PROVEEDOR".equals(devolucion.getTipoDevolucion())
                        || ("CLIENTE".equals(devolucion.getTipoDevolucion()) && "APTO_PARA_REINGRESO".equals(est));

                com.pharmacore.pharmacore.model.MovimientosInventario movRegistrado = null;
                if (afectaInventario) {
                    String tipoMovimiento = "PROVEEDOR".equals(devolucion.getTipoDevolucion())
                            ? "DEVOLUCION_PROVEEDOR" : "DEVOLUCION_CLIENTE";
                    movRegistrado = inventarioService.registrarMovimiento(
                            tipoMovimiento,
                            idProd,
                            loteId,
                            unidadesBaseDevueltas,
                            usuario.getIdUsuario().intValue(),
                            "Devolución " + devolucion.getTipoDevolucion() + " - " + mot + " (" + cant + " " + fmt + ")"
                    );
                    totalUnidadesBaseReintegradas += unidadesBaseDevueltas;
                }

                // Reembolso financiero si es devolución de cliente ligada a factura
                if ("CLIENTE".equals(devolucion.getTipoDevolucion()) && ventaAsoc != null) {
                    var detOpt = detallesVentasRepository.findByVenta_IdVenta(ventaAsoc.getIdVenta()).stream()
                            .filter(d -> d.getIdProducto().equals(idProd))
                            .findFirst();
                    if (detOpt.isPresent()) {
                        DetallesVentas det = detOpt.get();
                        java.math.BigDecimal precioVendido = det.getPrecioUnitario() != null ? det.getPrecioUnitario() : java.math.BigDecimal.ZERO;
                        int factorVenta = producto.calcularUnidadesBase(det.getFormato(), 1);
                        if (factorVenta <= 0) factorVenta = 1;

                        java.math.BigDecimal montoReembolso;
                        if (fmt.equalsIgnoreCase(det.getFormato())) {
                            montoReembolso = precioVendido.multiply(java.math.BigDecimal.valueOf(cant));
                        } else {
                            java.math.BigDecimal precioPorUnidadBase = precioVendido.divide(java.math.BigDecimal.valueOf(factorVenta), 4, java.math.RoundingMode.HALF_UP);
                            montoReembolso = precioPorUnidadBase.multiply(java.math.BigDecimal.valueOf(unidadesBaseDevueltas))
                                    .setScale(2, java.math.RoundingMode.HALF_UP);
                        }

                        if (montoReembolso.compareTo(java.math.BigDecimal.ZERO) > 0) {
                            finanzasService.registrarEgresoDevolucion(devFila, montoReembolso, ventaAsoc.getMetodoPago(), usuario.getIdUsuario().intValue(), movRegistrado);
                            totalMontoReembolsado = totalMontoReembolsado.add(montoReembolso);
                        }
                    }
                }
                totalLineasProcesadas++;
            }

            devolucionesRepository.flush();

            // Si se devolvió la totalidad de unidades base de la venta, cambiar estado a DEVUELTA
            if (ventaAsoc != null && "PAGADA".equals(ventaAsoc.getEstado())) {
                int totalBaseVendidas = detallesVentasRepository.findByVenta_IdVenta(ventaAsoc.getIdVenta()).stream()
                        .mapToInt(d -> {
                            Productos p = productosRepository.findById(d.getIdProducto()).orElse(null);
                            return p != null ? p.calcularUnidadesBase(d.getFormato(), d.getCantidad()) : d.getCantidad();
                        }).sum();
                int totalBaseDevueltas = devolucionesRepository.findByIdVenta(ventaAsoc.getIdVenta()).stream()
                        .mapToInt(dev -> {
                            Productos p = productosRepository.findById(dev.getIdProducto()).orElse(null);
                            return p != null ? p.calcularUnidadesBase(dev.getFormato(), dev.getCantidad()) : dev.getCantidad();
                        }).sum();
                if (totalBaseDevueltas >= totalBaseVendidas) {
                    ventaAsoc.setEstado("DEVUELTA");
                    ventasRepository.save(ventaAsoc);
                }
            }

            StringBuilder msg = new StringBuilder();
            msg.append("Se registraron ").append(totalLineasProcesadas).append(" producto(s) en la devolución con éxito.");
            if (totalUnidadesBaseReintegradas > 0) {
                msg.append(" Se reintegraron ").append(totalUnidadesBaseReintegradas).append(" pastillas base al inventario.");
            }
            if (totalMontoReembolsado.compareTo(java.math.BigDecimal.ZERO) > 0) {
                msg.append(" Reembolso total generado: $").append(String.format("%,.0f", totalMontoReembolsado.doubleValue())).append(".");
            }

            ra.addFlashAttribute("mensaje", msg.toString());
            return "redirect:/view/devoluciones";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo registrar la devolución: " + e.getMessage());
            return "redirect:/view/devoluciones/form";
        }
    }

    private void cargarListasApoyo(Model model) {
        model.addAttribute("productos", productosRepository.findAll());
        model.addAttribute("ventas", ventasRepository.findAll());
        model.addAttribute("compras", comprasRepository.findAll());
        model.addAttribute("tipos", Devoluciones.TIPOS);
        model.addAttribute("motivos", Devoluciones.MOTIVOS);
        model.addAttribute("estadosProducto", Devoluciones.ESTADOS_PRODUCTO);

        // Lotes por producto, para el <select> dependiente en el formulario (igual patrón que el POS).
        List<Map<String, Object>> lotesParaJs = lotesRepository.findAll().stream()
                .map(l -> Map.<String, Object>of(
                        "idLote", l.getIdLote(),
                        "idProducto", l.getIdProducto(),
                        "numeroLote", l.getNumeroLote()
                ))
                .collect(Collectors.toList());
        model.addAttribute("lotes", lotesParaJs);

        // Items de ventas para filtrado automático de producto/lote/formato en el formulario
        List<Devoluciones> todasDevoluciones = devolucionesRepository.findAll();
        List<Map<String, Object>> itemsVentasParaJs = detallesVentasRepository.findAll().stream()
                .map(d -> {
                    Productos p = productosRepository.findById(d.getIdProducto()).orElse(null);
                    String nombreProd = p != null ? p.getNombreComercial() : ("Producto #" + d.getIdProducto());
                    int uCaja = (p != null && p.getUnidadesPorCaja() != null) ? p.getUnidadesPorCaja() : 1;
                    int bCaja = (p != null && p.getBlisteresPorCaja() != null) ? p.getBlisteresPorCaja() : 1;
                    String fmt = d.getFormato() != null ? d.getFormato() : "UNIDAD";
                    int factorFmt = p != null ? p.calcularUnidadesBase(fmt, 1) : 1;
                    if (factorFmt <= 0) factorFmt = 1;

                    int unidadesVendidas = p != null ? p.calcularUnidadesBase(fmt, d.getCantidad()) : d.getCantidad();
                    int unidadesDevueltas = todasDevoluciones.stream()
                            .filter(dev -> dev.getIdVenta() != null && dev.getIdVenta().equals(d.getVenta().getIdVenta()) && dev.getIdProducto().equals(d.getIdProducto()))
                            .mapToInt(dev -> p != null ? p.calcularUnidadesBase(dev.getFormato(), dev.getCantidad()) : dev.getCantidad())
                            .sum();
                    int unidadesRestantes = Math.max(0, unidadesVendidas - unidadesDevueltas);
                    int cantRestante = factorFmt > 0 ? (unidadesRestantes / factorFmt) : 0;

                    Map<String, Object> map = new HashMap<>();
                    map.put("idVenta", d.getVenta().getIdVenta());
                    map.put("idProducto", d.getIdProducto());
                    map.put("nombreProducto", nombreProd);
                    map.put("idLote", d.getIdLote() != null ? d.getIdLote() : 0);
                    map.put("formato", fmt);
                    map.put("cantidad", d.getCantidad());
                    map.put("cantidadRestante", cantRestante);
                    map.put("unidadesRestantes", unidadesRestantes);
                    map.put("precioUnitario", d.getPrecioUnitario() != null ? d.getPrecioUnitario().doubleValue() : 0.0);
                    map.put("unidadesPorCaja", uCaja);
                    map.put("blisteresPorCaja", bCaja);
                    return map;
                })
                .collect(Collectors.toList());
        model.addAttribute("itemsVentas", itemsVentasParaJs);

        // Metadata de productos para conversiones en tiempo real
        List<Map<String, Object>> productosMetaParaJs = productosRepository.findAll().stream()
                .map(p -> Map.<String, Object>of(
                        "idProducto", p.getIdProducto(),
                        "nombre", p.getNombreComercial(),
                        "unidadesPorCaja", p.getUnidadesPorCaja() != null ? p.getUnidadesPorCaja() : 1,
                        "blisteresPorCaja", p.getBlisteresPorCaja() != null ? p.getBlisteresPorCaja() : 1,
                        "precioVenta", p.getPrecioVenta() != null ? p.getPrecioVenta().doubleValue() : 0.0
                ))
                .collect(Collectors.toList());
        model.addAttribute("productosMeta", productosMetaParaJs);
    }

    private void cargarNombres(Model model) {
        model.addAttribute("nombresProductos", productosRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getIdProducto(), p -> p.getNombreComercial())));
        model.addAttribute("nombresUsuarios", usuariosRepository.findAll().stream()
                .collect(Collectors.toMap(u -> u.getIdUsuario(), u -> u.getUsername())));
        model.addAttribute("numerosFactura", ventasRepository.findAll().stream()
                .collect(Collectors.toMap(v -> v.getIdVenta(), v -> v.getNumeroFactura())));
        model.addAttribute("facturasProveedor", comprasRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getIdCompra(), c -> c.getNumeroFacturaProveedor())));
    }
}
