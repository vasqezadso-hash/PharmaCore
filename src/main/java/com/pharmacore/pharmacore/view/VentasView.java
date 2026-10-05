package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.DetallesVentas;
import com.pharmacore.pharmacore.model.Empleados;
import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.model.Usuarios;
import com.pharmacore.pharmacore.model.Ventas;
import com.pharmacore.pharmacore.repository.*;
import com.pharmacore.pharmacore.service.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Autowired
    private VentasRepository ventasRepository;

    @Autowired
    private DetallesVentasRepository detallesVentasRepository;

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
    private FormulasMedicasRepository formulasMedicasRepository;

    @Autowired
    private DetallesFormulasRepository detallesFormulasRepository;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private FacturaPdfService facturaPdfService;

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
        model.addAttribute("venta", venta);
        model.addAttribute("detalles", detallesVentasRepository.findByVenta_IdVenta(id));
        cargarNombres(model);
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
                           @RequestParam(required = false) List<Integer> idProductos,
                           @RequestParam(required = false) List<Integer> idLotes,
                           @RequestParam(required = false) List<Integer> cantidades,
                           @RequestParam(required = false) List<BigDecimal> preciosUnitarios,
                           @RequestParam(required = false) List<String> idDetallesFormula,
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

            List<Integer> productosValidos = new ArrayList<>();
            List<Integer> lotesValidos = new ArrayList<>();
            List<Integer> cantidadesValidas = new ArrayList<>();
            List<BigDecimal> preciosValidos = new ArrayList<>();
            List<Integer> detallesFormulaValidos = new ArrayList<>();

            if (idProductos != null) {
                for (int i = 0; i < idProductos.size(); i++) {
                    Integer idProducto = idProductos.get(i);
                    Integer idLote = idLotes != null && idLotes.size() > i ? idLotes.get(i) : null;
                    Integer cantidad = cantidades != null && cantidades.size() > i ? cantidades.get(i) : null;
                    BigDecimal precio = preciosUnitarios != null && preciosUnitarios.size() > i ? preciosUnitarios.get(i) : null;
                    String idDetalleFormulaStr = idDetallesFormula != null && idDetallesFormula.size() > i ? idDetallesFormula.get(i) : null;
                    Integer idDetalleFormula = (idDetalleFormulaStr != null && !idDetalleFormulaStr.isBlank())
                            ? Integer.valueOf(idDetalleFormulaStr) : null;

                    boolean lineaCompleta = idProducto != null && idLote != null
                            && cantidad != null && cantidad > 0
                            && precio != null;

                    if (lineaCompleta) {

                        com.pharmacore.pharmacore.model.Lotes lote = lotesRepository.findById(idLote).orElse(null);
                        if (lote == null) {
                            throw new IllegalStateException("El lote seleccionado ya no existe.");
                        }
                        if (lote.isVencido()) {
                            throw new IllegalStateException("El lote " + lote.getNumeroLote() + " está vencido y no se puede vender.");
                        }

                        Productos producto = productosRepository.findById(idProducto).orElse(null);
                        if (producto == null) {
                            throw new IllegalStateException("El producto seleccionado ya no existe.");
                        }
                        if (Boolean.TRUE.equals(producto.getRequiereFormula())) {
                            if (idDetalleFormula == null) {
                                throw new IllegalStateException(producto.getNombreComercial()
                                        + " requiere fórmula médica. Seleccione una fórmula vigente del paciente para esa línea.");
                            }
                            var detalleFormula = detallesFormulasRepository.findById(idDetalleFormula.longValue()).orElse(null);
                            if (detalleFormula == null || !detalleFormula.getIdProducto().equals(idProducto)) {
                                throw new IllegalStateException("La fórmula seleccionada no corresponde a " + producto.getNombreComercial() + ".");
                            }
                            var formula = formulasMedicasRepository.findById(detalleFormula.getIdFormula()).orElse(null);
                            if (formula == null || venta.getIdCliente() == null || !formula.getIdCliente().equals(venta.getIdCliente())) {
                                throw new IllegalStateException("La fórmula seleccionada no pertenece al cliente de esta venta.");
                            }
                            LocalDate vencimientoFormula = formula.getFechaExpedicion().plusDays(
                                    formula.getVigenciaDias() != null ? formula.getVigenciaDias() : 0);
                            if (vencimientoFormula.isBefore(LocalDate.now())) {
                                throw new IllegalStateException("La fórmula médica del paciente ya venció (expiró el "
                                        + vencimientoFormula + "). No se puede vender " + producto.getNombreComercial() + " sin una vigente.");
                            }
                        } else if (idDetalleFormula != null) {
                            // El producto no la requiere: se ignora cualquier fórmula que hayan mandado por error.
                            idDetalleFormula = null;
                        }

                        productosValidos.add(idProducto);
                        lotesValidos.add(idLote);
                        cantidadesValidas.add(cantidad);
                        preciosValidos.add(precio);
                        detallesFormulaValidos.add(idDetalleFormula);
                    }
                }
            }

            if (productosValidos.isEmpty()) {
                ra.addFlashAttribute("error", "Agregue al menos un medicamento con producto, lote, cantidad y precio completos antes de registrar la venta.");
                return "redirect:/view/ventas/nueva";
            }

            Usuarios usuario = usuariosRepository.findByIdEmpleado(venta.getIdEmpleado());
            if (usuario == null) {
                throw new IllegalStateException("El empleado seleccionado no tiene un usuario del sistema asociado; no se puede descontar el inventario.");
            }

            if (venta.getNumeroFactura() == null || venta.getNumeroFactura().isBlank()) {
                venta.setNumeroFactura("FARM-" + System.currentTimeMillis());
            }

            BigDecimal subtotalVenta = BigDecimal.ZERO;
            for (int i = 0; i < productosValidos.size(); i++) {
                subtotalVenta = subtotalVenta.add(preciosValidos.get(i).multiply(BigDecimal.valueOf(cantidadesValidas.get(i))));
            }
            BigDecimal descuento = venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO;
            BigDecimal iva = venta.getImpuestoIva() != null ? venta.getImpuestoIva() : BigDecimal.ZERO;

            venta.setSubtotal(subtotalVenta);
            venta.setTotal(subtotalVenta.subtract(descuento).add(iva));

            Ventas ventaGuardada = ventasRepository.save(venta);

            for (int i = 0; i < productosValidos.size(); i++) {
                DetallesVentas detalle = new DetallesVentas();
                detalle.setVenta(ventaGuardada);
                detalle.setIdProducto(productosValidos.get(i));
                detalle.setIdLote(lotesValidos.get(i));
                detalle.setCantidad(cantidadesValidas.get(i));
                detalle.setPrecioUnitario(preciosValidos.get(i));
                detalle.setSubtotal(preciosValidos.get(i).multiply(BigDecimal.valueOf(cantidadesValidas.get(i))));
                detalle.setIdDetalleFormula(detallesFormulaValidos.get(i));
                detallesVentasRepository.save(detalle);

                inventarioService.registrarMovimiento(
                        "SALIDA_VENTA",
                        productosValidos.get(i),
                        lotesValidos.get(i),
                        cantidadesValidas.get(i),
                        usuario.getIdUsuario().intValue(),
                        "Venta " + ventaGuardada.getNumeroFactura()
                );
            }

            ra.addFlashAttribute("mensaje", "Venta registrada correctamente: " + ventaGuardada.getNumeroFactura());
            return "redirect:/view/ventas/nueva?exito=true";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Ocurrió un error al registrar la venta: " + e.getMessage());
            return "redirect:/view/ventas/nueva";
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
            for (DetallesVentas d : detalles) {
                inventarioService.registrarMovimiento(
                        "DEVOLUCION_CLIENTE",
                        d.getIdProducto(),
                        d.getIdLote(),
                        d.getCantidad(),
                        usuario.getIdUsuario().intValue(),
                        "Anulación de venta " + venta.getNumeroFactura()
                );
            }

            venta.setEstado("ANULADA");
            ventasRepository.save(venta);
            ra.addFlashAttribute("mensaje", "Venta anulada: el inventario fue devuelto a los lotes originales.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo anular la venta: " + e.getMessage());
        }
        return "redirect:/view/ventas/detalle/" + id;
    }

    private void cargarListasApoyo(Model model) {
        model.addAttribute("clientes", clientesRepository.findAll());
        model.addAttribute("empleados", empleadosRepository.findAll());
        model.addAttribute("productos", productosRepository.findAll());

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

        LocalDate hoy = LocalDate.now();
        List<Map<String, Object>> formulasVigentesParaJs = formulasMedicasRepository.findAll().stream()
                .filter(f -> f.getFechaExpedicion() != null
                        && !f.getFechaExpedicion().plusDays(f.getVigenciaDias() != null ? f.getVigenciaDias() : 0).isBefore(hoy))
                .flatMap(f -> detallesFormulasRepository.findByIdFormula(f.getIdFormula()).stream()
                        .map(d -> Map.<String, Object>of(
                                "idDetalleFormula", d.getIdDetalleFormula(),
                                "idCliente", f.getIdCliente(),
                                "idProducto", d.getIdProducto(),
                                "dosis", d.getDosis(),
                                "frecuencia", d.getFrecuencia(),
                                "medico", f.getNombreMedico()
                        )))
                .collect(Collectors.toList());
        model.addAttribute("formulasVigentes", formulasVigentesParaJs);
    }
}
