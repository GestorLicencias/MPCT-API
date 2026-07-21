package com.example.mpct;

import com.example.mpct.model.entity.Comprobante;
import com.example.mpct.model.enums.TipoComprobante;
import com.example.mpct.service.ComprobanteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("local")
public class ComprobantePdfTest {

    @Autowired
    private ComprobanteService comprobanteService;

    @Test
    public void testGenerarPdf() throws Exception {
        Comprobante comp = new Comprobante();
        comp.setTipo(TipoComprobante.FACTURA_INTERNA);
        comp.setSerie("F001");
        comp.setCorrelativo(158L);
        comp.setMontoTotal(new BigDecimal("250.75"));
        comp.setFechaEmision(LocalDateTime.now());
        comp.setRazonSocialSnapshot("Empresa de Prueba S.A.C.");
        comp.setDireccionSnapshot("Av. Los Pinos 123");
        comp.setDocumentoClienteSnapshot("20123456789");

        byte[] pdfBytes = comprobanteService.generarPdf(comp);
        
        try (FileOutputStream fos = new FileOutputStream("test-comprobante.pdf")) {
            fos.write(pdfBytes);
        }
        System.out.println("TEST_PDF_SUCCESSFULLY_GENERATED: test-comprobante.pdf (" + pdfBytes.length + " bytes)");
    }
}
