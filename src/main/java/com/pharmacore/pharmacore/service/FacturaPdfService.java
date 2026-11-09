package com.pharmacore.pharmacore.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pharmacore.pharmacore.model.DetallesVentas;
import com.pharmacore.pharmacore.model.Ventas;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Genera la factura de una venta en PDF (OpenPDF).
 * arma el documento directamente en memoria y lo devuelve como bytes, para que
 * entregue como descarga
 */
@Service
public class FacturaPdfService {

    private static final Color VERDE_PHARMACORE = new Color(0, 101, 101); // #006565
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarFacturaPdf(Ventas venta,
                                     List<DetallesVentas> detalles,
                                     Map<Integer, String> nombresProductos,
                                     Map<Integer, String> nombresLotes,
                                     String nombreCliente,
                                     String nombreEmpleado) throws DocumentException, IOException {

        Document documento = new Document(PageSize.A4, 40, 40, 50, 40);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfWriter.getInstance(documento, salida);
        documento.open();

        Font fuenteTitulo = new Font(Font.HELVETICA, 20, Font.BOLD, VERDE_PHARMACORE);
        Font fuenteSubtitulo = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font fuenteEtiqueta = new Font(Font.HELVETICA, 8, Font.BOLD, Color.GRAY);
        Font fuenteValor = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
        Font fuenteEncabezadoTabla = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font fuenteCelda = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
        Font fuenteTotal = new Font(Font.HELVETICA, 12, Font.BOLD, VERDE_PHARMACORE);

        // Encabezado
        Paragraph titulo = new Paragraph("PHARMACORE", fuenteTitulo);
        documento.add(titulo);
        documento.add(new Paragraph("Sistema de gestión de farmacia — Factura de venta", fuenteSubtitulo));
        documento.add(new Paragraph(" "));

        // Datos de la factura
        PdfPTable datosGenerales = new PdfPTable(2);
        datosGenerales.setWidthPercentage(100);
        datosGenerales.setSpacingAfter(15);
        agregarDato(datosGenerales, "N° FACTURA", venta.getNumeroFactura(), fuenteEtiqueta, fuenteValor);
        agregarDato(datosGenerales, "FECHA", venta.getFechaVenta().format(FMT_FECHA), fuenteEtiqueta, fuenteValor);
        agregarDato(datosGenerales, "CLIENTE", nombreCliente != null ? nombreCliente : "Sin cliente registrado", fuenteEtiqueta, fuenteValor);
        agregarDato(datosGenerales, "ATENDIÓ", nombreEmpleado != null ? nombreEmpleado : "-", fuenteEtiqueta, fuenteValor);
        agregarDato(datosGenerales, "MÉTODO DE PAGO", venta.getMetodoPago(), fuenteEtiqueta, fuenteValor);
        agregarDato(datosGenerales, "ESTADO", venta.getEstado(), fuenteEtiqueta, fuenteValor);
        documento.add(datosGenerales);

        // Tabla de productos
        PdfPTable tabla = new PdfPTable(new float[]{3.2f, 1.6f, 1f, 1.4f, 1.4f});
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(5);

        agregarEncabezado(tabla, "Producto", fuenteEncabezadoTabla);
        agregarEncabezado(tabla, "Lote", fuenteEncabezadoTabla);
        agregarEncabezado(tabla, "Cant.", fuenteEncabezadoTabla);
        agregarEncabezado(tabla, "Precio Unit.", fuenteEncabezadoTabla);
        agregarEncabezado(tabla, "Subtotal", fuenteEncabezadoTabla);

        for (DetallesVentas d : detalles) {
            String nombreProducto = nombresProductos.getOrDefault(d.getIdProducto(), "Producto #" + d.getIdProducto());
            String numeroLote = nombresLotes.getOrDefault(d.getIdLote(), "Lote #" + d.getIdLote());
            agregarCelda(tabla, nombreProducto, fuenteCelda, Element.ALIGN_LEFT);
            agregarCelda(tabla, numeroLote, fuenteCelda, Element.ALIGN_LEFT);
            String cantTexto = d.getCantidad() + (d.getFormato() != null && !d.getFormato().isBlank() ? " (" + d.getFormato().toLowerCase() + ")" : "");
            agregarCelda(tabla, cantTexto, fuenteCelda, Element.ALIGN_CENTER);
            agregarCelda(tabla, formatearMoneda(d.getPrecioUnitario()), fuenteCelda, Element.ALIGN_RIGHT);
            agregarCelda(tabla, formatearMoneda(d.getSubtotal()), fuenteCelda, Element.ALIGN_RIGHT);
        }
        documento.add(tabla);

        // Totales
        PdfPTable totales = new PdfPTable(2);
        totales.setWidthPercentage(45);
        totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totales.setSpacingBefore(15);
        agregarTotal(totales, "Subtotal", formatearMoneda(venta.getSubtotal()), fuenteValor, false);
        agregarTotal(totales, "Descuento", formatearMoneda(venta.getDescuento()), fuenteValor, false);
        agregarTotal(totales, "IVA", formatearMoneda(venta.getImpuestoIva()), fuenteValor, false);
        agregarTotal(totales, "TOTAL", formatearMoneda(venta.getTotal()), fuenteTotal, true);
        documento.add(totales);

        // Pie de página
        Paragraph pie = new Paragraph("\n\nEste documento es un comprobante generado por el sistema PharmaCore.", fuenteSubtitulo);
        pie.setAlignment(Element.ALIGN_CENTER);
        documento.add(pie);

        documento.close();
        return salida.toByteArray();
    }

    private void agregarDato(PdfPTable tabla, String etiqueta, String valor, Font fuenteEtiqueta, Font fuenteValor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, fuenteEtiqueta));
        celdaEtiqueta.setBorder(Rectangle.NO_BORDER);
        celdaEtiqueta.setPaddingBottom(2);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, fuenteValor));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        celdaValor.setPaddingBottom(6);
        tabla.addCell(celdaValor);
    }

    private void agregarEncabezado(PdfPTable tabla, String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setBackgroundColor(VERDE_PHARMACORE);
        celda.setPadding(6);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(celda);
    }

    private void agregarCelda(PdfPTable tabla, String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setPadding(5);
        celda.setHorizontalAlignment(alineacion);
        tabla.addCell(celda);
    }

    private void agregarTotal(PdfPTable tabla, String etiqueta, String valor, Font fuente, boolean destacado) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, fuente));
        celdaEtiqueta.setBorder(destacado ? Rectangle.TOP : Rectangle.NO_BORDER);
        celdaEtiqueta.setPadding(4);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, fuente));
        celdaValor.setBorder(destacado ? Rectangle.TOP : Rectangle.NO_BORDER);
        celdaValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
        celdaValor.setPadding(4);
        tabla.addCell(celdaValor);
    }

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) valor = BigDecimal.ZERO;
        return "$" + String.format("%,.0f", valor);
    }
}
