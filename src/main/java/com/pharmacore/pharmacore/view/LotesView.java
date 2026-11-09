package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Lotes;
import com.pharmacore.pharmacore.repository.LotesRepository;
import com.pharmacore.pharmacore.repository.ProductosRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class LotesView {

    @Autowired
    private LotesRepository lotesRepository;

    @Autowired
    private ProductosRepository productosRepository;

    @GetMapping("/view/lotes")
    public String lista(Model model) {
        model.addAttribute("lotes", lotesRepository.findAll());
        cargarNombresProductos(model);
        return "lotes/lotes";
    }

    // FORMULARIO CREAR
    @GetMapping("/view/lotes/form")
    public String form(@RequestParam(value = "idProducto", required = false) Integer idProducto,
                       @RequestParam(value = "origen", required = false) String origen,
                       Model model) {
        Lotes lote = new Lotes();
        if (idProducto != null) {
            lote.setIdProducto(idProducto);
        }
        model.addAttribute("lote", lote);
        model.addAttribute("productos", productosRepository.findAll());
        model.addAttribute("origen", origen);
        return "lotes/lotesForm";
    }

    // FORMULARIO EDITAR
    @GetMapping("/view/lotes/edit/{id}")
    public String edit(@PathVariable Integer id,
                       @RequestParam(value = "origen", required = false) String origen,
                       Model model) {
        Lotes lote = lotesRepository.findById(id).orElse(null);
        if (lote == null) {
            return "redirect:/view/lotes";
        }
        model.addAttribute("lote", lote);
        model.addAttribute("productos", productosRepository.findAll());
        model.addAttribute("origen", origen);
        return "lotes/lotesForm";
    }

    // GUARDAR (CREAR O ACTUALIZAR)
    @PostMapping("/view/lotes/save")
    public String save(@Valid @ModelAttribute("lote") Lotes lote, BindingResult result,
                       @RequestParam(value = "origen", required = false) String origen,
                       Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("error", "Verifica los campos obligatorios.");
            model.addAttribute("productos", productosRepository.findAll());
            model.addAttribute("origen", origen);
            return "lotes/lotesForm";
        }
        if (lote.getCantidadActual() == null) {
            lote.setCantidadActual(lote.getCantidadInicial());
        }
        lotesRepository.save(lote);

        // Sincronizar stock consolidado del producto maestro
        sincronizarStockProducto(lote.getIdProducto());

        ra.addFlashAttribute("mensaje", "Lote guardado correctamente y stock del medicamento sincronizado");
        if ("detalle".equalsIgnoreCase(origen) && lote.getIdProducto() != null) {
            return "redirect:/view/medicamentos/detalle/" + lote.getIdProducto();
        }
        return "redirect:/view/lotes";
    }

    // ELIMINAR
    @PostMapping("/view/lotes/delete/{id}")
    public String delete(@PathVariable Integer id,
                         @RequestParam(value = "origen", required = false) String origen,
                         RedirectAttributes ra) {
        Lotes lote = lotesRepository.findById(id).orElse(null);
        Integer idProducto = lote != null ? lote.getIdProducto() : null;

        lotesRepository.deleteById(id);

        if (idProducto != null) {
            sincronizarStockProducto(idProducto);
        }

        ra.addFlashAttribute("mensaje", "Lote eliminado y stock del producto recalculado");
        if ("detalle".equalsIgnoreCase(origen) && idProducto != null) {
            return "redirect:/view/medicamentos/detalle/" + idProducto;
        }
        return "redirect:/view/lotes";
    }

    /**
     * Sincroniza el stock consolidado del producto maestro
     * sumando las existencias de todos sus lotes no retirados.
     */
    public void sincronizarStockProducto(Integer idProducto) {
        if (idProducto == null) return;
        var optProducto = productosRepository.findById(idProducto);
        if (optProducto.isEmpty()) return;

        var producto = optProducto.get();
        List<Lotes> lotes = lotesRepository.findByIdProductoOrderByFechaVencimientoAsc(idProducto);
        int totalStock = 0;
        for (Lotes l : lotes) {
            if (!"RETIRADO".equals(l.getEstado()) && !"DEVUELTO".equals(l.getEstado())) {
                if (l.getCantidadActual() != null && l.getCantidadActual() > 0) {
                    totalStock += l.getCantidadActual();
                }
            }
        }
        producto.setStockTotal(totalStock);
        productosRepository.save(producto);
    }

    @GetMapping("/view/vencimientos")
    public String vencimientos(Model model) {
        LocalDate hoy = LocalDate.now();
        List<Lotes> todos = lotesRepository.findAll();

        List<Lotes> vencidos = todos.stream()
                .filter(l -> l.getFechaVencimiento() != null && l.getFechaVencimiento().isBefore(hoy))
                .collect(Collectors.toList());

        List<Lotes> proximosAVencer = todos.stream()
                .filter(l -> l.getFechaVencimiento() != null
                        && !l.getFechaVencimiento().isBefore(hoy)
                        && !l.getFechaVencimiento().isAfter(hoy.plusDays(30)))
                .collect(Collectors.toList());

        model.addAttribute("vencidos", vencidos);
        model.addAttribute("proximosAVencer", proximosAVencer);
        cargarNombresProductos(model);
        return "lotes/vencimientos";
    }

    private void cargarNombresProductos(Model model) {
        model.addAttribute("nombresProductos", productosRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getIdProducto(), p -> p.getNombreComercial())));
    }
}
