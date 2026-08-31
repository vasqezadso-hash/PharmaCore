package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Clientes;
import com.pharmacore.pharmacore.repository.ClientesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ClientesView
{
    @Autowired
    private ClientesRepository clientesRepository;

    // LISTA
    @GetMapping("/view/clientes")
    public String lista(Model model)
    {
        model.addAttribute("clientes", clientesRepository.findAll());

        return "clientes/clientes";
    }

    // FORMULARIO NUEVO
    @GetMapping("/view/clientes/form")
    public String form(Model model)
    {
        model.addAttribute("cliente", new Clientes());

        return "clientes/clientesForm";
    }

    // GUARDAR (CREAR / ACTUALIZAR)
    @PostMapping("/view/clientes/save")
    public String save(@ModelAttribute Clientes clientes,
                       RedirectAttributes ra)
    {
        clientesRepository.save(clientes);

        ra.addFlashAttribute("mensaje",
                "Cliente registrado con éxito");

        return "redirect:/view/clientes";
    }

    // EDITAR (Corregido a "cliente" en singular)
    // EDITAR
    @GetMapping("/view/clientes/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Clientes cliente = clientesRepository.findById(id).orElse(new Clientes());
        model.addAttribute("cliente", cliente);
        return "clientes/clientesForm";
    }

    // ELIMINAR
    @PostMapping("/view/clientes/delete/{id}")
    public String delete(@PathVariable Long id,
                         RedirectAttributes ra)
    {
        clientesRepository.deleteById(id);

        ra.addFlashAttribute("mensaje",
                "Cliente eliminado con éxito");

        return "redirect:/view/clientes";
    }
}