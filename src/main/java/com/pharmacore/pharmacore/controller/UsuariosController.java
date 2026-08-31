package com.pharmacore.pharmacore.controller;

import com.pharmacore.pharmacore.model.Usuarios;
import com.pharmacore.pharmacore.repository.UsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuariosController {

    @Autowired
    private UsuariosRepository usuariosRepository;

    @GetMapping
    public List<Usuarios> getAll() {
        return usuariosRepository.findAll();
    }

    @GetMapping("/{id}")
    public Usuarios getById(@PathVariable Long id) {
        return usuariosRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Usuarios create(@RequestBody Usuarios usuario) {
        return usuariosRepository.save(usuario);
    }

    @PutMapping("/{id}")
    public Usuarios update(@PathVariable Long id, @RequestBody Usuarios usuario) {
        // Usamos el setter en camelCase que definimos en la entidad
        usuario.setIdUsuario(id);

        // Verificamos el password usando el nuevo nombre de atributo/getter
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isEmpty()) {
            Usuarios existente = usuariosRepository.findById(id).orElse(null);
            if (existente != null) {
                usuario.setPasswordHash(existente.getPasswordHash());
            }
        }

        return usuariosRepository.save(usuario);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        usuariosRepository.deleteById(id);
    }
}