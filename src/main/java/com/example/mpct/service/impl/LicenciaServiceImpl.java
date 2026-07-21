package com.example.mpct.service.impl;

import com.example.mpct.service.LicenciaService;
import com.example.mpct.model.entity.Licencia;
import com.example.mpct.model.entity.Tramite;
import com.example.mpct.repository.LicenciaRepository;
import com.example.mpct.repository.TramiteRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Random;
import org.springframework.core.io.ClassPathResource;

@Service
@RequiredArgsConstructor
public class LicenciaServiceImpl implements LicenciaService {

    private final LicenciaRepository licenciaRepository;
    private final TramiteRepository tramiteRepository;
    private final TemplateEngine templateEngine;

    public byte[] generarCertificadoPorRuc(String ruc) {
        Licencia licencia = licenciaRepository.findByTramiteRuc(ruc)
                .orElseThrow(() -> new RuntimeException("Licencia o trámite no encontrado para este RUC"));
                
        byte[] pdf = licencia.getPdfArchivo();
        
        if (licencia.getEstado() == com.example.mpct.model.enums.EstadoLicencia.VENCIDA || 
            licencia.getEstado() == com.example.mpct.model.enums.EstadoLicencia.HISTORICA) {
            try {
                com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdf);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                com.lowagie.text.pdf.PdfStamper stamper = new com.lowagie.text.pdf.PdfStamper(reader, out);
                
                int numPages = reader.getNumberOfPages();
                com.lowagie.text.pdf.BaseFont font = com.lowagie.text.pdf.BaseFont.createFont(
                    com.lowagie.text.pdf.BaseFont.HELVETICA_BOLD, com.lowagie.text.pdf.BaseFont.WINANSI, com.lowagie.text.pdf.BaseFont.EMBEDDED);
                
                String watermarkText = licencia.getEstado() == com.example.mpct.model.enums.EstadoLicencia.VENCIDA ? "VENCIDA" : "NO VALIDA";
                
                for (int i = 1; i <= numPages; i++) {
                    com.lowagie.text.pdf.PdfContentByte over = stamper.getOverContent(i);
                    over.saveState();
                    
                    com.lowagie.text.pdf.PdfGState gs = new com.lowagie.text.pdf.PdfGState();
                    gs.setFillOpacity(0.3f);
                    over.setGState(gs);
                    
                    over.beginText();
                    over.setFontAndSize(font, 100);
                    over.setColorFill(java.awt.Color.RED);
                    
                    com.lowagie.text.Rectangle pageSize = reader.getPageSizeWithRotation(i);
                    float x = (pageSize.getLeft() + pageSize.getRight()) / 2;
                    float y = (pageSize.getBottom() + pageSize.getTop()) / 2;
                    
                    over.showTextAligned(com.lowagie.text.Element.ALIGN_CENTER, watermarkText, x, y, 45);
                    over.endText();
                    over.restoreState();
                }
                stamper.close();
                reader.close();
                
                return out.toByteArray();
            } catch (Exception e) {
                System.err.println("No se pudo aplicar la marca de agua: " + e.getMessage());
                // Fallback to original if something fails
            }
        }
        
        return pdf;
    }

    public Licencia generarLicencia(Tramite tramite) {

        String numLicencia = "LIC-" + System.currentTimeMillis();
        String qrContent = "Validar en: https://mpct.gob.pe/validar/" + numLicencia;

        try {
            Random random = new Random();
            long catRandom = (long) (Math.random() * 10000000000000L);
            String codigoCatastral = String.format("%013d", catRandom);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] qrImageBytes = pngOutputStream.toByteArray();

            LocalDateTime fechaEmision = LocalDateTime.now();
            LocalDateTime fechaVencimiento = fechaEmision.plusYears(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'del' yyyy");
            String fechaFormat = fechaEmision.format(formatter);

            Context context = new Context();
            context.setVariable("titular", tramite.getRazonSocial());
            context.setVariable("ruc", tramite.getRuc());
            context.setVariable("representante", tramite.getRepresentanteLegal());
            context.setVariable("dni", tramite.getDni());
            context.setVariable("direccion", tramite.getDomicilioFiscal());
            context.setVariable("codigoCatastral", codigoCatastral);
            context.setVariable("giro", tramite.getRubro());
            context.setVariable("area", tramite.getArea().toString());
            context.setVariable("expediente", tramite.getId().toString().substring(0, 8));
            context.setVariable("fechaActual", fechaFormat);
            
            String logoBase64 = "";
            String firmaBase64 = "";
            try {
                ClassPathResource logoRes = new ClassPathResource("static/images/logo.png");
                ClassPathResource firmaRes = new ClassPathResource("static/images/firma.png");
                
                logoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(logoRes.getInputStream().readAllBytes());
                firmaBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(firmaRes.getInputStream().readAllBytes());
            } catch (Exception e) {
                // Ignore si no existen imágenes locales
            }
            
            context.setVariable("logoPath", logoBase64);
            context.setVariable("firmaPath", firmaBase64);

            Licencia licenciaParcial = Licencia.builder()
                    .numeroLicencia(numLicencia)
                    .build();
            context.setVariable("licencia", licenciaParcial);

            String html = templateEngine.process("licencia", context);

            ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(pdfOutputStream);
            byte[] pdfBytes = pdfOutputStream.toByteArray();

            Licencia licencia;
            java.util.Optional<Licencia> licenciaExistenteOpt = licenciaRepository.findByTramiteRuc(tramite.getRuc());

            if (tramite.getTipo() == com.example.mpct.model.enums.TipoTramite.RENOVACION && licenciaExistenteOpt.isPresent()) {
                licencia = licenciaExistenteOpt.get();
                // Update existing license
                fechaVencimiento = licencia.getFechaVencimiento().plusYears(1);
                
                // Re-render PDF with new dates and attach new tramite
                context.setVariable("fechaActual", LocalDateTime.now().format(formatter));
                context.setVariable("licencia", licencia);
                html = templateEngine.process("licencia", context);
                
                ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
                ITextRenderer rendererUpdate = new ITextRenderer();
                rendererUpdate.setDocumentFromString(html);
                rendererUpdate.layout();
                rendererUpdate.createPDF(pdfOut);
                
                licencia.setTramite(tramite);
                licencia.setEstado(com.example.mpct.model.enums.EstadoLicencia.VIGENTE);
                licencia.setFechaVencimiento(fechaVencimiento);
                licencia.setPdfArchivo(pdfOut.toByteArray());
                
            } else {
                licencia = Licencia.builder()
                        .tramite(tramite)
                        .numeroLicencia(numLicencia)
                        .codigoCatastral(codigoCatastral)
                        .qrData(qrContent)
                        .pdfArchivo(pdfBytes)
                        .fechaEmision(fechaEmision)
                        .fechaVencimiento(fechaVencimiento)
                        .build();
            }

            return licenciaRepository.save(licencia);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar Licencia PDF: " + e.getMessage(), e);
        }
    }

    public java.util.Optional<Licencia> obtenerPorNumero(String numeroLicencia) {
        return licenciaRepository.findByNumeroLicencia(numeroLicencia);
    }
}
