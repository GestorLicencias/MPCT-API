package com.example.mpct.service.impl;

import com.example.mpct.service.SunatScrapingService;
import com.example.mpct.dto.sunat.SunatRucResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SunatScrapingServiceImpl implements SunatScrapingService {

    private final org.springframework.web.client.RestTemplate restTemplate;

    public SunatScrapingServiceImpl(org.springframework.web.client.RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SunatRucResponse validarRuc(String ruc) {
        if (ruc == null || ruc.length() != 11) {
            throw new RuntimeException("El RUC debe tener 11 dígitos");
        }
        
        if (!ruc.startsWith("20")) {
            throw new RuntimeException("Este trámite solo acepta RUC de persona jurídica");
        }

        try {
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

            String estado = (String) response.get("estado");
            if (estado != null && !estado.trim().equalsIgnoreCase("ACTIVO")) {
                throw new RuntimeException("El RUC no se encuentra ACTIVO (Estado actual: " + estado + ").");
            }

            String provincia = (String) response.get("provincia");
            String departamento = (String) response.get("departamento");
            String domicilioFiscal = (String) response.get("direccion");
            
            boolean esTrujillo = false;
            if (provincia != null && provincia.toUpperCase().contains("TRUJILLO")) esTrujillo = true;
            if (departamento != null && departamento.toUpperCase().contains("TRUJILLO")) esTrujillo = true;
            if (domicilioFiscal != null && domicilioFiscal.toUpperCase().contains("TRUJILLO")) esTrujillo = true;

            if (!esTrujillo) {
                throw new RuntimeException("El trámite no procede: El domicilio fiscal no corresponde a la jurisdicción de Trujillo.");
            }

            String razonSocial = (String) response.get("nombre");
            String condicion = (String) response.get("condicion");
            
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
