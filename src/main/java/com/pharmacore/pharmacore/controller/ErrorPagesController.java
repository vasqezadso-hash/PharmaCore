package com.pharmacore.pharmacore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping({"/errores", "/error"})
public class ErrorPagesController {

    @GetMapping("/general")
    public String errorGeneral(Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Error Inesperado");
        model.addAttribute("message", "No fue posible procesar la solicitud en este momento.");
        return "error/error";
    }

    @GetMapping("/500")
    public String error500(Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Error Interno del Servidor");
        model.addAttribute("message", "Ocurrió un inconveniente interno en el sistema. Inténtalo de nuevo más tarde.");
        return "error/error";
    }

    @GetMapping("/403")
    public String accesoRestringido() {
        return "error/403";
    }
}