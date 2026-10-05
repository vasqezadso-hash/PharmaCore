package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.MovimientosInventario;
import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.repository.*;
import com.pharmacore.pharmacore.service.InventarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class InventarioView {

    @Autowired
    private MovimientosInventarioRepository movimientosRepository;

    @Autowired
    private InventarioService inventarioService;

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private CategoriasRepository categoriasRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private UsuariosRepository usuariosRepository;

    @Autowired
    private LotesRepository lotesRepository;

    // KARDEX: historial de movimientos
    @GetMapping("/view/inventario/movimientos")
    public String movimientos(Model model) {
        model.addAttribute("movimientos", movimientosRepository.findAllByOrderByFechaMovimientoDesc());
        model.addAttribute("nombresProductos", productosRepository.findAll().stream()
                .collect(Collectors.toMap(Productos::getIdProducto, Productos::getNombreComercial)));
        model.addAttribute("nombresLotes", lotesRepository.findAll().stream()
                .collect(Collectors.toMap(com.pharmacore.pharmacore.model.Lotes::getIdLote, com.pharmacore.pharmacore.model.Lotes::getNumeroLote)));
        model.addAttribute("nombresUsuarios", usuariosRepository.findAll().stream()
                .collect(Collectors.toMap(u -> u.getIdUsuario().intValue(), u -> u.getUsername())));
        return "inventario/movimientos";
    }

    @GetMapping("/view/inventario/movimientos/form")
    public String form(Model model) {
        cargarListasApoyo(model);
        return "inventario/movimientosForm";
    }

    @Transactional
    @PostMapping("/view/inventario/movimientos/guardar")
    public String guardar(@RequestParam String tipoMovimiento,
                           @RequestParam Integer idProducto,
                           @RequestParam(required = false) Integer idLote,
                           @RequestParam Integer cantidad,
                           @RequestParam Integer idUsuario,
                           @RequestParam(required = false) String motivo,
                           RedirectAttributes ra) {
        try {
            inventarioService.registrarMovimiento(tipoMovimiento, idProducto, idLote, cantidad, idUsuario, motivo);
            ra.addFlashAttribute("mensaje", "Movimiento registrado y stock actualizado correctamente.");
            return "redirect:/view/inventario/movimientos";
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/view/inventario/movimientos/form";
        }
    }

    // CONSULTA DE STOCK: por categoría / proveedor / ubicación, con alerta de stock bajo
    @GetMapping("/view/inventario/stock")
    public String stock(@RequestParam(required = false) Long idCategoria,
                         @RequestParam(required = false) Integer idProveedor,
                         @RequestParam(required = false) String ubicacion,
                         @RequestParam(required = false) Boolean soloStockBajo,
                         Model model) {

        List<Productos> productos = productosRepository.findAll().stream()
                .filter(p -> idCategoria == null || (p.getCategoria() != null && idCategoria.equals(p.getCategoria().getId_categoria())))
                .filter(p -> idProveedor == null || (p.getProveedor() != null && idProveedor.equals(p.getProveedor().getId_proveedor())))
                .filter(p -> ubicacion == null || ubicacion.isBlank()
                        || (p.getUbicacionEstante() != null && p.getUbicacionEstante().toLowerCase().contains(ubicacion.toLowerCase())))
                .filter(p -> !Boolean.TRUE.equals(soloStockBajo) || p.isStockBajo())
                .collect(Collectors.toList());

        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categoriasRepository.findAll());
        model.addAttribute("proveedores", proveedoresRepository.findAll());
        model.addAttribute("totalStockBajo", productosRepository.findAll().stream().filter(Productos::isStockBajo).count());
        return "inventario/stock";
    }

    private void cargarListasApoyo(Model model) {
        model.addAttribute("productos", productosRepository.findAll());
        model.addAttribute("usuarios", usuariosRepository.findAll());

        model.addAttribute("tiposMovimiento", new String[]{"AJUSTE_PERDIDA", "AJUSTE_DANIO", "DEVOLUCION_CLIENTE", "DEVOLUCION_PROVEEDOR"});
    }
}
