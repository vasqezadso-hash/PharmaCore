package com.pharmacore.pharmacore.view;

import com.pharmacore.pharmacore.model.*;
import com.pharmacore.pharmacore.repository.*;
import com.pharmacore.pharmacore.service.ReportesExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReportesView {

    @Autowired
    private VentasRepository ventasRepository;

    @Autowired
    private DetallesVentasRepository detallesVentasRepository;

    @Autowired
    private ComprasRepository comprasRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private ProductosRepository productosRepository;

    @Autowired
    private LotesRepository lotesRepository;

    @Autowired
    private ReportesExportService reportesExportService;

    private static final DateTimeFormatter FMT_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @GetMapping("/view/reportes")
    public String reportes(@RequestParam(required = false) String desde,
                            @RequestParam(required = false) String hasta,
                            Model model) {

        LocalDate fechaDesde = parseFecha(desde, LocalDate.now().withDayOfMonth(1));
        LocalDate fechaHasta = parseFecha(hasta, LocalDate.now());

        List<Ventas> ventasEnRango = obtenerVentasEnRango(fechaDesde, fechaHasta);
        BigDecimal totalIngresos = totalIngresos(ventasEnRango);
        BigDecimal ticketPromedio = ventasEnRango.isEmpty() ? BigDecimal.ZERO
                : totalIngresos.divide(BigDecimal.valueOf(ventasEnRango.size()), 2, RoundingMode.HALF_UP);

        List<Map<String, Object>> masVendidos = obtenerMasVendidos(ventasEnRango);
        List<Map<String, Object>> comprasPorProveedorList = obtenerComprasPorProveedor(fechaDesde, fechaHasta);
        List<Productos> stockBajo = obtenerStockBajo();
        List<Lotes> lotesVencidos = obtenerLotesPorEstado("VENCIDO");
        List<Lotes> lotesProximos = obtenerLotesPorEstado("PROXIMO_A_VENCER");

        Map<Integer, String> nombresProductos = productosRepository.findAll().stream()
                .collect(Collectors.toMap(Productos::getIdProducto, Productos::getNombreComercial));

        model.addAttribute("desde", fechaDesde);
        model.addAttribute("hasta", fechaHasta);
        model.addAttribute("ventasEnRango", ventasEnRango);
        model.addAttribute("totalVentas", ventasEnRango.size());
        model.addAttribute("totalIngresos", totalIngresos);
        model.addAttribute("ticketPromedio", ticketPromedio);
        model.addAttribute("masVendidos", masVendidos);
        model.addAttribute("comprasPorProveedor", comprasPorProveedorList);
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("lotesVencidos", lotesVencidos);
        model.addAttribute("lotesProximos", lotesProximos);
        model.addAttribute("nombresProductosLote", nombresProductos);

        return "reportes/reportes";
    }


    @GetMapping("/view/reportes/exportar/ventas/{formato}")
    public ResponseEntity<byte[]> exportarVentas(@RequestParam(required = false) String desde,
                                                  @RequestParam(required = false) String hasta,
                                                  @org.springframework.web.bind.annotation.PathVariable String formato) throws Exception {
        LocalDate fechaDesde = parseFecha(desde, LocalDate.now().withDayOfMonth(1));
        LocalDate fechaHasta = parseFecha(hasta, LocalDate.now());
        List<Ventas> ventas = obtenerVentasEnRango(fechaDesde, fechaHasta);

        List<String> encabezados = List.of("N° Factura", "Fecha", "Subtotal", "Descuento", "IVA", "Total", "Método de pago", "Estado");
        List<List<String>> filas = ventas.stream().map(v -> List.of(
                textoOr(v.getNumeroFactura()),
                v.getFechaVenta() != null ? v.getFechaVenta().format(FMT_FECHA_HORA) : "-",
                moneda(v.getSubtotal()),
                moneda(v.getDescuento()),
                moneda(v.getImpuestoIva()),
                moneda(v.getTotal()),
                textoOr(v.getMetodoPago()),
                textoOr(v.getEstado())
        )).collect(Collectors.toList());

        String subtitulo = "Del " + fechaDesde.format(FMT_FECHA) + " al " + fechaHasta.format(FMT_FECHA)
                + " — " + ventas.size() + " venta(s), total " + moneda(totalIngresos(ventas));
        String nombreArchivo = "reporte-ventas-" + fechaDesde + "_a_" + fechaHasta;
        return exportar(formato, "Ventas del período", subtitulo, encabezados, filas, nombreArchivo);
    }

    @GetMapping("/view/reportes/exportar/masvendidos/{formato}")
    public ResponseEntity<byte[]> exportarMasVendidos(@RequestParam(required = false) String desde,
                                                       @RequestParam(required = false) String hasta,
                                                       @org.springframework.web.bind.annotation.PathVariable String formato) throws Exception {
        LocalDate fechaDesde = parseFecha(desde, LocalDate.now().withDayOfMonth(1));
        LocalDate fechaHasta = parseFecha(hasta, LocalDate.now());
        List<Ventas> ventas = obtenerVentasEnRango(fechaDesde, fechaHasta);
        List<Map<String, Object>> masVendidos = obtenerMasVendidos(ventas);

        List<String> encabezados = List.of("Producto", "Unidades vendidas");
        List<List<String>> filas = masVendidos.stream().map(m -> List.of(
                String.valueOf(m.get("producto")),
                String.valueOf(m.get("cantidad"))
        )).collect(Collectors.toList());

        String subtitulo = "Del " + fechaDesde.format(FMT_FECHA) + " al " + fechaHasta.format(FMT_FECHA) + " — Top 10";
        String nombreArchivo = "reporte-mas-vendidos-" + fechaDesde + "_a_" + fechaHasta;
        return exportar(formato, "Productos más vendidos", subtitulo, encabezados, filas, nombreArchivo);
    }

    @GetMapping("/view/reportes/exportar/compras-proveedor/{formato}")
    public ResponseEntity<byte[]> exportarComprasPorProveedor(@RequestParam(required = false) String desde,
                                                                @RequestParam(required = false) String hasta,
                                                                @org.springframework.web.bind.annotation.PathVariable String formato) throws Exception {
        LocalDate fechaDesde = parseFecha(desde, LocalDate.now().withDayOfMonth(1));
        LocalDate fechaHasta = parseFecha(hasta, LocalDate.now());
        List<Map<String, Object>> lista = obtenerComprasPorProveedor(fechaDesde, fechaHasta);

        List<String> encabezados = List.of("Proveedor", "Total comprado");
        List<List<String>> filas = lista.stream().map(c -> List.of(
                String.valueOf(c.get("proveedor")),
                moneda((BigDecimal) c.get("total"))
        )).collect(Collectors.toList());

        String subtitulo = "Del " + fechaDesde.format(FMT_FECHA) + " al " + fechaHasta.format(FMT_FECHA);
        String nombreArchivo = "reporte-compras-proveedor-" + fechaDesde + "_a_" + fechaHasta;
        return exportar(formato, "Compras por proveedor", subtitulo, encabezados, filas, nombreArchivo);
    }

    @GetMapping("/view/reportes/exportar/stock-bajo/{formato}")
    public ResponseEntity<byte[]> exportarStockBajo(@org.springframework.web.bind.annotation.PathVariable String formato) throws Exception {
        List<Productos> stockBajo = obtenerStockBajo();

        List<String> encabezados = List.of("Producto", "Stock actual", "Stock mínimo");
        List<List<String>> filas = stockBajo.stream().map(p -> List.of(
                textoOr(p.getNombreComercial()),
                String.valueOf(p.getStockTotal()),
                String.valueOf(p.getStockMinimo())
        )).collect(Collectors.toList());

        String subtitulo = "Estado actual del inventario — " + stockBajo.size() + " producto(s) en stock bajo";
        String nombreArchivo = "reporte-stock-bajo-" + LocalDate.now();
        return exportar(formato, "Productos con stock bajo", subtitulo, encabezados, filas, nombreArchivo);
    }

    @GetMapping("/view/reportes/exportar/vencimientos/{formato}")
    public ResponseEntity<byte[]> exportarVencimientos(@org.springframework.web.bind.annotation.PathVariable String formato) throws Exception {
        List<Lotes> vencidos = obtenerLotesPorEstado("VENCIDO");
        List<Lotes> proximos = obtenerLotesPorEstado("PROXIMO_A_VENCER");
        Map<Integer, String> nombresProductos = productosRepository.findAll().stream()
                .collect(Collectors.toMap(Productos::getIdProducto, Productos::getNombreComercial));

        List<String> encabezados = List.of("Lote", "Producto", "Fecha de vencimiento", "Estado");
        List<List<String>> filas = new ArrayList<>();
        for (Lotes l : vencidos) {
            filas.add(List.of(textoOr(l.getNumeroLote()),
                    nombresProductos.getOrDefault(l.getIdProducto(), "Producto #" + l.getIdProducto()),
                    l.getFechaVencimiento() != null ? l.getFechaVencimiento().format(FMT_FECHA) : "-",
                    "VENCIDO"));
        }
        for (Lotes l : proximos) {
            filas.add(List.of(textoOr(l.getNumeroLote()),
                    nombresProductos.getOrDefault(l.getIdProducto(), "Producto #" + l.getIdProducto()),
                    l.getFechaVencimiento() != null ? l.getFechaVencimiento().format(FMT_FECHA) : "-",
                    "PRÓXIMO A VENCER"));
        }

        String subtitulo = vencidos.size() + " lote(s) vencido(s), " + proximos.size() + " próximo(s) a vencer";
        String nombreArchivo = "reporte-vencimientos-" + LocalDate.now();
        return exportar(formato, "Lotes vencidos y próximos a vencer", subtitulo, encabezados, filas, nombreArchivo);
    }


    private List<Ventas> obtenerVentasEnRango(LocalDate fechaDesde, LocalDate fechaHasta) {
        LocalDateTime desdeDT = fechaDesde.atStartOfDay();
        LocalDateTime hastaDT = fechaHasta.atTime(23, 59, 59);
        return ventasRepository.findAll().stream()
                .filter(v -> "PAGADA".equals(v.getEstado()))
                .filter(v -> v.getFechaVenta() != null
                        && !v.getFechaVenta().isBefore(desdeDT) && !v.getFechaVenta().isAfter(hastaDT))
                .sorted(Comparator.comparing(Ventas::getFechaVenta).reversed())
                .collect(Collectors.toList());
    }

    private BigDecimal totalIngresos(List<Ventas> ventas) {
        return ventas.stream()
                .map(v -> v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> obtenerMasVendidos(List<Ventas> ventasEnRango) {
        Set<Long> idsVentasEnRango = ventasEnRango.stream().map(Ventas::getIdVenta).collect(Collectors.toSet());
        Map<Integer, Integer> cantidadPorProducto = detallesVentasRepository.findAll().stream()
                .filter(d -> d.getVenta() != null && idsVentasEnRango.contains(d.getVenta().getIdVenta()))
                .collect(Collectors.groupingBy(DetallesVentas::getIdProducto, Collectors.summingInt(DetallesVentas::getCantidad)));

        Map<Integer, String> nombresProductos = productosRepository.findAll().stream()
                .collect(Collectors.toMap(Productos::getIdProducto, Productos::getNombreComercial));

        return cantidadPorProducto.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> Map.<String, Object>of(
                        "producto", nombresProductos.getOrDefault(e.getKey(), "Producto #" + e.getKey()),
                        "cantidad", e.getValue()))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> obtenerComprasPorProveedor(LocalDate fechaDesde, LocalDate fechaHasta) {
        LocalDateTime desdeDT = fechaDesde.atStartOfDay();
        LocalDateTime hastaDT = fechaHasta.atTime(23, 59, 59);
        Map<Integer, String> nombresProveedores = proveedoresRepository.findAll().stream()
                .collect(Collectors.toMap(Proveedores::getId_proveedor, Proveedores::getRazon_social));

        Map<Integer, BigDecimal> comprasPorProveedor = comprasRepository.findAll().stream()
                .filter(c -> c.getFechaCompra() != null
                        && !c.getFechaCompra().isBefore(desdeDT) && !c.getFechaCompra().isAfter(hastaDT))
                .collect(Collectors.groupingBy(Compras::getIdProveedor,
                        Collectors.reducing(BigDecimal.ZERO, c -> c.getTotal() != null ? c.getTotal() : BigDecimal.ZERO, BigDecimal::add)));

        return comprasPorProveedor.entrySet().stream()
                .sorted(Map.Entry.<Integer, BigDecimal>comparingByValue().reversed())
                .map(e -> Map.<String, Object>of(
                        "proveedor", nombresProveedores.getOrDefault(e.getKey(), "Proveedor #" + e.getKey()),
                        "total", e.getValue()))
                .collect(Collectors.toList());
    }

    private List<Productos> obtenerStockBajo() {
        return productosRepository.findAll().stream()
                .filter(Productos::isStockBajo)
                .sorted(Comparator.comparing(Productos::getStockTotal))
                .collect(Collectors.toList());
    }

    private List<Lotes> obtenerLotesPorEstado(String estado) {
        return lotesRepository.findAll().stream()
                .filter(l -> estado.equals(l.getEstadoCalculado()))
                .collect(Collectors.toList());
    }

    private ResponseEntity<byte[]> exportar(String formato, String titulo, String subtitulo,
                                             List<String> encabezados, List<List<String>> filas,
                                             String nombreArchivoSinExtension) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        byte[] contenido;
        String extension;

        if ("excel".equalsIgnoreCase(formato) || "xlsx".equalsIgnoreCase(formato)) {
            contenido = reportesExportService.generarExcel(titulo, encabezados, filas);
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            extension = ".xlsx";
        } else if ("pdf".equalsIgnoreCase(formato)) {
            contenido = reportesExportService.generarPdf(titulo, subtitulo, encabezados, filas);
            headers.setContentType(MediaType.APPLICATION_PDF);
            extension = ".pdf";
        } else {
            return ResponseEntity.badRequest().build();
        }

        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(nombreArchivoSinExtension + extension).build());
        return ResponseEntity.ok().headers(headers).body(contenido);
    }

    private String textoOr(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private String moneda(BigDecimal valor) {
        if (valor == null) valor = BigDecimal.ZERO;
        return "$" + String.format("%,.0f", valor);
    }

    private LocalDate parseFecha(String valor, LocalDate porDefecto) {
        if (valor == null || valor.isBlank()) return porDefecto;
        try {
            return LocalDate.parse(valor);
        } catch (Exception e) {
            return porDefecto;
        }
    }
}
