package com.example.mpct.service.impl;

import com.example.mpct.service.*;

import com.example.mpct.dto.sunat.SunatRucResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SunatScrapingServiceImpl implements SunatScrapingService {

    private static final String SUNAT_URL = "https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/jcrS00Alias";

    public SunatRucResponse validarRuc(String ruc) {
        if (ruc == null || ruc.length() != 11) {
            throw new RuntimeException("El RUC debe tener 11 dígitos");
        }

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://api.apis.net.pe/v1/ruc?numero=" + ruc;
            
            var response = restTemplate.getForObject(url, java.util.Map.class);
            
            if (response == null) {
                throw new RuntimeException("RUC no encontrado o servicio inactivo.");
            }
            
            String razonSocial = (String) response.get("nombre");
            String estado = (String) response.get("estado");
            String condicion = (String) response.get("condicion");
            String direccion = (String) response.get("direccion");

            if (!"ACTIVO".equalsIgnoreCase(estado) || !"HABIDO".equalsIgnoreCase(condicion)) {
                throw new RuntimeException("El contribuyente debe estar ACTIVO y HABIDO.");
            }

            return new SunatRucResponse(ruc, razonSocial, estado, condicion, direccion);
            
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            throw new RuntimeException("RUC no encontrado en la base de datos de SUNAT.");
        } catch (Exception e) {
            throw new RuntimeException("Error al validar el RUC en línea: " + e.getMessage());
        }
    }
}
