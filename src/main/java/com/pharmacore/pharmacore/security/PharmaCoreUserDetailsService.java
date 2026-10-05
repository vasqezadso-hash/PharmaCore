package com.pharmacore.pharmacore.security;

import com.pharmacore.pharmacore.model.Usuarios;
import com.pharmacore.pharmacore.repository.UsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class PharmaCoreUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuarios usuario = usuariosRepository.findAll().stream()
                .filter(u -> username.equalsIgnoreCase(u.getUsername()))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        boolean habilitado = usuario.getEstado() == Usuarios.EstadoUsuario.ACTIVO;

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPasswordHash())
                .disabled(!habilitado)
                .authorities(List.of(new SimpleGrantedAuthority(autoridadDeRol(usuario))))
                .build();
    }

    // Convierte el nombre del rol (ej: "Administrador") a la convención de Spring Security
    // ("ROLE_ADMINISTRADOR"), quitando tildes por si alguien los agrega al nombre del rol.
    private String autoridadDeRol(Usuarios usuario) {
        String nombreRol = (usuario.getRol() != null && usuario.getRol().getNombre() != null)
                ? usuario.getRol().getNombre() : "SIN_ROL";
        String sinTildes = Normalizer.normalize(nombreRol, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String limpio = Pattern.compile("[^A-Za-z0-9]").matcher(sinTildes).replaceAll("_");
        return "ROLE_" + limpio.toUpperCase();
    }
}
