package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.ConfiguracionGlobal; // Asegúrate de ajustar o crear el model según tu paquete
import com.pharmacore.pharmacore.repository.ConfiguracionRepository; // Asegúrate de tener este repositorio
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ConfiguracionGlobalView {

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    // VISTA DE CONFIGURACIÓN
    @GetMapping("/view/configuracion")
    public String verConfiguracion(Model model) {
        // Buscamos el registro global con ID 1, si no existe inicializamos uno vacío
        ConfiguracionGlobal config = configuracionRepository.findById(1L).orElse(new ConfiguracionGlobal());
        model.addAttribute("configuracion", config);

        // Métodos de pago globales soportados por el sistema POS
        List<String> metodosPagoDisponibles = List.of(
                "EFECTIVO", "TARJETA_DEBITO", "TARJETA_CREDITO", "TRANSFERENCIA", "NEQUI_DAVIPLATA", "PAGO_MIXTO"
        );
        model.addAttribute("metodosPago", metodosPagoDisponibles);

        return "configuracion/configuracion"; // Ruta del archivo HTML dentro de templates/configuracion/index.html
    }

    // GUARDAR CONFIGURACIÓN
    @PostMapping("/view/configuracion/save")
    public String guardarConfiguracion(@ModelAttribute ConfiguracionGlobal configuracion,
                                       RedirectAttributes ra) {
        configuracion.setId(1L); // Forzamos siempre el ID 1 para mantener un único registro global
        configuracionRepository.save(configuracion);

        ra.addFlashAttribute("mensaje", "¡Configuración del sistema actualizada con éxito!");

        return "redirect:/view/configuracion";
    }
}