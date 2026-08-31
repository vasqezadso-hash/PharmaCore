package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Categorias;
import com.pharmacore.pharmacore.repository.CategoriasRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriasController
{
    @Autowired
    private CategoriasRepository categoriasRepository;

    @GetMapping
    public List<Categorias> getAll()
    {
        return categoriasRepository.findAll();
    }

    @GetMapping("/{id}")
    public Categorias getById(@PathVariable Integer id)
    {
        return categoriasRepository.findById(id).orElse(null);
    }

    @PutMapping("/{id}")
    public Categorias update(@PathVariable Integer id, @RequestBody Categorias categorias)
    {
        categorias.setId(id);
        return categoriasRepository.save(categorias);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id)
    {
        categoriasRepository.deleteById(id);
    }
}