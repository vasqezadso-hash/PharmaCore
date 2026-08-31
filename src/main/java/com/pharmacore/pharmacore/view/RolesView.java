package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Roles;
import com.pharmacore.pharmacore.repository.RolesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RolesView {

    @Autowired
    private RolesRepository rolesRepository;

    // LISTA DE ROLES
    @GetMapping("/view/roles")
    public String lista(Model model) {
        model.addAttribute("roles", rolesRepository.findAll());
        return "roles/roles"; // Asegúrate de que tu plantilla esté en templates/roles/roles.html
    }

    // FORMULARIO PARA CREAR NUEVO ROL
    @GetMapping("/view/roles/form")
    public String formCrear(Model model) {
        model.addAttribute("rol", new Roles());
        return "roles/rolesForm"; // Asegúrate de que tu plantilla esté en templates/roles/rolesForm.html
    }

    // FORMULARIO PARA EDITAR ROL EXISTENTE
    @GetMapping("/view/roles/edit/{id}")
    public String formEditar(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Roles rol = rolesRepository.findById(id).orElse(null);
        if (rol == null) {
            ra.addFlashAttribute("error", "El rol especificado no existe.");
            return "redirect:/view/roles";
        }
        model.addAttribute("rol", rol);
        return "roles/rolesForm";
    }

    // GUARDAR (CREAR / EDITAR)
    @PostMapping("/view/roles/save")
    public String save(@ModelAttribute Roles rol, RedirectAttributes ra) {
        try {
            rolesRepository.save(rol);
            ra.addFlashAttribute("mensaje", "Rol guardado con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar el rol. Es posible que el nombre ya exista.");
        }
        return "redirect:/view/roles";
    }

    // ELIMINAR ROL
    @PostMapping("/view/roles/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            rolesRepository.deleteById(id);
            ra.addFlashAttribute("mensaje", "Rol eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se puede eliminar el rol porque está asociado a uno o más usuarios.");
        }
        return "redirect:/view/roles";
    }
}