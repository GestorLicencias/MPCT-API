package com.example.mpct.service;

import com.example.mpct.service.impl.SunatScrapingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class SunatScrapingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SunatScrapingServiceImpl sunatScrapingService;

    @BeforeEach
    void setUp() {
        sunatScrapingService = new SunatScrapingServiceImpl(restTemplate);
    }

    @Test
    void testRucNoEmpiezaCon20() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            sunatScrapingService.validarRuc("10123456789");
        });
        assertTrue(exception.getMessage().contains("solo acepta RUC de persona jurídica"));
    }

    @Test
    void testRucFueraDeTrujillo() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("nombre", "Empresa Lima");
        mockResponse.put("estado", "ACTIVO");
        mockResponse.put("provincia", "LIMA");
        mockResponse.put("departamento", "LIMA");
        mockResponse.put("direccion", "AV. LIMA 123");

        Mockito.when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(mockResponse));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            sunatScrapingService.validarRuc("20123456789");
        });
        assertTrue(exception.getMessage().contains("no corresponde a la jurisdicción de Trujillo"));
    }

    @Test
    void testRucInactivo() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("nombre", "Empresa Inactiva");
        mockResponse.put("estado", "BAJA DE OFICIO");

        Mockito.when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(mockResponse));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            sunatScrapingService.validarRuc("20123456789");
        });
        assertTrue(exception.getMessage().contains("El RUC no se encuentra ACTIVO"));
    }

    @Test
    void testRucValidoTrujillo() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("nombre", "Empresa Trujillo S.A.C.");
        mockResponse.put("estado", "ACTIVO");
        mockResponse.put("provincia", "TRUJILLO");
        mockResponse.put("departamento", "LA LIBERTAD");
        mockResponse.put("direccion", "AV. ESPAÑA 123");

        Mockito.when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(mockResponse));

        var result = sunatScrapingService.validarRuc("20123456789");
        assertTrue(result.razonSocial().equals("Empresa Trujillo S.A.C."));
    }
}
