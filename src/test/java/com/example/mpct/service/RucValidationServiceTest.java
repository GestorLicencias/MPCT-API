package com.example.mpct.service;

import com.example.mpct.dto.tramite.ValidacionRucResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RucValidationServiceTest {

    private RucValidationService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        service = new RucValidationService(); // Instancia manual sin Spring
        ReflectionTestUtils.setField(service, "apiKey", "dummy");
        
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    private void mockRucResponse() {
        String rucJson = """
            {
              "success": true,
              "data": {
                "estado": "ACTIVO",
                "condicion": "HABIDO",
                "provincia": "TRUJILLO",
                "distrito": "TRUJILLO",
                "nombre_o_razon_social": "EMPRESA PRUEBA SAC",
                "direccion_completa": "AV. ESPAÑA 123"
              }
            }
            """;
        mockServer.expect(requestTo("https://api.json.pe/api/ruc"))
                  .andExpect(method(HttpMethod.POST))
                  .andRespond(withSuccess(rucJson, MediaType.APPLICATION_JSON));
    }

    @Test
    void a_shouldSelectGerenteGeneralOverOthers() {
        mockRucResponse();
        
        String repJson = """
            {
              "success": true,
              "data": [
                {
                  "cargo": "APODERADO",
                  "fecha_desde": "01/01/2020",
                  "numero_de_documento": "11111111",
                  "nombre": "JUAN APODERADO"
                },
                {
                  "cargo": "GERENTE GENERAL",
                  "fecha_desde": "01/01/2023",
                  "numero_de_documento": "99999999",
                  "nombre": "MARIA GERENTE"
                }
              ]
            }
            """;
            
        mockServer.expect(requestTo("https://api.json.pe/api/ruc/representantes"))
                  .andRespond(withSuccess(repJson, MediaType.APPLICATION_JSON));

        ValidacionRucResponse res = service.validarRuc("20123456789", "127.0.0.1");
        
        assertTrue(res.isValido());
        assertEquals("99999999", res.getDniGerente(), "Debería elegir al Gerente General (mayor prioridad)");
        assertEquals("MARIA GERENTE", res.getNombreGerente());
        mockServer.verify();
    }

    @Test
    void b_shouldResolveTieWithOldestFechaDesde() {
        mockRucResponse();
        
        // Usamos formato dd/MM/yyyy como en la API real
        String repJson = """
            {
              "success": true,
              "data": [
                {
                  "cargo": "APODERADO",
                  "fecha_desde": "01/01/2023",
                  "numero_de_documento": "22222222",
                  "nombre": "PEDRO RECIENTE"
                },
                {
                  "cargo": "APODERADO COMERCIAL",
                  "fecha_desde": "14/03/2013",
                  "numero_de_documento": "11111111",
                  "nombre": "JUAN ANTIGUO"
                }
              ]
            }
            """;
            
        mockServer.expect(requestTo("https://api.json.pe/api/ruc/representantes"))
                  .andRespond(withSuccess(repJson, MediaType.APPLICATION_JSON));

        ValidacionRucResponse res = service.validarRuc("20123456781", "127.0.0.2");
        
        assertTrue(res.isValido());
        assertEquals("11111111", res.getDniGerente(), "Debería elegir al Apoderado con fecha más antigua");
        assertEquals("JUAN ANTIGUO", res.getNombreGerente());
        mockServer.verify();
    }

    @Test
    void c_shouldLeaveEmptyIfNoValidRole() {
        mockRucResponse();
        
        String repJson = """
            {
              "success": true,
              "data": [
                {
                  "cargo": "CONTADOR PUBLICO",
                  "fecha_desde": "01/01/2010",
                  "numero_de_documento": "33333333",
                  "nombre": "ALEX CONTADOR"
                },
                {
                  "cargo": "SECRETARIA",
                  "fecha_desde": "01/01/2015",
                  "numero_de_documento": "44444444",
                  "nombre": "ANA SECRETARIA"
                }
              ]
            }
            """;
            
        mockServer.expect(requestTo("https://api.json.pe/api/ruc/representantes"))
                  .andRespond(withSuccess(repJson, MediaType.APPLICATION_JSON));

        ValidacionRucResponse res = service.validarRuc("20123456782", "127.0.0.3");
        
        assertTrue(res.isValido());
        assertNull(res.getDniGerente(), "Debería dejar DNI en nulo");
        assertNull(res.getNombreGerente(), "Debería dejar nombre en nulo");
        assertTrue(res.getMensaje().contains("Inserte manualmente"));
        mockServer.verify();
    }
}
