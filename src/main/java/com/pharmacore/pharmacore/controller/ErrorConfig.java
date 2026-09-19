package com.pharmacore.pharmacore.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ErrorConfig implements ErrorViewResolver {

    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
        Map<String, Object> errorModel = new HashMap<>(model);
        errorModel.putIfAbsent("status", status.value());
        errorModel.putIfAbsent("error", status.getReasonPhrase());
        errorModel.putIfAbsent("message", "No fue posible procesar la solicitud en este momento.");

        if (status == HttpStatus.NOT_FOUND) {
            return new ModelAndView("error/404", errorModel);
        }
        if (status == HttpStatus.FORBIDDEN) {
            return new ModelAndView("error/403", errorModel);
        }
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return new ModelAndView("error/500", errorModel);
        }
        return new ModelAndView("error/error", errorModel);
    }
}