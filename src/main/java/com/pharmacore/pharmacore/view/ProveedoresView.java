//by Jacob Mafla
package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Proveedores;
import com.pharmacore.pharmacore.repository.ProveedoresRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProveedoresView {

    @Autowired
    private ProveedoresRepository repo;

    // LISTAR
    @GetMapping("/view/proveedores")
    public String lista(Model model) {
        model.addAttribute("proveedores", repo.findAll());
        return "proveedores/proveedores";
    }

    // FORMULARIO (Crear)
    @GetMapping("/view/proveedores/form")
    public String form(Model model) {
        model.addAttribute("proveedor", new Proveedores());
        return "proveedores/proveedoresForm";
    }

    // GUARDAR
    @PostMapping("/view/proveedores/save")
    public String save(@Valid @ModelAttribute Proveedores proveedor, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Verifica los campos obligatorios.");
            return "redirect:/view/proveedores/form";
        }
        repo.save(proveedor);
        ra.addFlashAttribute("mensaje", "Proveedor guardado correctamente");
        return "redirect:/view/proveedores";
    }

    // EDITAR
    @GetMapping("/view/proveedores/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("proveedor", repo.findById(id).orElse(new Proveedores()));
        return "proveedores/proveedoresForm";
    }

    // ELIMINAR
    @PostMapping("/view/proveedores/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("mensaje", "Proveedor eliminado");
        return "redirect:/view/proveedores";
    }
}