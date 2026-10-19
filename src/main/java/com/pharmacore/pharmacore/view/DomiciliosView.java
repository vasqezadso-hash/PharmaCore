package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Clientes;
import com.pharmacore.pharmacore.model.Domicilios;
import com.pharmacore.pharmacore.model.Ventas;
import com.pharmacore.pharmacore.repository.ClientesRepository;
import com.pharmacore.pharmacore.repository.DomiciliosRepository;
import com.pharmacore.pharmacore.repository.VentasRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Controller
public class DomiciliosView {

    @Autowired
    private DomiciliosRepository domiciliosRepository;

    @Autowired
    private VentasRepository ventasRepository;

    @Autowired
    private ClientesRepository clientesRepository;

    // LISTA
    @GetMapping("/view/domicilios")
    public String lista(Model model) {
        model.addAttribute("domicilios", domiciliosRepository.findAllByOrderByIdDomicilioDesc());
        cargarNombres(model);
        return "domicilios/domicilios";
    }

    // FORMULARIO
    @GetMapping("/view/domicilios/form")
    public String form(Model model) {
        model.addAttribute("domicilio", new Domicilios());
        cargarListasApoyo(model);
        return "domicilios/domiciliosForm";
    }

    // GUARDAR
    @PostMapping("/view/domicilios/guardar")
    public String guardar(@Valid @ModelAttribute("domicilio") Domicilios domicilio,
                           BindingResult bindingResult,
                           RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String mensaje = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .distinct()
                    .collect(Collectors.joining(" "));
            ra.addFlashAttribute("error", mensaje);
            return "redirect:/view/domicilios/form";
        }

        try {
            if (domiciliosRepository.existsByIdVenta(domicilio.getIdVenta())) {
                throw new IllegalStateException("Esa venta ya tiene un domicilio registrado.");
            }
            domicilio.setEstado("PENDIENTE");
            domiciliosRepository.save(domicilio);
            ra.addFlashAttribute("mensaje", "Domicilio registrado correctamente.");
            return "redirect:/view/domicilios";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo registrar el domicilio: " + e.getMessage());
            return "redirect:/view/domicilios/form";
        }
    }


    @PostMapping("/view/domicilios/estado/{id}")
    public String cambiarEstado(@PathVariable Integer id,
                                 @RequestParam String nuevoEstado,
                                 RedirectAttributes ra) {
        Domicilios domicilio = domiciliosRepository.findById(id).orElse(null);
        if (domicilio == null) {
            ra.addFlashAttribute("error", "El domicilio indicado ya no existe.");
            return "redirect:/view/domicilios";
        }
        if ("ENTREGADO".equals(domicilio.getEstado()) || "CANCELADO".equals(domicilio.getEstado())) {
            ra.addFlashAttribute("error", "Este domicilio ya está " + domicilio.getEstado().toLowerCase() + " y no se puede modificar.");
            return "redirect:/view/domicilios";
        }

        domicilio.setEstado(nuevoEstado);
        if ("EN_CAMINO".equals(nuevoEstado)) {
            domicilio.setFechaHoraSalida(LocalDateTime.now());
        } else if ("ENTREGADO".equals(nuevoEstado)) {
            if (domicilio.getFechaHoraSalida() == null) {
                domicilio.setFechaHoraSalida(LocalDateTime.now());
            }
            domicilio.setFechaHoraEntrega(LocalDateTime.now());
        }
        domiciliosRepository.save(domicilio);
        ra.addFlashAttribute("mensaje", "Estado del domicilio actualizado a " + nuevoEstado.replace("_", " ") + ".");
        return "redirect:/view/domicilios";
    }

    private void cargarListasApoyo(Model model) {
        // Solo se ofrecen ventas PAGADAS que todavía no tengan domicilio.
        List<Ventas> ventasDisponibles = ventasRepository.findAll().stream()
                .filter(v -> "PAGADA".equals(v.getEstado()))
                .filter(v -> v.getIdCliente() != null)
                .filter(v -> !domiciliosRepository.existsByIdVenta(v.getIdVenta()))
                .collect(Collectors.toList());
        model.addAttribute("ventas", ventasDisponibles);

        // Datos de cada venta (cliente, dirección, teléfono) para autocompletar el formulario por JS.
        List<Clientes> clientes = clientesRepository.findAll();
        model.addAttribute("clientesJs", clientes.stream()
                .collect(Collectors.toMap(Clientes::getId_cliente, c -> java.util.Map.of(
                        "nombre", c.getNombreCompleto() != null ? c.getNombreCompleto() : "",
                        "direccion", c.getDireccion() != null ? c.getDireccion() : "",
                        "telefono", c.getTelefono() != null ? c.getTelefono() : ""
                ))));
        model.addAttribute("ventasJs", ventasDisponibles.stream()
                .collect(Collectors.toMap(Ventas::getIdVenta, v -> v.getIdCliente())));
    }

    private void cargarNombres(Model model) {
        model.addAttribute("nombresClientes", clientesRepository.findAll().stream()
                .collect(Collectors.toMap(Clientes::getId_cliente, Clientes::getNombreCompleto)));
        model.addAttribute("numerosFactura", ventasRepository.findAll().stream()
                .collect(Collectors.toMap(Ventas::getIdVenta, Ventas::getNumeroFactura)));
    }
}
