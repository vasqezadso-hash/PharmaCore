//by Jacob Mafla
package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Categorias;
import com.pharmacore.pharmacore.repository.CategoriasRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CategoriasView {

    @Autowired
    private CategoriasRepository repo;

    // LISTAR
    @GetMapping("/view/categorias")
    public String lista(Model model) {
        model.addAttribute("categorias", repo.findAll());
        return "categorias/categorias";
    }

    // FORMULARIO (Crear)
    @GetMapping("/view/categorias/form")
    public String form(Model model) {
        model.addAttribute("categoria", new Categorias());
        return "categorias/categoriasForm";
    }

    // GUARDAR
    @PostMapping("/view/categorias/save")
    public String save(@Valid @ModelAttribute Categorias categoria, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("error", "Verifica los campos obligatorios.");
            return "redirect:/view/categorias/form";
        }
        repo.save(categoria);
        ra.addFlashAttribute("mensaje", "Categoría guardada correctamente");
        return "redirect:/view/categorias";
    }

    // EDITAR
    @GetMapping("/view/categorias/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("categoria", repo.findById(id).orElse(new Categorias()));
        return "categorias/categoriasForm";
    }

    // ELIMINAR
    @PostMapping("/view/categorias/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        repo.deleteById(id);
        ra.addFlashAttribute("mensaje", "Categoría eliminada");
        return "redirect:/view/categorias";
    }
}