package com.pharmacore.pharmacore.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Esta clase es una "red de seguridad" para todos los controladores @Controller
// (los que están en el paquete view). Antes de tenerla, si un módulo se le
// olvidaba poner su propio try/catch (por ejemplo, al eliminar un empleado que
// ya tenía ventas registradas), el error se iba directo a la pantalla en
// blanco por defecto de Spring. Con @ControllerAdvice centralizo eso: cualquier
// excepción que se le escape a un controlador cae acá y se convierte en un
// mensaje claro para el usuario, en vez de un error técnico. con esto ne se remplaza el try/catch de cada controlador
@ControllerAdvice(basePackages = "com.pharmacore.pharmacore.view")
public class GlobalWebExceptionHandler {

    // Salta cuando la base de datos rechaza la operación por una restricción

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String manejarIntegridadDatos(DataIntegrityViolationException ex, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "No se pudo completar la operación: el registro está relacionado con otros " +
                "datos del sistema (ventas, compras, movimientos de inventario, etc.) y no se puede eliminar ni " +
                "guardar así. Si necesita desactivarlo, use el estado en vez de eliminarlo.");
        return "redirect:" + destinoSeguro(request);
    }

    @ExceptionHandler(BindException.class)
    public String manejarErroresDeValidacion(BindException ex, HttpServletRequest request, RedirectAttributes ra) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage())
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .reduce((a, b) -> a + " " + b)
                .orElse("Revise los datos del formulario: hay campos obligatorios sin completar o con un valor inválido.");
        ra.addFlashAttribute("error", mensaje);
        return "redirect:" + destinoSeguro(request);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String manejarErrorDeNegocio(RuntimeException ex, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage() != null ? ex.getMessage() : "No se pudo completar la operación.");
        return "redirect:" + destinoSeguro(request);
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalWebExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public String manejarErrorGenerico(Exception ex, HttpServletRequest request, RedirectAttributes ra) {
        log.error("Error no controlado en la ruta {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ra.addFlashAttribute("error", "Ocurrió un error inesperado al procesar la solicitud. " +
                "Si el problema persiste, contacte al administrador del sistema.");
        return "redirect:" + destinoSeguro(request);
    }

    private String destinoSeguro(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String uriActual = request.getRequestURI();
        if (referer != null && referer.contains("://" + request.getServerName())) {
            // Evita redirigir a /login o a la misma ruta que originó el error para no generar un bucle infinito
            if (!referer.contains("/login") && !referer.endsWith(uriActual)) {
                return referer;
            }
        }
        if (uriActual != null && (uriActual.equals("/view/home") || uriActual.equals("/"))) {
            return "/view/medicamentos";
        }
        return "/view/home";
    }
}
