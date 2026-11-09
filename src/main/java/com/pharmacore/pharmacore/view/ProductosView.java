package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Lotes;
import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.repository.CategoriasRepository;
import com.pharmacore.pharmacore.repository.LotesRepository;
import com.pharmacore.pharmacore.repository.ProductosRepository;
import com.pharmacore.pharmacore.repository.ProveedoresRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class ProductosView {

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private LotesRepository lotesRepository;

    @Autowired
    private CategoriasRepository categoriasRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @GetMapping("/view/medicamentos")
    public String lista(@RequestParam(value = "q", required = false) String q, Model model) {
        List<Productos> productos;
        if (StringUtils.hasText(q)) {
            productos = productosRepository
                    .findByNombreComercialContainingIgnoreCaseOrNombreGenericoContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(q, q, q);
        } else {
            productos = productosRepository.findAll();
        }
        model.addAttribute("productos", productos);

        // Conteo de lotes activos por producto para mostrar en el catálogo maestro
        List<Lotes> todosLotes = lotesRepository.findAll();
        Map<Integer, Long> conteoLotes = todosLotes.stream()
                .collect(Collectors.groupingBy(Lotes::getIdProducto, Collectors.counting()));
        model.addAttribute("conteoLotes", conteoLotes);

        model.addAttribute("q", q == null ? "" : q);
        return "medicamentos/medicamentos";
    }

    // DETALLE DEL PRODUCTO MAESTRO E HISTORIAL DE LOTES
    @GetMapping("/view/medicamentos/detalle/{id}")
    public String detalle(@PathVariable Integer id, Model model) {
        Productos producto = productosRepository.findById(id).orElse(null);
        if (producto == null) {
            return "redirect:/view/medicamentos";
        }
        List<Lotes> lotes = lotesRepository.findByIdProductoOrderByFechaVencimientoAsc(id);

        // Recalcular stock acumulado real de sus lotes activos para asegurar total consistencia
        int stockCalculado = lotes.stream()
                .filter(l -> !"RETIRADO".equals(l.getEstado()) && !"DEVUELTO".equals(l.getEstado()))
                .mapToInt(l -> l.getCantidadActual() != null ? l.getCantidadActual() : 0)
                .sum();

        if (producto.getStockTotal() == null || !producto.getStockTotal().equals(stockCalculado)) {
            producto.setStockTotal(stockCalculado);
            productosRepository.save(producto);
        }

        long lotesDisponibles = lotes.stream().filter(l -> "DISPONIBLE".equals(l.getEstadoCalculado())).count();
        long lotesProximos = lotes.stream().filter(l -> "PROXIMO_A_VENCER".equals(l.getEstadoCalculado())).count();
        long lotesVencidos = lotes.stream().filter(l -> "VENCIDO".equals(l.getEstadoCalculado())).count();

        model.addAttribute("producto", producto);
        model.addAttribute("lotes", lotes);
        model.addAttribute("stockCalculado", stockCalculado);
        model.addAttribute("lotesDisponibles", lotesDisponibles);
        model.addAttribute("lotesProximos", lotesProximos);
        model.addAttribute("lotesVencidos", lotesVencidos);

        return "medicamentos/medicamentoDetalle";
    }

    // FORMULARIO CREAR
    @GetMapping("/view/medicamentos/form")
    public String form(Model model) {
        model.addAttribute("producto", new Productos());
        cargarListasApoyo(model);
        return "medicamentos/medicamentosForm";
    }

    // GUARDAR (CREAR O ACTUALIZAR)
    @PostMapping("/view/medicamentos/save")
    public String save(@Valid @ModelAttribute("producto") Productos producto, BindingResult result,
                        Model model, RedirectAttributes ra) {

        if (result.hasErrors()) {
            model.addAttribute("error", "Hay campos obligatorios vacíos o incorrectos.");
            cargarListasApoyo(model);
            return "medicamentos/medicamentosForm";
        }

        String errorDuplicado = validarDuplicado(producto);
        if (errorDuplicado != null) {
            model.addAttribute("error", errorDuplicado);
            cargarListasApoyo(model);
            return "medicamentos/medicamentosForm";
        }

        productosRepository.save(producto);
        ra.addFlashAttribute("mensaje", "Medicamento maestro guardado con éxito.");
        return "redirect:/view/medicamentos/detalle/" + producto.getIdProducto();
    }

    // EDITAR
    @GetMapping("/view/medicamentos/edit/{id}")
    public String edit(@PathVariable Integer id, Model model) {
        Productos producto = productosRepository.findById(id).orElse(null);
        if (producto == null) return "redirect:/view/medicamentos";

        model.addAttribute("producto", producto);
        cargarListasApoyo(model);
        return "medicamentos/medicamentosForm";
    }

    // ELIMINAR
    @PostMapping("/view/medicamentos/delete/{id}")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        productosRepository.deleteById(id);
        ra.addFlashAttribute("mensaje", "Medicamento eliminado");
        return "redirect:/view/medicamentos";
    }

    private void cargarListasApoyo(Model model) {
        model.addAttribute("categorias", categoriasRepository.findAll());
        model.addAttribute("proveedores", proveedoresRepository.findAll());
    }

    /**
     * Valida que no se dupliquen medicamentos en el catálogo general.
     * En particular, evita duplicar un producto por lote, orientando al usuario
     * a registrar una nueva remesa en el producto existente.
     */
    private String validarDuplicado(Productos producto) {
        if (StringUtils.hasText(producto.getCodigoBarras())) {
            Optional<Productos> porBarras = productosRepository.findByCodigoBarras(producto.getCodigoBarras().trim());
            if (porBarras.isPresent() && !porBarras.get().getIdProducto().equals(producto.getIdProducto())) {
                return "Ya existe un medicamento registrado con el código de barras '" + producto.getCodigoBarras() + "'.";
            }
        }

        if (StringUtils.hasText(producto.getNombreComercial())) {
            String nomNorm = normalizar(producto.getNombreComercial());
            String concNorm = normalizar(producto.getConcentracion());

            for (Productos p : productosRepository.findAll()) {
                if (producto.getIdProducto() != null && p.getIdProducto().equals(producto.getIdProducto())) {
                    continue;
                }
                boolean mismoNombre = normalizar(p.getNombreComercial()).equals(nomNorm);
                boolean mismaConcentracion = normalizar(p.getConcentracion()).equals(concNorm);

                if (mismoNombre && mismaConcentracion) {
                    return "Ya existe el producto maestro '" + p.getNombreComercial() + (StringUtils.hasText(p.getConcentracion()) ? " (" + p.getConcentracion() + ")" : "") +
                           "'. No duplique productos por cada remesa o lote; ingrese a la ficha del medicamento y agregue el nuevo lote con su fecha de vencimiento.";
                }
            }
        }

        return null;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", "")
                .toLowerCase();
    }
}
