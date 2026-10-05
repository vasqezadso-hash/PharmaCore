package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.repository.CategoriasRepository;
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

@Controller
public class ProductosView {

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private CategoriasRepository categoriasRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @GetMapping("/view/medicamentos")
    public String lista(@RequestParam(value = "q", required = false) String q, Model model) {
        if (StringUtils.hasText(q)) {
            model.addAttribute("productos", productosRepository
                    .findByNombreComercialContainingIgnoreCaseOrNombreGenericoContainingIgnoreCaseOrCodigoInternoContainingIgnoreCase(q, q, q));
        } else {
            model.addAttribute("productos", productosRepository.findAll());
        }
        model.addAttribute("q", q == null ? "" : q);
        return "medicamentos/medicamentos";
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

        if (existeDuplicado(producto)) {
            model.addAttribute("error", "Ya existe un medicamento con ese código interno o código de barras.");
            cargarListasApoyo(model);
            return "medicamentos/medicamentosForm";
        }

        productosRepository.save(producto);
        ra.addFlashAttribute("mensaje", "Medicamento guardado con éxito");
        return "redirect:/view/medicamentos";
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

    private boolean existeDuplicado(Productos producto) {
        return productosRepository.findByCodigoInterno(producto.getCodigoInterno())
                .filter(p -> !p.getIdProducto().equals(producto.getIdProducto()))
                .isPresent()
                || productosRepository.findByCodigoBarras(producto.getCodigoBarras())
                .filter(p -> !p.getIdProducto().equals(producto.getIdProducto()))
                .isPresent();
    }
}
