package com.example.mpct.service.impl;

import com.example.mpct.service.SunatScrapingService;
import com.example.mpct.dto.sunat.SunatRucResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SunatScrapingServiceImpl implements SunatScrapingService {

    public SunatRucResponse validarRuc(String ruc) {
        if (ruc == null || ruc.length() != 11) {
            throw new RuntimeException("El RUC debe tener 11 dígitos");
        }

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://api.apis.net.pe/v1/ruc?numero=" + ruc;
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>("parameters", headers);

            org.springframework.http.ResponseEntity<java.util.Map> responseEntity = 
                restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, java.util.Map.class);
            
            java.util.Map<String, Object> response = responseEntity.getBody();
            
            if (response == null || !response.containsKey("nombre")) {
                throw new RuntimeException("No se encontraron datos para el RUC especificado.");
            }

            String razonSocial = (String) response.get("nombre");
            String estado = (String) response.get("estado");
            String condicion = (String) response.get("condicion");
            String domicilioFiscal = (String) response.get("direccion");
            if (domicilioFiscal == null || domicilioFiscal.isEmpty()) {
                domicilioFiscal = "No registrado";
            }
            
            // FUENTE SECUNDARIA: Obtener el Rubro / Actividad Económica
            // Debido a bloqueos de Cloudflare en páginas públicas sin token de API, 
            // simularemos la respuesta exacta que espera el inspector basándonos en el nombre,
            // o retornaremos una lista de actividades por defecto.
            // FUENTE SECUNDARIA: Obtener el Rubro / Actividad Económica
            // ELIMINADO para evitar bloqueos y demoras de 15 segundos.
            // El rubro será proporcionado manualmente por el usuario en el frontend.
            String rubro = "No especificado";

            return new SunatRucResponse(ruc, razonSocial, estado, condicion, domicilioFiscal, rubro);

        } catch (Exception e) {
            throw new RuntimeException("Error al consultar el RUC en la API pública: " + e.getMessage());
        }
    }
}
