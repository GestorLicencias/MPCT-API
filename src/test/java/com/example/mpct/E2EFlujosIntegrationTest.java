package com.example.mpct;

import com.example.mpct.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.mpct.service.RucValidationService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class E2EFlujosIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TramiteRepository tramiteRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private PagoDetalleRepository pagoDetalleRepository;
    @Autowired private ComprobanteRepository comprobanteRepository;
    @Autowired private LicenciaRepository licenciaRepository;
    @Autowired private InspeccionRepository inspeccionRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockBean
    private RucValidationService rucValidationService;

    // RUC fijo — la limpieza es responsabilidad del test, no del estado externo de la BD.
    private static final String RUC_TEST = "20111111111";

    /**
     * Limpieza robusta respetando el orden de claves foráneas del esquema:
     *   comprobantes → pago_detalles → pagos → licencias → inspecciones → tramites
     *
     * Se ejecuta en @BeforeEach Y @AfterEach para que el test sea idempotente:
     * pasa la primera vez, pasa la décima vez, sin importar el estado previo de la BD.
     */
    private void limpiarPorRuc(String ruc) {
        transactionTemplate.execute(tx -> {
            tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc).ifPresent(tramite -> {
                var tramiteId = tramite.getId();

                // 1. Comprobantes — referencian pagos (FK: comprobantes.pago_id → pagos.id)
                pagoRepository.findByTramiteId(tramiteId).ifPresent(pago -> {
                    comprobanteRepository.findAll().stream()
                        .filter(c -> pago.getId().equals(c.getPago() != null ? c.getPago().getId() : null))
                        .forEach(comprobanteRepository::delete);
                });

                // 2. Pago detalles — referencian pagos
                pagoRepository.findByTramiteId(tramiteId).ifPresent(pago -> {
                    pagoDetalleRepository.findAll().stream()
                        .filter(d -> pago.getId().equals(d.getPago() != null ? d.getPago().getId() : null))
                        .forEach(pagoDetalleRepository::delete);
                    // 3. Pago
                    pagoRepository.delete(pago);
                });

                // 4. Licencias — referencian tramites
                licenciaRepository.findByTramiteId(tramiteId).ifPresent(licenciaRepository::delete);

                // 5. Inspecciones — referencian tramites
                inspeccionRepository.findByTramiteId(tramiteId).forEach(inspeccionRepository::delete);

                // 6. Trámite
                tramiteRepository.delete(tramite);
            });
            return null;
        });
    }

    @BeforeEach
    void setup() {
        // Garantiza estado limpio antes de cada test — sin dependencia de BD externa
        limpiarPorRuc(RUC_TEST);
    }

    @AfterEach
    void cleanup() {
        // Limpia después para no contaminar la próxima corrida de la suite
        limpiarPorRuc(RUC_TEST);
    }

    @Test
    void testFlujoCompleto_CreacionYActualizacionTramite_ConDTO() throws Exception {
        when(rucValidationService.validarRuc(any(), any())).thenReturn(
                com.example.mpct.dto.tramite.ValidacionRucResponse.builder()
                        .valido(true)
                        .ruc(RUC_TEST)
                        .razonSocial("Empresa Test E2E")
                        .domicilioFiscal("Av. Test 123")
                        .build()
        );

        // 1. Crear trámite
        MockMultipartFile plano = new MockMultipartFile(
                "plano", "plano.pdf", "application/pdf", "plano_content".getBytes());

        mockMvc.perform(multipart("/api/v1/tramites")
                .file(plano)
                .param("ruc", RUC_TEST)
                .param("representanteLegal", "Juan Perez")
                .param("rubro", "Ventas")
                .param("dni", "12345678")
                .param("email", "test@example.com")
                .param("area", "150.00")
                .param("tipo", "NUEVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"));

        // 2. Pagar por Banco de la Nación
        MockMultipartFile voucher = new MockMultipartFile(
                "voucher", "voucher.jpg", "image/jpeg", "voucher".getBytes());

        mockMvc.perform(multipart("/api/v1/tramites/" + RUC_TEST + "/pagar")
                .file(voucher)
                .param("metodoPago", "BANCO_NACION")
                .param("numeroComprobante", "12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDANDO_PAGO"));
    }
}
