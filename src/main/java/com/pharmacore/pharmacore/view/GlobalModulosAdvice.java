package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Usuarios;
import com.pharmacore.pharmacore.repository.UsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.Map;

@ControllerAdvice
public class GlobalModulosAdvice {

    @Autowired
    private UsuariosRepository usuariosRepository;

    @ModelAttribute("permisosModulos")
    public Map<String, Boolean> permisosModulos(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Collections.emptyMap();
        }
        try {
            Usuarios usuario = usuariosRepository.findByUsernameIgnoreCase(authentication.getName());
            if (usuario != null && usuario.getRol() != null) {
                return usuario.getRol().obtenerMapaPermisos();
            }
        } catch (Exception ignored) {
        }
        return Collections.emptyMap();
    }
}
