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
        return "roles/roles";
    }

    // FORMULARIO PARA CREAR NUEVO ROL
    @GetMapping("/view/roles/form")
    public String formCrear(Model model) {
        model.addAttribute("rol", new Roles());
        return "roles/rolesForm";
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
            // Asegurar que los checkboxes no marcados queden en false (comportamiento estándar HTTP)
            if (rol.getAccesoPos() == null) rol.setAccesoPos(false);
            if (rol.getAccesoVentasHistorial() == null) rol.setAccesoVentasHistorial(false);
            if (rol.getAccesoClientes() == null) rol.setAccesoClientes(false);
            if (rol.getAccesoProductos() == null) rol.setAccesoProductos(false);
            if (rol.getAccesoCategorias() == null) rol.setAccesoCategorias(false);
            if (rol.getAccesoLotes() == null) rol.setAccesoLotes(false);
            if (rol.getAccesoInventario() == null) rol.setAccesoInventario(false);
            if (rol.getAccesoCompras() == null) rol.setAccesoCompras(false);
            if (rol.getAccesoProveedores() == null) rol.setAccesoProveedores(false);
            if (rol.getAccesoDevoluciones() == null) rol.setAccesoDevoluciones(false);
            if (rol.getAccesoDomicilios() == null) rol.setAccesoDomicilios(false);
            if (rol.getAccesoFinanzas() == null) rol.setAccesoFinanzas(false);
            if (rol.getAccesoReportes() == null) rol.setAccesoReportes(false);
            if (rol.getAccesoUsuarios() == null) rol.setAccesoUsuarios(false);

            rolesRepository.save(rol);
            ra.addFlashAttribute("mensaje", "Rol y permisos guardados con éxito.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar el rol. Es posible que el nombre ya exista: " + e.getMessage());
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
