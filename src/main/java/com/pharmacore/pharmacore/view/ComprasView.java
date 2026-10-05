package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Compras;
import com.pharmacore.pharmacore.model.DetallesCompras;
import com.pharmacore.pharmacore.model.Lotes;
import com.pharmacore.pharmacore.repository.*;
import com.pharmacore.pharmacore.service.InventarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
public class ComprasView {

    @Autowired
    private ComprasRepository comprasRepository;

    @Autowired
    private DetallesComprasRepository detallesComprasRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

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

    // LISTA
    @GetMapping("/view/ordenes-compra")
    public String lista(Model model) {
        model.addAttribute("compras", comprasRepository.findAll());
        cargarNombres(model);
        return "compras/compras";
    }

    // FORMULARIO CREAR CABECERA
    @GetMapping("/view/ordenes-compra/form")
    public String form(Model model) {
        model.addAttribute("compra", new Compras());
        cargarListasApoyo(model);
        return "compras/comprasForm";
    }

    // FORMULARIO EDITAR CABECERA (solo datos generales; las líneas se editan aparte)
    @GetMapping("/view/ordenes-compra/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        Compras compra = comprasRepository.findById(id).orElse(null);
        if (compra == null) {
            return "redirect:/view/ordenes-compra";
        }
        model.addAttribute("compra", compra);
        cargarListasApoyo(model);
        return "compras/comprasForm";
    }

