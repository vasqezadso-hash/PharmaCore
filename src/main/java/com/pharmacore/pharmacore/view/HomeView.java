package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.DetallesVentas;
import com.pharmacore.pharmacore.model.Lotes;
import com.pharmacore.pharmacore.model.Productos;
import com.pharmacore.pharmacore.model.Ventas;
import com.pharmacore.pharmacore.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Controller
public class HomeView {

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private VentasRepository ventasRepository;

    @Autowired
    private DetallesVentasRepository detallesVentasRepository;

    @Autowired
    private LotesRepository lotesRepository;

    @Autowired
    private ClientesRepository clientesRepository;

    @GetMapping({"/", "/view/home"})
    public String home(Model model) {
        LocalDate hoy = LocalDate.now();

        long totalProductos = productosRepository.count();
        long totalClientes = clientesRepository.count();
        long totalVentas = ventasRepository.count();

        // ---- Lotes (contadores + gráfica de estado) ----
        List<Lotes> lotes = lotesRepository.findAll();
        long lotesVencidos = lotes.stream().filter(Lotes::isVencido).count();
        long lotesPorVencer = lotes.stream()
                .filter(l -> "PROXIMO_A_VENCER".equals(l.getEstadoCalculado())).count();
        long lotesDisponibles = lotes.stream()
                .filter(l -> "DISPONIBLE".equals(l.getEstadoCalculado())).count();
        long lotesAgotados = lotes.stream()
                .filter(l -> "AGOTADO".equals(l.getEstadoCalculado())).count();

        // Solo cuento las ventas PAGADAS: una venta ANULADA no es dinero que entró.
        List<Ventas> ventasPagadas = ventasRepository.findAll().stream()
                .filter(v -> "PAGADA".equals(v.getEstado()) && v.getFechaVenta() != null)
                .collect(Collectors.toList());

        // ---- KPIs de dinero: hoy y mes actual ----
        double ventasHoy = ventasPagadas.stream()
                .filter(v -> v.getFechaVenta().toLocalDate().equals(hoy))
                .mapToDouble(v -> valor(v.getTotal())).sum();
        double ventasMes = ventasPagadas.stream()
                .filter(v -> v.getFechaVenta().getYear() == hoy.getYear()
                        && v.getFechaVenta().getMonth() == hoy.getMonth())
                .mapToDouble(v -> valor(v.getTotal())).sum();

        // ---- Gráfica 1: ventas de los últimos 7 días (incluye los días sin ventas en 0) ----
        DateTimeFormatter fmtDia = DateTimeFormatter.ofPattern("dd/MM");
        List<String> diasLabels = new ArrayList<>();
        List<Double> diasValores = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            diasLabels.add(dia.format(fmtDia));
            diasValores.add(ventasPagadas.stream()
                    .filter(v -> v.getFechaVenta().toLocalDate().equals(dia))
                    .mapToDouble(v -> valor(v.getTotal())).sum());
        }
        List<Map<String, Object>> ventasDias = barras(diasLabels, diasValores, false);

        // ---- Gráfica 2: top 5 productos más vendidos (unidades) ----
        Set<Long> idsVentasPagadas = ventasPagadas.stream().map(Ventas::getIdVenta).collect(Collectors.toSet());
        Map<Integer, Integer> unidadesPorProducto = detallesVentasRepository.findAll().stream()
                .filter(d -> d.getVenta() != null && idsVentasPagadas.contains(d.getVenta().getIdVenta()))
                .collect(Collectors.groupingBy(DetallesVentas::getIdProducto,
                        Collectors.summingInt(d -> d.getCantidad() != null ? d.getCantidad() : 0)));

        List<Productos> todosProductos = productosRepository.findAll();
        Map<Integer, String> nombres = todosProductos.stream()
                .collect(Collectors.toMap(Productos::getIdProducto, Productos::getNombreComercial));

        List<Map.Entry<Integer, Integer>> top = unidadesPorProducto.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(5).collect(Collectors.toList());
        List<Map<String, Object>> topProductos = barras(
                top.stream().map(e -> nombres.getOrDefault(e.getKey(), "Producto #" + e.getKey())).collect(Collectors.toList()),
                top.stream().map(Map.Entry::getValue).collect(Collectors.toList()), false);

        // ---- Gráfica 3: ventas por método de pago (el % es la parte del total de ventas) ----
        Map<String, Long> porMetodo = ventasPagadas.stream()
                .collect(Collectors.groupingBy(v -> v.getMetodoPago() != null ? v.getMetodoPago() : "SIN_DATO",
                        TreeMap::new, Collectors.counting()));
        List<Map<String, Object>> metodosPago = barras(
                porMetodo.keySet().stream().map(m -> m.replace("_", " ")).collect(Collectors.toList()),
                new ArrayList<Long>(porMetodo.values()), true);

        // ---- Gráfica 4: estado de los lotes (el % es la parte del total de lotes) ----
        List<Map<String, Object>> lotesEstado = barras(
                List.of("Disponibles", "Próximos a vencer", "Vencidos", "Agotados"),
                List.of(lotesDisponibles, lotesPorVencer, lotesVencidos, lotesAgotados), true);

        // ---- Gráfica 5: productos con stock más bajo respecto a su mínimo ----
        // El % es stock actual / stock mínimo (tope 100): mientras más corta la barra, más urgente.
        List<Map<String, Object>> stockBajo = todosProductos.stream()
                .filter(Productos::isStockBajo)
                .sorted(Comparator.comparing(Productos::getStockTotal))
                .limit(6)
                .map(p -> {
                    int minimo = p.getStockMinimo() != null ? p.getStockMinimo() : 0;
                    int pct = minimo > 0 ? Math.min(100, p.getStockTotal() * 100 / minimo) : 0;
                    Map<String, Object> m = new HashMap<>();
                    m.put("label", p.getNombreComercial());
                    m.put("actual", p.getStockTotal());
                    m.put("minimo", minimo);
                    m.put("pct", pct);
                    return m;
                }).collect(Collectors.toList());

        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalVentas", totalVentas);
        model.addAttribute("lotesVencidos", lotesVencidos);
        model.addAttribute("lotesPorVencer", lotesPorVencer);
        model.addAttribute("ventasHoy", ventasHoy);
        model.addAttribute("ventasMes", ventasMes);

        model.addAttribute("ventasDias", ventasDias);
        model.addAttribute("topProductos", topProductos);
        model.addAttribute("metodosPago", metodosPago);
        model.addAttribute("lotesEstado", lotesEstado);
        model.addAttribute("stockBajo", stockBajo);

        return "home/home";
    }

    private double valor(BigDecimal b) {
        return b != null ? b.doubleValue() : 0.0;
    }

    private List<Map<String, Object>> barras(List<String> labels, List<? extends Number> valores, boolean porTotal) {
        double base = porTotal
                ? valores.stream().mapToDouble(Number::doubleValue).sum()
                : valores.stream().mapToDouble(Number::doubleValue).max().orElse(0);
        List<Map<String, Object>> filas = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            double v = valores.get(i).doubleValue();
            Map<String, Object> fila = new HashMap<>();
            fila.put("label", labels.get(i));
            fila.put("valor", valores.get(i));
            fila.put("pct", base > 0 ? (int) Math.round(v * 100 / base) : 0);
            filas.add(fila);
        }
        return filas;
    }
}
