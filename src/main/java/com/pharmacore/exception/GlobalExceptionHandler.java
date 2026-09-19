package com.pharmacore.exception;


import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String manejarRecursoNoEncontrado(RecursoNoEncontradoException ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("error", "Recurso No Encontrado");
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String manejarExcepcionesGenerales(Exception ex, Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("error", "Error Interno del Servidor");
        model.addAttribute("message", "Ocurrió un error inesperado: " + ex.getMessage());
        model.addAttribute("timestamp", LocalDateTime.now());
        return "error/500";
    }
}