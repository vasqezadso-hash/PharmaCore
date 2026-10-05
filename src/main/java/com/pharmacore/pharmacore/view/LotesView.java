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
    public String form(Model model) {
        model.addAttribute("lote", new Lotes());
        model.addAttribute("productos", productosRepository.findAll());
        return "lotes/lotesForm";
    }

    // FORMULARIO EDITAR
    @GetMapping("/view/lotes/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        Lotes lote = lotesRepository.findById(id).orElse(null);
        if (lote == null) {
            return "redirect:/view/lotes";
        }
        model.addAttribute("lote", lote);
        model.addAttribute("productos", productosRepository.findAll());
        return "lotes/lotesForm";
    }

    // GUARDAR (CREAR O ACTUALIZAR)
    @PostMapping("/view/lotes/save")
    public String save(@Valid @ModelAttribute("lote") Lotes lote, BindingResult result,
                        Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("error", "Verifica los campos obligatorios.");
            model.addAttribute("productos", productosRepository.findAll());
            return "lotes/lotesForm";
        }
        if (lote.getCantidadActual() == null) {
            lote.setCantidadActual(lote.getCantidadInicial());
        }
        lotesRepository.save(lote);
        ra.addFlashAttribute("mensaje", "Lote guardado correctamente");
        return "redirect:/view/lotes";
    }

    // ELIMINAR
    @PostMapping("/view/lotes/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        lotesRepository.deleteById(id);
        ra.addFlashAttribute("mensaje", "Lote eliminado");
        return "redirect:/view/lotes";
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
