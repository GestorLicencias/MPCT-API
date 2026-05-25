package com.example.mpct.service.impl;

import com.example.mpct.service.LicenciaService;
import com.example.mpct.model.entity.Licencia;
import com.example.mpct.model.entity.Tramite;
import com.example.mpct.model.entity.UserProfile;
import com.example.mpct.repository.LicenciaRepository;
import com.example.mpct.repository.UserProfileRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
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
    private final UserProfileRepository userProfileRepository;
    private final TemplateEngine templateEngine;

    @Value("${app.storage.path:./uploads}")
    private String storagePath;

    public Licencia generarLicencia(Tramite tramite) {
        UserProfile profile = userProfileRepository.findByUserId(tramite.getSolicitante().getId())
                .orElseThrow(() -> new RuntimeException("Perfil no encontrado"));

        String numLicencia = "LIC-" + System.currentTimeMillis();
        String qrContent = "Validar en: https://mpct.gob.pe/validar/" + numLicencia;

        try {
            // Generar código catastral random de 13 dígitos
            Random random = new Random();
            long catRandom = (long) (Math.random() * 10000000000000L);
            String codigoCatastral = String.format("%013d", catRandom);

            // Generar QR
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] qrImageBytes = pngOutputStream.toByteArray();
            String qrBase64 = Base64.getEncoder().encodeToString(qrImageBytes);

            LocalDateTime fechaEmision = LocalDateTime.now();
            LocalDateTime fechaVencimiento = fechaEmision.plusYears(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'del' yyyy");
            String fechaFormat = fechaEmision.format(formatter);

            // Configurar contexto de Thymeleaf
            Context context = new Context();
            context.setVariable("titular", profile.getRazonSocial());
            context.setVariable("ruc", profile.getRuc());
            context.setVariable("representante", profile.getRepresentanteLegal());
            context.setVariable("dni", "No Registrado");
            context.setVariable("direccion", profile.getDomicilioFiscal());
            context.setVariable("codigoCatastral", codigoCatastral);
            context.setVariable("giro", tramite.getTipo().name());
            context.setVariable("area", tramite.getArea() != null ? tramite.getArea().toString() : "0");
            context.setVariable("expediente", tramite.getId().toString().substring(0, 8));
            context.setVariable("fechaActual", fechaFormat);
            
            // Imagenes (asegúrate de que estén accesibles para ITextRenderer, mejor en base64 para evitar rutas absolutas)
            // Leer imagenes a base64
            Path logoPathObj = Paths.get("src/main/resources/static/images/logo.png").toAbsolutePath();
            Path firmaPathObj = Paths.get("src/main/resources/static/images/firma.png").toAbsolutePath();
            
            String logoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(logoPathObj));
            String firmaBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(firmaPathObj));
            
            context.setVariable("logoPath", logoBase64);
            context.setVariable("firmaPath", firmaBase64);

            Licencia licenciaParcial = Licencia.builder()
                    .numeroLicencia(numLicencia)
                    .build();
            context.setVariable("licencia", licenciaParcial);

            // Procesar plantilla
            String html = templateEngine.process("licencia", context);

            // Generar PDF con Flying Saucer en memoria
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
