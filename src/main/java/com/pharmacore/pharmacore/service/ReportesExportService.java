package com.pharmacore.pharmacore.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportesExportService {

    private static final Color VERDE_PHARMACORE = new Color(0, 101, 101); // #006565
    public static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generarPdf(String titulo, String subtitulo, List<String> encabezados, List<List<String>> filas)
            throws DocumentException {

        Document documento = new Document(PageSize.A4.rotate(), 30, 30, 50, 40);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfWriter.getInstance(documento, salida);
        documento.open();

        Font fuenteTitulo = new Font(Font.HELVETICA, 18, Font.BOLD, VERDE_PHARMACORE);
        Font fuenteSubtitulo = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.GRAY);
        Font fuenteEncabezadoTabla = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font fuenteCelda = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
        Font fuentePie = new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY);

        documento.add(new Paragraph("PHARMACORE", fuenteTitulo));
        documento.add(new Paragraph(titulo, new Font(Font.HELVETICA, 13, Font.BOLD, Color.DARK_GRAY)));
        if (subtitulo != null && !subtitulo.isBlank()) {
            documento.add(new Paragraph(subtitulo, fuenteSubtitulo));
        }
        documento.add(new Paragraph(" "));

        if (filas.isEmpty()) {
            documento.add(new Paragraph("No hay datos para este reporte en el período/criterio seleccionado.", fuenteSubtitulo));
        } else {
            float[] anchos = new float[encabezados.size()];
            java.util.Arrays.fill(anchos, 1f);
            PdfPTable tabla = new PdfPTable(anchos);
            tabla.setWidthPercentage(100);

            for (String encabezado : encabezados) {
                PdfPCell celda = new PdfPCell(new Phrase(encabezado, fuenteEncabezadoTabla));
                celda.setBackgroundColor(VERDE_PHARMACORE);
                celda.setPadding(6);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(celda);
            }
            tabla.setHeaderRows(1);

            boolean parImpar = false;
            for (List<String> fila : filas) {
                Color fondo = parImpar ? new Color(0xF7, 0xFA, 0xF9) : Color.WHITE;
                for (String valor : fila) {
                    PdfPCell celda = new PdfPCell(new Phrase(valor != null ? valor : "-", fuenteCelda));
                    celda.setPadding(5);
                    celda.setBackgroundColor(fondo);
                    celda.setBorderColor(new Color(0xE2, 0xE8, 0xF0));
                    tabla.addCell(celda);
                }
                parImpar = !parImpar;
            }
            documento.add(tabla);
        }

        Paragraph pie = new Paragraph("\nReporte generado por el sistema PharmaCore.", fuentePie);
        pie.setAlignment(Element.ALIGN_CENTER);
        documento.add(pie);

        documento.close();
        return salida.toByteArray();
    }

    public byte[] generarExcel(String tituloHoja, List<String> encabezados, List<List<String>> filas) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            String nombreHoja = tituloHoja.replaceAll("[\\\\/*?:\\[\\]]", " ");
            if (nombreHoja.length() > 31) nombreHoja = nombreHoja.substring(0, 31);
            Sheet hoja = workbook.createSheet(nombreHoja);

            XSSFCellStyle estiloEncabezado = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(new XSSFColor(
                    new byte[]{0, 101, 101}, null));
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);

            XSSFCellStyle estiloFilaPar = workbook.createCellStyle();
            estiloFilaPar.setFillForegroundColor(new XSSFColor(
                    new byte[]{(byte) 0xF7, (byte) 0xFA, (byte) 0xF9}, null));
            estiloFilaPar.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row filaEncabezado = hoja.createRow(0);
            for (int col = 0; col < encabezados.size(); col++) {
                Cell celda = filaEncabezado.createCell(col);
                celda.setCellValue(encabezados.get(col));
                celda.setCellStyle(estiloEncabezado);
            }

            int numeroFila = 1;
            for (List<String> fila : filas) {
                Row filaExcel = hoja.createRow(numeroFila);
                boolean esPar = numeroFila % 2 == 0;
                for (int col = 0; col < fila.size(); col++) {
                    Cell celda = filaExcel.createCell(col);
                    celda.setCellValue(fila.get(col) != null ? fila.get(col) : "");
                    if (esPar) celda.setCellStyle(estiloFilaPar);
                }
                numeroFila++;
            }

            for (int col = 0; col < encabezados.size(); col++) {
                hoja.autoSizeColumn(col);
                if (hoja.getColumnWidth(col) < 3000) hoja.setColumnWidth(col, 3000);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            workbook.write(salida);
            return salida.toByteArray();
        }
    }
}