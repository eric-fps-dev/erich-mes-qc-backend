package com.fps.svmes.controllers;

import com.fps.svmes.dto.requests.ReportRequest;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;


import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;

@RestController
public class PdfController {

    private final TemplateEngine templateEngine;

    public PdfController(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @PostMapping("/generate-pdf-with-chart")
    public void generatePdfWithChart(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String chartBase64 = request.get("chartImage");

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=chart_report.pdf");

        Document document = new Document();

        try (OutputStream outputStream = response.getOutputStream()) {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Load Chinese Font for Text
            InputStream fontStream = new ClassPathResource("fonts/simsun.ttc").getInputStream();
            BaseFont baseFont = BaseFont.createFont("simsun.ttc,0", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontStream.readAllBytes(), null);
            Font chineseFont = new Font(baseFont, 12, Font.NORMAL);

            // Add title with Chinese font
            document.add(new Paragraph("ECharts PDF 报告", new Font(baseFont, 18, Font.BOLD, BaseColor.BLUE)));
            document.add(Chunk.NEWLINE);

            // Add the chart image if available
            if (chartBase64 != null && !chartBase64.isEmpty()) {
                byte[] chartBytes = Base64.getDecoder().decode(chartBase64.split(",")[1]);
                Image chartImage = Image.getInstance(chartBytes);
                chartImage.scaleToFit(500, 300);
                chartImage.setAlignment(Element.ALIGN_CENTER);
                document.add(chartImage);
            }

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("此报告包含 ECharts 生成的图表及数据概述。", chineseFont));

            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Error generating PDF with chart", e);
        }
    }

    @GetMapping("/generate-qc-report")
    public void generateQcReport(HttpServletResponse response) {
        try {
            // Sample dynamic data
            Map<String, Object> data = new HashMap<>();
            data.put("date", "2025-02-20");
            data.put("inspector", "马晓梅");
            data.put("phosphorusContent", "15 mg/L");
            data.put("phValue", "7.8");
            data.put("moisture", "12%");

            // Render Thymeleaf template
            Context context = new Context();
            context.setVariables(data);
            String htmlContent = templateEngine.process("qc_report_en", context);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=qc_report.pdf");

            // Load Chinese font for HTML-to-PDF
            FontProvider fontProvider = new FontProvider();
            try (InputStream fontStream = new ClassPathResource("fonts/simsun.ttc").getInputStream()) {
                // Specify ",0" to select the first font inside the TTC collection
                PdfFont chineseFont = PdfFontFactory.createFont("fonts/simsun.ttc,0", "Identity-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                fontProvider.addFont(chineseFont.getFontProgram());
            }

            // Convert HTML to PDF with proper fonts
            try (OutputStream os = response.getOutputStream()) {
                ConverterProperties properties = new ConverterProperties();
                properties.setFontProvider(fontProvider);
                HtmlConverter.convertToPdf(htmlContent, os, properties);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error generating QC report", e);
        }
    }

    @PostMapping(value = "/generate", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateReport(
            @RequestBody ReportRequest request,
            @RequestParam(defaultValue = "en-US") String language
    ) {
        try {
            // 1️⃣ Process charts and save images as temporary files
            ArrayList<Object> imagePaths = new ArrayList<>();

            for (var chart : request.getCharts()) {
                if (chart.getChartImage() != null && !chart.getChartImage().isEmpty()) {
                    try {
                        String base64String = chart.getChartImage();

                        // Validate Base64 format
                        if (!base64String.startsWith("data:image/png;base64,")) {
                            System.err.println("Invalid base64 format: " + base64String);
                            chart.setChartImage(null);
                            continue;
                        }

                        // Extract actual Base64 data
                        String base64Data = base64String.substring(base64String.indexOf(",") + 1);
                        if (base64Data.isEmpty()) {
                            System.err.println("Base64 data is empty.");
                            chart.setChartImage(null);
                            continue;
                        }

                        // Decode and save as temporary file
                        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
                        Path tempFile = Files.createTempFile("chart_", ".png");
                        Files.write(tempFile, decodedBytes);

                        // ✅ Store absolute file path instead of base64 string
                        String absolutePath = tempFile.toAbsolutePath().toString().replace("\\", "/");
                        String imageUrl = "file:///" + absolutePath; // 注意是三个斜杠 file:///
                        chart.setChartImage(imageUrl);

                        // ✅ Store file paths for cleanup
                        imagePaths.add(absolutePath);

                    } catch (IOException e) {
                        System.err.println("Error processing chart image: " + e.getMessage());
                        chart.setChartImage(null);
                    }
                }
            }

            // 2️⃣ Render HTML using Thymeleaf
            Context context = new Context();
            context.setVariable("qcFormName", request.getQcFormName());
            context.setVariable("startDateTime", request.getStartDateTime());
            context.setVariable("endDateTime", request.getEndDateTime());
            context.setVariable("charts", request.getCharts());

            String templateName = "zh-CN".equalsIgnoreCase(language) ? "qc_report_cn" : "qc_report_en";
            String htmlContent = renderHtml(templateName, context);

            // 3️⃣ Convert HTML to PDF (pass imagePaths for cleanup)
            byte[] pdfBytes = convertHtmlToPdf(htmlContent, imagePaths);

            // 4️⃣ Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename("Quality_Report.pdf").build());

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Method to render HTML using Thymeleaf
    private String renderHtml(String templateName, Context context) {
        return templateEngine.process(templateName, context);
    }

    // Convert HTML to PDF using iText with proper Chinese font support
    private byte[] convertHtmlToPdf(String htmlContent, ArrayList<Object> imagePaths) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // Load Chinese font
            FontProvider fontProvider = new FontProvider();
            try (InputStream fontStream = new ClassPathResource("fonts/simsun.ttc").getInputStream()) {
                PdfFont chineseFont = PdfFontFactory.createFont("fonts/simsun.ttc,0", "Identity-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                fontProvider.addFont(chineseFont.getFontProgram());
            }

            // Set up PDF conversion properties
            ConverterProperties properties = new ConverterProperties();
            properties.setFontProvider(fontProvider);

            // ✅ Set **base URI to root directory**
            properties.setBaseUri("/");

            // Convert HTML to PDF
            HtmlConverter.convertToPdf(htmlContent, outputStream, properties);

        } catch (Exception e) {
            throw new RuntimeException("Error converting HTML to PDF", e);
        } finally {
            // ✅ Delete temporary image files after PDF generation
            for (Object path : imagePaths) {
                try {
                    Files.deleteIfExists(Path.of((String) path));
                } catch (IOException e) {
                    System.err.println("Failed to delete temporary image: " + path);
                }
            }
        }

        return outputStream.toByteArray();
    }

}
