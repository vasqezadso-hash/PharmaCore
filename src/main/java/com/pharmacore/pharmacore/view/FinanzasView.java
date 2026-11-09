package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.MovimientosInventario;
import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.model.TransaccionesFinancieras;
import com.pharmacore.pharmacore.model.Usuarios;
import com.pharmacore.pharmacore.repository.MovimientosInventarioRepository;
import com.pharmacore.pharmacore.repository.ProductosRepository;
import com.pharmacore.pharmacore.repository.TransaccionesFinancierasRepository;
import com.pharmacore.pharmacore.repository.UsuariosRepository;
import com.pharmacore.pharmacore.service.FinanzasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class FinanzasView {

    @Autowired
    private TransaccionesFinancierasRepository transaccionesRepository;

    @Autowired
    private FinanzasService finanzasService;

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private MovimientosInventarioRepository movimientosRepository;

    @GetMapping({"/view/finanzas", "/view/ingresos-egresos", "/view/caja"})
    public String finanzas(@RequestParam(required = false) String tipo,
                           @RequestParam(required = false) String concepto,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                           Model model) {

        List<TransaccionesFinancieras> todas = transaccionesRepository.findAllByOrderByFechaTransaccionDesc();

        BigDecimal totalIngresos = transaccionesRepository.sumMontoByTipo(TransaccionesFinancieras.TIPO_INGRESO);
        BigDecimal totalEgresos = transaccionesRepository.sumMontoByTipo(TransaccionesFinancieras.TIPO_EGRESO);
        if (totalIngresos == null) totalIngresos = BigDecimal.ZERO;
        if (totalEgresos == null) totalEgresos = BigDecimal.ZERO;
        BigDecimal balanceNeto = totalIngresos.subtract(totalEgresos);

        // Aplicar filtros en memoria
        LocalDateTime dtInicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime dtFin = fechaFin != null ? fechaFin.atTime(LocalTime.MAX) : null;

        List<TransaccionesFinancieras> filtradas = todas.stream()
                .filter(t -> tipo == null || tipo.isBlank() || tipo.equalsIgnoreCase(t.getTipoTransaccion()))
                .filter(t -> concepto == null || concepto.isBlank() || concepto.equalsIgnoreCase(t.getConcepto()))
                .filter(t -> dtInicio == null || !t.getFechaTransaccion().isBefore(dtInicio))
                .filter(t -> dtFin == null || !t.getFechaTransaccion().isAfter(dtFin))
                .collect(Collectors.toList());

        // Mapas de soporte
        Map<Integer, String> nombresUsuarios = usuariosRepository.findAll().stream()
                .collect(Collectors.toMap(u -> u.getIdUsuario().intValue(), Usuarios::getUsername, (a, b) -> a));

        Map<Integer, Productos> productosMap = productosRepository.findAll().stream()
                .collect(Collectors.toMap(Productos::getIdProducto, p -> p, (a, b) -> a));

        Map<Integer, String> resumenMovimientos = new HashMap<>();
        for (MovimientosInventario m : movimientosRepository.findAll()) {
            Productos p = m.getIdProducto() != null ? productosMap.get(m.getIdProducto()) : null;
            String nomProd = p != null ? p.getNombreComercial() : ("Prod #" + m.getIdProducto());
            resumenMovimientos.put(m.getIdMovimiento(), m.getTipoMovimiento() + " • " + nomProd + " (" + m.getCantidad() + " unid.)");
        }

        model.addAttribute("transacciones", filtradas);
        model.addAttribute("totalIngresos", totalIngresos);
        model.addAttribute("totalEgresos", totalEgresos);
        model.addAttribute("balanceNeto", balanceNeto);
        model.addAttribute("totalTransacciones", todas.size());
        model.addAttribute("nombresUsuarios", nombresUsuarios);
        model.addAttribute("resumenMovimientos", resumenMovimientos);

        // Valores de filtro seleccionados
        model.addAttribute("tipoFiltro", tipo);
        model.addAttribute("conceptoFiltro", concepto);
        model.addAttribute("fechaInicioFiltro", fechaInicio);
        model.addAttribute("fechaFinFiltro", fechaFin);

        return "finanzas/finanzas";
    }

    @PostMapping("/view/finanzas/ajuste/guardar")
    public String registrarAjuste(@RequestParam String tipoTransaccion,
                                  @RequestParam String concepto,
                                  @RequestParam BigDecimal monto,
                                  @RequestParam(required = false) String metodoPago,
                                  @RequestParam(required = false) String descripcion,
                                  Authentication authentication,
                                  RedirectAttributes ra) {
        try {
            Integer idUsuario = 1;
            if (authentication != null) {
                Usuarios usuario = usuariosRepository.findByUsernameIgnoreCase(authentication.getName());
                if (usuario != null) {
                    idUsuario = usuario.getIdUsuario().intValue();
                }
            }

            finanzasService.registrarAjusteManual(tipoTransaccion, concepto, monto, metodoPago, descripcion, idUsuario);
            ra.addFlashAttribute("mensaje", "Ajuste de caja (" + tipoTransaccion + ") registrado con éxito por $" + monto + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al registrar el ajuste de caja: " + e.getMessage());
        }
        return "redirect:/view/finanzas";
    }
}
