package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.repository.RolesRepository;
import com.pharmacore.pharmacore.repository.UsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UsuariosView {

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Autowired
    private RolesRepository rolesRepository;

    // LISTA (Vista principal para ver los usuarios y sus empleados asociados)
    @GetMapping("/view/usuarios")
    public String lista(Model model) {
        model.addAttribute("usuarios", usuariosRepository.findAll());
        model.addAttribute("roles", rolesRepository.findAll());
        return "usuarios/usuarios";
    }

    // ELIMINAR (Opcional: por si necesitas dar de baja credenciales desde aquí)
    @PostMapping("/view/usuarios/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        usuariosRepository.deleteById(id);
        ra.addFlashAttribute("mensaje", "Usuario eliminado correctamente");
        return "redirect:/view/usuarios";
    }
}