    // GUARDAR CABECERA (crear o actualizar) -> siempre termina en la pantalla de líneas
    @PostMapping("/view/ordenes-compra/save")
    public String save(@Valid @ModelAttribute("compra") Compras compra, BindingResult result,
                        Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("error", "Verifica los campos obligatorios.");
            cargarListasApoyo(model);
            return "compras/comprasForm";
        }
        if (compra.getIdCompra() == null) {
            // Compra nueva, arranca en ceros
            compra.setSubtotal(BigDecimal.ZERO);
            compra.setTotal(compra.getImpuestos() != null ? compra.getImpuestos() : BigDecimal.ZERO);
        } else {
            //
            Compras existente = comprasRepository.findById(compra.getIdCompra()).orElse(null);
            if (existente != null) {
                compra.setSubtotal(existente.getSubtotal());
                compra.setTotal(existente.getSubtotal().add(compra.getImpuestos() != null ? compra.getImpuestos() : BigDecimal.ZERO));
            }
        }
        Compras guardada = comprasRepository.save(compra);
        ra.addFlashAttribute("mensaje", "Orden de compra guardada. Ahora agrega los productos.");
        return "redirect:/view/ordenes-compra/detalle/" + guardada.getIdCompra();
    }

    // ELIMINAR CABECERA
    @PostMapping("/view/ordenes-compra/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        comprasRepository.deleteById(id);
        ra.addFlashAttribute("mensaje", "Orden de compra eliminada");
        return "redirect:/view/ordenes-compra";
    }

    // DETALLE: cabecera + líneas + formulario para agregar línea + accesos a Recibir/Cancelar
    @GetMapping("/view/ordenes-compra/detalle/{id}")
    public String detalle(@PathVariable Integer id, Model model) {
        Compras compra = comprasRepository.findById(id).orElse(null);
        if (compra == null) {
            return "redirect:/view/ordenes-compra";
        }
        model.addAttribute("compra", compra);
        model.addAttribute("detalles", detallesComprasRepository.findByCompra_IdCompra(id));
        model.addAttribute("productos", productosRepository.findAll());
        cargarNombres(model);
        return "compras/compraDetalle";
    }

    // Mapas "ID -> nombre" para no mostrar IDs crudos en las plantillas.
    private void cargarNombres(Model model) {
        model.addAttribute("nombresProveedores", proveedoresRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getId_proveedor(), p -> p.getRazon_social())));
        model.addAttribute("nombresEmpleados", empleadosRepository.findAll().stream()
                .collect(Collectors.toMap(e -> e.getId_empleado(), e -> e.getNombre_completo())));
        model.addAttribute("nombresProductos", productosRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getIdProducto(), p -> p.getNombreComercial())));
    }

    // AGREGAR LÍNEA (solo tiene sentido mientras la compra sigue PENDIENTE)
    @PostMapping("/view/ordenes-compra/detalle/{id}/agregar-linea")
    public String agregarLinea(@PathVariable Integer id,
                                @RequestParam Integer idProducto,
                                @RequestParam Integer cantidad,
                                @RequestParam BigDecimal precioUnitario,
                                RedirectAttributes ra) {
        Compras compra = comprasRepository.findById(id).orElse(null);
        if (compra == null) {
            return "redirect:/view/ordenes-compra";
        }
        if (!"PENDIENTE".equals(compra.getEstado())) {
            ra.addFlashAttribute("error", "Solo se pueden agregar líneas a compras en estado PENDIENTE.");
            return "redirect:/view/ordenes-compra/detalle/" + id;
        }

        DetallesCompras linea = DetallesCompras.builder()
                .compra(compra)
                .idProducto(idProducto)
                .cantidad(cantidad)
                .precioUnitario(precioUnitario)
                .subtotal(precioUnitario.multiply(BigDecimal.valueOf(cantidad)))
                .build();
        detallesComprasRepository.save(linea);

        recalcularTotales(compra);
        ra.addFlashAttribute("mensaje", "Producto agregado a la orden de compra.");
        return "redirect:/view/ordenes-compra/detalle/" + id;
    }

    // ELIMINAR LÍNEA
    @PostMapping("/view/ordenes-compra/detalle/{id}/eliminar-linea/{idDetalle}")
    public String eliminarLinea(@PathVariable Integer id, @PathVariable Integer idDetalle, RedirectAttributes ra) {
        Compras compra = comprasRepository.findById(id).orElse(null);
        if (compra == null) {
            return "redirect:/view/ordenes-compra";
        }
        if (!"PENDIENTE".equals(compra.getEstado())) {
            ra.addFlashAttribute("error", "No se pueden quitar líneas de una compra que ya fue recibida.");
            return "redirect:/view/ordenes-compra/detalle/" + id;
        }
        detallesComprasRepository.deleteById(idDetalle);
        recalcularTotales(compra);
        ra.addFlashAttribute("mensaje", "Línea eliminada.");
        return "redirect:/view/ordenes-compra/detalle/" + id;
    }

    // PANTALLA PARA RECIBIR: pide número de lote y vencimiento por cada línea
    @GetMapping("/view/ordenes-compra/recibir/{id}")
    public String formRecibir(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Compras compra = comprasRepository.findById(id).orElse(null);
        if (compra == null) {
            return "redirect:/view/ordenes-compra";
        }
        if (!"PENDIENTE".equals(compra.getEstado())) {
            ra.addFlashAttribute("error", "Esta compra ya no está pendiente.");
            return "redirect:/view/ordenes-compra/detalle/" + id;
        }
        List<DetallesCompras> detalles = detallesComprasRepository.findByCompra_IdCompra(id);
        if (detalles.isEmpty()) {
            ra.addFlashAttribute("error", "Agrega al menos un producto antes de recibir la compra.");
            return "redirect:/view/ordenes-compra/detalle/" + id;
        }
        model.addAttribute("compra", compra);
        model.addAttribute("detalles", detalles);
        model.addAttribute("productos", productosRepository.findAll());
        model.addAttribute("usuarios", usuariosRepository.findAll());
        return "compras/comprasRecibir";
    }

    // CONFIRMAR RECEPCIÓN: crea un lote por línea y registra la entrada en InventarioService
    @Transactional
    @PostMapping("/view/ordenes-compra/recibir/{id}/confirmar")
    public String confirmarRecibir(@PathVariable Integer id,
                                    @RequestParam List<Integer> idDetalle,
                                    @RequestParam List<String> numeroLote,
                                    @RequestParam List<String> fechaVencimiento,
                                    @RequestParam Integer idUsuario,
                                    RedirectAttributes ra) {
        Compras compra = comprasRepository.findById(id).orElse(null);
        if (compra == null) {
            return "redirect:/view/ordenes-compra";
        }
        if (!"PENDIENTE".equals(compra.getEstado())) {
            ra.addFlashAttribute("error", "Esta compra ya no está pendiente.");
            return "redirect:/view/ordenes-compra/detalle/" + id;
        }

        try {
            for (int i = 0; i < idDetalle.size(); i++) {
                DetallesCompras linea = detallesComprasRepository.findById(idDetalle.get(i))
                        .orElseThrow(() -> new IllegalStateException("Una de las líneas ya no existe."));

                if (numeroLote.get(i) == null || numeroLote.get(i).isBlank()
                        || fechaVencimiento.get(i) == null || fechaVencimiento.get(i).isBlank()) {
                    throw new IllegalStateException("Falta número de lote o fecha de vencimiento en alguna línea.");
                }

                Lotes lote = Lotes.builder()
                        .idProducto(linea.getIdProducto())
                        .numeroLote(numeroLote.get(i))
                        .fechaFabricacion(LocalDate.now())
                        .fechaVencimiento(LocalDate.parse(fechaVencimiento.get(i)))
                        .cantidadInicial(linea.getCantidad())
                        .cantidadActual(0) // arranca en 0: InventarioService lo sube al registrar la entrada
                        .estado("DISPONIBLE")
                        .build();
                Lotes loteGuardado = lotesRepository.save(lote);

                inventarioService.registrarMovimiento(
                        "ENTRADA_COMPRA",
                        linea.getIdProducto(),
                        loteGuardado.getIdLote(),
                        linea.getCantidad(),
                        idUsuario,
                        "Recepción de compra #" + compra.getIdCompra()
                                + (compra.getNumeroFacturaProveedor() != null ? " (" + compra.getNumeroFacturaProveedor() + ")" : "")
                );
            }

            compra.setEstado("RECIBIDA");
            comprasRepository.save(compra);

            ra.addFlashAttribute("mensaje", "Compra recibida: se generaron los lotes y se actualizó el inventario.");
            return "redirect:/view/ordenes-compra/detalle/" + id;
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo recibir la compra: " + e.getMessage());
            return "redirect:/view/ordenes-compra/recibir/" + id;
        }
    }

    // CANCELAR (no toca inventario)
    @PostMapping("/view/ordenes-compra/cancelar/{id}")
    public String cancelar(@PathVariable Integer id, RedirectAttributes ra) {
        Compras compra = comprasRepository.findById(id).orElse(null);
        if (compra == null) {
            return "redirect:/view/ordenes-compra";
        }
        if (!"PENDIENTE".equals(compra.getEstado())) {
            ra.addFlashAttribute("error", "Solo se pueden cancelar compras en estado PENDIENTE.");
            return "redirect:/view/ordenes-compra/detalle/" + id;
        }
        compra.setEstado("CANCELADA");
        comprasRepository.save(compra);
        ra.addFlashAttribute("mensaje", "Orden de compra cancelada.");
        return "redirect:/view/ordenes-compra/detalle/" + id;
    }

    private void recalcularTotales(Compras compra) {
        BigDecimal subtotal = detallesComprasRepository.findByCompra_IdCompra(compra.getIdCompra()).stream()
                .map(DetallesCompras::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal impuestos = compra.getImpuestos() != null ? compra.getImpuestos() : BigDecimal.ZERO;
        compra.setSubtotal(subtotal);
        compra.setTotal(subtotal.add(impuestos));
        comprasRepository.save(compra);
    }

    private void cargarListasApoyo(Model model) {
        model.addAttribute("proveedores", proveedoresRepository.findAll());
        model.addAttribute("empleados", empleadosRepository.findAll());
    }
}
