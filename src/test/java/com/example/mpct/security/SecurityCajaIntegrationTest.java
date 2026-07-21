package com.example.mpct.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.example.mpct.service.RucValidationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
    "JSON_PE_API_KEY=dummy",
    "MERCADO_PAGO_ACCESS_TOKEN=dummy"
})
class SecurityCajaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RucValidationService rucValidationService;

    @Test
    @WithMockUser(username = "ciudadano@test.com", roles = {"CIUDADANO"})
    void ciudadanoCanAccessCajaAbrir() throws Exception {
        // Verifica que el endpoint requiere rol CAJERO. Un ciudadano debe recibir 403.
        // Esta prueba valida el fix de P1 (IDOR): el endpoint debe estar protegido.
        mockMvc.perform(post("/api/v1/caja/abrir"))
               .andExpect(result -> {
                   int statusCode = result.getResponse().getStatus();
                   System.out.println("CÓDIGO DE RESPUESTA PARA CIUDADANO EN /api/v1/caja/abrir: " + statusCode);
                   if (statusCode != 403) {
                       throw new AssertionError("El endpoint NO está protegido. Se esperaba 403 Forbidden, se obtuvo: " + statusCode);
                   }
               });
    }
}
