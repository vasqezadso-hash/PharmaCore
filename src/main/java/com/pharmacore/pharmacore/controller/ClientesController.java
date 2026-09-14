package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Clientes;
import com.pharmacore.pharmacore.repository.ClientesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/clientes")
public class ClientesController {

    @Autowired
    private ClientesRepository clientesRepository;

    // Carga la vista HTML ubicada en templates/clientes/clientes.html
    @GetMapping({"", "/"})
    public String index() {
        return "clientes/clientes";
    }

    // Endpoints de la API (devuelven JSON gracias a @ResponseBody)
    @GetMapping("/api")
    @ResponseBody
    public List<Clientes> getAll() {
        return clientesRepository.findAll();
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public Clientes getById(@PathVariable long id) {
        return clientesRepository.findById(id).orElse(null);
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public Clientes update(@PathVariable long id, @RequestBody Clientes clientes) {
        clientes.setId_cliente(id);
        return clientesRepository.save(clientes);
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public void delete(@PathVariable long id) {
        clientesRepository.deleteById(id);
    }
}