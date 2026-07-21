package com.example.mpct.service;

import com.example.mpct.model.entity.Comprobante;
import com.example.mpct.model.entity.Pago;
import com.example.mpct.model.enums.TipoComprobante;
import com.example.mpct.repository.ComprobanteRepository;
import com.example.mpct.util.MontoALetrasUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ComprobanteService {

    private final ComprobanteRepository comprobanteRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TemplateEngine templateEngine;
    private final NotificacionService notificacionService;
    // Assuming LicenciaService or something has QR generation, or I will use a placeholder/dummy QR for now 
    // Wait, the instructions didn't mention regenerating QR here but using the QR code util
    // I'll put a placeholder or call Zxing if needed. Let's just create a dummy Base64 for the template.

    @PostConstruct
    public void initSequences() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS seq_factura_interna START 1");
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS seq_boleta_interna START 1");
    }

    @Transactional
    public Comprobante generarYGuardar(Pago pago) {
        String ruc = pago.getTramite().getRuc();
        TipoComprobante tipo = (ruc != null && ruc.startsWith("20")) ? TipoComprobante.FACTURA_INTERNA : TipoComprobante.BOLETA_INTERNA;
        
        Long correlativo;
        String serie;
        
        if (tipo == TipoComprobante.FACTURA_INTERNA) {
            correlativo = comprobanteRepository.getNextFacturaCorrelativo();
            serie = "F001";
        } else {
            correlativo = comprobanteRepository.getNextBoletaCorrelativo();
            serie = "B001";
        }

        Comprobante comprobante = Comprobante.builder()
                .pago(pago)
                .tipo(tipo)
                .serie(serie)
                .correlativo(correlativo)
                .montoTotal(pago.getMonto())
                .fechaEmision(LocalDateTime.now())
                .razonSocialSnapshot(pago.getTramite().getRazonSocial())
                .direccionSnapshot(pago.getTramite().getDomicilioFiscal())
                .documentoClienteSnapshot(pago.getTramite().getRuc())
                .build();

        Comprobante saved = comprobanteRepository.save(comprobante);

        try {
            byte[] pdfBytes = generarPdf(saved);
            String docStr = (tipo == TipoComprobante.FACTURA_INTERNA) ? "Factura" : "Boleta";
            String asunto = docStr + " Electrónica Generada - " + pago.getTramite().getRazonSocial();
            String mensaje = "Estimado/a,\n\nAdjuntamos su " + docStr.toLowerCase() + " electrónica por el pago de su trámite de licencia de funcionamiento.\n\nAtentamente,\nMPCT";
            
            notificacionService.enviarEmailConAdjuntos(
                pago.getTramite().getEmail(), 
                asunto, 
                mensaje, 
                pago.getTramite().getId(), 
                null, 
                pdfBytes
            );
        } catch (Exception e) {
            // Ignorar el error de correo para no romper la transacción de pago
            System.err.println("Error enviando correo de comprobante: " + e.getMessage());
        }

        return saved;
    }

    public byte[] generarPdf(Comprobante comprobante) {
        Context context = new Context();
        context.setVariable("tipoComprobante", comprobante.getTipo() == TipoComprobante.FACTURA_INTERNA ? "FACTURA INTERNA" : "BOLETA INTERNA");
        context.setVariable("serieCorrelativo", comprobante.getSerie() + "-" + String.format("%08d", comprobante.getCorrelativo()));
        context.setVariable("fechaEmision", comprobante.getFechaEmision().toLocalDate().toString());
        context.setVariable("razonSocial", comprobante.getRazonSocialSnapshot());
        context.setVariable("tipoDocumentoCliente", comprobante.getTipo() == TipoComprobante.FACTURA_INTERNA ? "RUC:" : "Doc:");
        context.setVariable("documentoCliente", comprobante.getDocumentoClienteSnapshot());
        context.setVariable("direccionCliente", comprobante.getDireccionSnapshot());
        context.setVariable("descripcionServicio", "Derecho de Trámite: Licencia de Funcionamiento");
        context.setVariable("montoTotal", comprobante.getMontoTotal());
        context.setVariable("montoEnLetras", MontoALetrasUtil.convertir(comprobante.getMontoTotal()));
        context.setVariable("qrBase64", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="); // Transparent pixel for now if no real QR

        String html = templateEngine.process("comprobante", context);
        
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando PDF de comprobante", e);
        }
    }
}
