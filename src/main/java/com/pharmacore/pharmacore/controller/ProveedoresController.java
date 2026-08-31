package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Proveedores;
import com.pharmacore.pharmacore.repository.ProveedoresRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/proveedores")
public class ProveedoresController {

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    // ---------- API REST (JSON) ----------

    @GetMapping
    @ResponseBody
    public List<Proveedores> getAll() {
        return proveedoresRepository.findAll();
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Proveedores getById(@PathVariable Integer id) {
        return proveedoresRepository.findById(id).orElse(null);
    }

    @PostMapping
    @ResponseBody
    public Proveedores create(@RequestBody Proveedores proveedores) {
        return proveedoresRepository.save(proveedores);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public Proveedores update(@PathVariable Integer id, @RequestBody Proveedores proveedores) {
        proveedores.setId_provedor(id);
        return proveedoresRepository.save(proveedores);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public void delete(@PathVariable Integer id) {
        proveedoresRepository.deleteById(id);
    }



    @GetMapping("/vista")
    public String lista(Model model) {
        model.addAttribute("proveedores", proveedoresRepository.findAll());
        // Asegúrate de que este sea el nombre correcto del archivo proveedores.html
        return "proveedores/proveedores";
    }

    @GetMapping("/vista/nuevo")
    public String mostrarFormulario(Model model) {
        model.addAttribute("proveedor", new Proveedores());
        // CAMBIADO AQUÍ: ahora apunta a "proveedores.form" que corresponde a proveedoresForm.html
        return "proveedores/proveedores/form";
    }

    @PostMapping("/vista/guardar")
    public String guardar(@ModelAttribute Proveedores proveedor) {
        proveedoresRepository.save(proveedor);
        return "redirect:/proveedores/proveedores";
    }

}

