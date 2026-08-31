package com.pharmacore.pharmacore.controller; // Ajusta según tu paquete base

import com.pharmacore.pharmacore.model.ConfiguracionGlobal;
import com.pharmacore.pharmacore.repository.ConfiguracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionGlobalController {

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    // 1. Mostrar la vista con los datos actuales
    @GetMapping
    public String verConfiguracion(Model model) {
        // Buscamos el registro con ID 1 por defecto, si no existe inicializamos uno vacío
        ConfiguracionGlobal config = configuracionRepository.findById(1L).orElse(new ConfiguracionGlobal());
        model.addAttribute("configuracion", config);

        // Métodos de pago globales soportados por el sistema de ventas/POS
        List<String> metodosPagoDisponibles = List.of(
                "EFECTIVO", "TARJETA_DEBITO", "TARJETA_CREDITO", "TRANSFERENCIA", "NEQUI_DAVIPLATA", "PAGO_MIXTO"
        );
        model.addAttribute("metodosPago", metodosPagoDisponibles);

        return "configuracion/configuracion"; // Ruta del archivo HTML en templates
    }

    // 2. Procesar el formulario de actualización
    @PostMapping("/guardar")
    public String guardarConfiguracion(@ModelAttribute ConfiguracionGlobal configuracion, RedirectAttributes redirectAttributes) {
        configuracion.setId(1L); // Forzamos siempre el ID 1 para asegurar la persistencia en el registro único global
        configuracionRepository.save(configuracion);

        redirectAttributes.addFlashAttribute("mensaje", "¡Configuración del sistema actualizada con éxito!");
        return "redirect:/configuracion";
    }
}