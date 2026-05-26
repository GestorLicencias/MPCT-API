package com.example.mpct.service.impl;

import com.example.mpct.service.DniScrapingService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import java.util.Map;

@Service
public class DniScrapingServiceImpl implements DniScrapingService {

    @Override
    public String obtenerNombresPorDni(String dni) {
        if (dni == null || dni.length() != 8) {
            throw new RuntimeException("El DNI debe tener 8 dígitos");
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
            
            String url = "https://api.apis.net.pe/v1/dni?numero=" + dni;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            if (response.getBody() != null && response.getBody().containsKey("nombre")) {
                return response.getBody().get("nombre").toString();
            }
            
            throw new RuntimeException("No se encontraron datos para este DNI en apis.net.pe");

        } catch (Exception ex) {
            System.out.println("Error consultando DNI en apis.net.pe desde el backend: " + ex.getMessage());
            throw new RuntimeException("No se pudo obtener la información del DNI (Error de conexión)");
        }
    }
}
