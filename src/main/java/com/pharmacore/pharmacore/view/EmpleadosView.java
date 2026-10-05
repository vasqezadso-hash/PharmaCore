package com.pharmacore.pharmacore.view;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import com.pharmacore.pharmacore.model.Empleados;
import com.pharmacore.pharmacore.model.Roles;
import com.pharmacore.pharmacore.model.Usuarios;
import com.pharmacore.pharmacore.repository.EmpleadosRepository;
import com.pharmacore.pharmacore.repository.RolesRepository;
import com.pharmacore.pharmacore.repository.UsuariosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class EmpleadosView {

    @Autowired
    private EmpleadosRepository empleadosRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Autowired
    private UsuariosRepository usuariosRepository;

    // Esto es lo que me permite cifrar la contraseña antes de guardarla (lo uso
    // más abajo en save()). Al principio no lo tenía y guardaba la contraseña
    // tal cual la escribía el admin, en texto plano, y por eso nadie podía
    // iniciar sesión: BCrypt solo reconoce contraseñas que él mismo cifró.
    @Autowired
    private PasswordEncoder passwordEncoder;

    // LISTA
    @GetMapping("/view/empleados")
    public String lista(Model model) {
        List<Empleados> empleados = empleadosRepository.findAll();

        // "usuario" es @Transient en Empleados: findAll() nunca lo trae poblado.
        // Sin esto, la columna "Rol" de empleados.html siempre cae en 'Sin Rol'
        // aunque el empleado sí tenga usuario y rol asignados en la BD.
        // Se trae la lista completa de usuarios de una sola vez (no uno por
        // empleado) y se indexa por idEmpleado para no hacer N+1 consultas.
        Map<Long, Usuarios> usuariosPorEmpleado = usuariosRepository.findAll().stream()
                .filter(u -> u.getIdEmpleado() != null)
                .collect(Collectors.toMap(Usuarios::getIdEmpleado, u -> u));

        empleados.forEach(e -> e.setUsuario(usuariosPorEmpleado.get(e.getId_empleado())));

        model.addAttribute("empleados", empleados);
        return "empleados/empleados";
    }

    // FORMULARIO CREAR
    @GetMapping("/view/empleados/form")
    public String form(Model model) {
        Empleados empleado = new Empleados();
        empleado.setUsuario(new Usuarios()); // <-- Vital para evitar nulos en el th:field
        model.addAttribute("empleado", empleado);
        model.addAttribute("roles", rolesRepository.findAll());
        return "empleados/empleadosForm";
    }

    @PostMapping("/view/empleados/save")
    public String save(
            @Valid @ModelAttribute Empleados empleado,
                       BindingResult result,
                       @RequestParam(value = "password_hash", required = false) String passwordHash,
                       RedirectAttributes ra) {

        if (result.hasErrors())
        {
            ra.addFlashAttribute("error", "hay campos obligatorios vacios.");
            return "redirect:/view/empleados/form";
        }

        if (empleado.getId_empleado() == null &&
                empleadosRepository.existsByNumeroDocumento(
                        empleado.getNumero_documento()))
        {

            ra.addFlashAttribute(
                    "error",
                    "Ese número de documento ya está registrado."
            );

            return "redirect:/view/empleados/form";
        }
        // 1. Extraer el usuario del formulario y desvincularlo temporalmente
        Usuarios usuarioForm = empleado.getUsuario();
        empleado.setUsuario(null);

        // 2. Guardar primero el empleado para obtener/asegurar su ID
        Empleados empleadoGuardado = empleadosRepository.save(empleado);

        // 3. Procesar el usuario solo si se ingresó un username
        if (usuarioForm != null && usuarioForm.getUsername() != null && !usuarioForm.getUsername().trim().isEmpty()) {

            Usuarios usuarioToSave = null;

            // A. Buscar si el empleado ya tiene un usuario asociado en la base de datos
            if (empleadoGuardado.getId_empleado() != null) {
                // Nota: Asegúrate de que en UsuariosRepository exista un método como findByIdEmpleado(Long id)
                // o findByEmpleados_Id(Long id) dependiendo de cómo lo tengas definido.
                usuarioToSave = usuariosRepository.findByIdEmpleado(empleadoGuardado.getId_empleado());
            }

            // B. Si no se encontró por empleado pero viene un idUsuario desde el formulario
            if (usuarioToSave == null && usuarioForm.getIdUsuario() != null) {
                usuarioToSave = usuariosRepository.findById(usuarioForm.getIdUsuario()).orElse(null);
            }

            // C. Si definitivamente es nuevo, creamos la instancia
            if (usuarioToSave == null) {
                usuarioToSave = new Usuarios();
                usuarioToSave.setIdEmpleado(empleadoGuardado.getId_empleado());
            }

            // 4. Actualizar los campos del usuario
            usuarioToSave.setUsername(usuarioForm.getUsername());
            usuarioToSave.setEstado(usuarioForm.getEstado());

            // Acá cifro la contraseña antes de guardarla. Nunca guardo lo que el
            // usuario escribió tal cual: paso passwordEncoder.encode(...) primero,
            // para que en la base de datos quede el hash de BCrypt, no la
            // contraseña real.
            if (passwordHash != null && !passwordHash.trim().isEmpty()) {
                usuarioToSave.setPasswordHash(passwordEncoder.encode(passwordHash));
            } else if (usuarioToSave.getPasswordHash() == null || usuarioToSave.getPasswordHash().trim().isEmpty()) {
                // Si es un usuario nuevo y no le pusieron contraseña, prefiero cortar
                // acá con un mensaje claro en vez de guardarlo con algo inventado que
                // después no le va a servir para entrar al sistema.
                ra.addFlashAttribute("error", "Debe asignar una contraseña al usuario (" + usuarioForm.getUsername() + ").");
                return "redirect:/view/empleados/form";
            }

            // Asignar rol
            if (usuarioForm.getRol() != null && usuarioForm.getRol().getIdRol() != null) {
                Roles rol = rolesRepository.findById(usuarioForm.getRol().getIdRol()).orElse(null);
                usuarioToSave.setRol(rol);
            }

            // 5. Guardar el usuario
            Usuarios usuarioGuardado = usuariosRepository.save(usuarioToSave);

            // 6. Vincular de regreso al empleado y actualizar
            empleadoGuardado.setUsuario(usuarioGuardado);
            empleadosRepository.save(empleadoGuardado);
        }

        ra.addFlashAttribute("mensaje", "Empleado y usuario guardados con éxito");
        return "redirect:/view/empleados";
    }

    // EDITAR
    @GetMapping("/view/empleados/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Empleados empleado = empleadosRepository.findById(id).orElse(null);
        if (empleado == null) return "redirect:/view/empleados";

        Usuarios usuario = usuariosRepository.findByIdEmpleado(empleado.getId_empleado());
        empleado.setUsuario(usuario != null ? usuario : new Usuarios());

        model.addAttribute("empleado", empleado);
        model.addAttribute("roles", rolesRepository.findAll());
        return "empleados/empleadosForm";
    }

    // ELIMINAR
    @PostMapping("/view/empleados/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        empleadosRepository.deleteById(id);
        ra.addFlashAttribute("mensaje", "Empleado eliminado");
        return "redirect:/view/empleados";
    }
}