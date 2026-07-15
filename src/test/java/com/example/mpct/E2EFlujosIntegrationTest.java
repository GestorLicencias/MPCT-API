package com.example.mpct;

import com.example.mpct.model.enums.*;
import com.example.mpct.repository.TramiteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mpct.service.SunatScrapingService;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
public class E2EFlujosIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TramiteRepository tramiteRepository;

    @MockBean
    private SunatScrapingService sunatScrapingService;

    @Autowired
    private com.example.mpct.repository.PagoRepository pagoRepository;

    private static final String RUC_TEST = "20123456789";

    @AfterEach
    void cleanup() {
        tramiteRepository.findTopByRucOrderByCreatedAtDesc(RUC_TEST).ifPresent(t -> {
            pagoRepository.findByTramiteId(t.getId()).ifPresent(p -> pagoRepository.delete(p));
            tramiteRepository.delete(t);
        });
    }

    @Test
    void testFlujoCompleto_CreacionYActualizacionTramite_ConDTO() throws Exception {
        // Mocking SunatScrapingService
        when(sunatScrapingService.validarRuc(any())).thenReturn(
                new com.example.mpct.dto.sunat.SunatRucResponse(
                        "20123456789", "Empresa Test E2E", "ACTIVO", "HABIDO", "Av. Test 123", "Ventas"
                )
        );

        // 1. Simular la subida de archivos (crear trámite) a través del controlador
        MockMultipartFile plano = new MockMultipartFile("plano", "plano.pdf", "application/pdf", "plano_content".getBytes());
        MockMultipartFile foto = new MockMultipartFile("fotos", "foto1.jpg", "image/jpeg", "foto_content".getBytes());

        mockMvc.perform(multipart("/api/v1/tramites")
                .file(plano)
                .file(foto)
                .param("ruc", RUC_TEST)
                .param("representanteLegal", "Juan Perez")
                .param("rubro", "Ventas")
                .param("dni", "12345678")
                .param("area", "150.00")
                .param("tipo", "NUEVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"));
                
        // 2. Simular pago presencial, lo que avanza el trámite
        MockMultipartFile voucher = new MockMultipartFile("voucher", "voucher.jpg", "image/jpeg", "voucher".getBytes());
        
        mockMvc.perform(multipart("/api/v1/tramites/" + RUC_TEST + "/pagar")
                .file(voucher)
                .param("metodoPago", "BANCO_NACION")
                .param("numeroComprobante", "12345")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDANDO_PAGO"));
                
        // Nota: PENDIENTE_REVISION si requiere inspeccion previa (TIPO: NUEVO <= 100m2 es PROGRAMADO)
        // Como le puse 150.00m2, si es mayor a 100m2, la inspeccion es posterior y se APRUEBA de inmediato 
        // y se pasa a PROGRAMADO la inspeccion post, o tal vez PENDIENTE_REVISION.
        // Veremos qué sale al correrlo.
    }
}
