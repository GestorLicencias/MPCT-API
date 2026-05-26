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

@Service
@RequiredArgsConstructor
public class LicenciaServiceImpl implements LicenciaService {

    private final LicenciaRepository licenciaRepository;
    private final TramiteRepository tramiteRepository;
    private final TemplateEngine templateEngine;

    public byte[] generarCertificadoPorRuc(String ruc) {
        Tramite tramite = tramiteRepository.findByRuc(ruc)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));
        Licencia licencia = licenciaRepository.findByTramiteId(tramite.getId())
                .orElseThrow(() -> new RuntimeException("Licencia no generada para este trámite aún"));
        return licencia.getPdfArchivo();
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
            
            Path logoPathObj = Paths.get("src/main/resources/static/images/logo.png").toAbsolutePath();
            Path firmaPathObj = Paths.get("src/main/resources/static/images/firma.png").toAbsolutePath();
            
            String logoBase64 = "";
            String firmaBase64 = "";
            try {
                logoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(logoPathObj));
                firmaBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(firmaPathObj));
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

            Licencia licencia = Licencia.builder()
                    .tramite(tramite)
                    .numeroLicencia(numLicencia)
                    .codigoCatastral(codigoCatastral)
                    .qrData(qrContent)
                    .pdfArchivo(pdfBytes)
                    .fechaEmision(fechaEmision)
                    .fechaVencimiento(fechaVencimiento)
                    .build();

            return licenciaRepository.save(licencia);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar Licencia PDF: " + e.getMessage(), e);
        }
    }
}
