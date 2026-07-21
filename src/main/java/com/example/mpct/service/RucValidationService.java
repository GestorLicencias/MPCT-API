package com.example.mpct.service;

import com.example.mpct.dto.tramite.ValidacionRucResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RucValidationService {

    @Value("${json.pe.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private static final Set<String> DISTRITOS_PERMITIDOS = Set.of(
            "TRUJILLO", "VICTOR LARCO HERRERA", "HUANCHACO", "LA ESPERANZA", 
            "FLORENCIA DE MORA", "EL PORVENIR", "MOCHE", "SALAVERRY", 
            "LAREDO", "POROTO", "SIMBAL", "ALTO CHICAMA"
    );

    public Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofHours(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public ValidacionRucResponse validarRuc(String ruc, String ip) {
        Bucket bucket = resolveBucket(ip);
        if (!bucket.tryConsume(1)) {
            return ValidacionRucResponse.builder().valido(false).mensaje("Demasiados intentos. Por favor intente más tarde.").build();
        }

        // 1. Validar que empiece con 20
        if (ruc == null || !ruc.startsWith("20") || ruc.length() != 11) {
            return ValidacionRucResponse.builder().valido(false).mensaje("Solo se aceptan RUC de empresas (RUC 20)").build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("ruc", ruc), headers);

        try {
            // 2. Consultar Endpoint Principal
            Map<String, Object> response1 = restTemplate.postForObject("https://api.json.pe/api/ruc", request, Map.class);
            if (response1 == null || !(Boolean) response1.get("success")) {
                return ValidacionRucResponse.builder().valido(false).mensaje("Error al consultar el RUC en SUNAT.").build();
            }

            Map<String, Object> dataRuc = (Map<String, Object>) response1.get("data");
            
            // Validar Estado
            String estado = (String) dataRuc.get("estado");
            if (!"ACTIVO".equalsIgnoreCase(estado)) {
                return ValidacionRucResponse.builder().valido(false).mensaje("El RUC no está ACTIVO. Estado actual: " + estado).build();
            }

            // Validar Condición
            String condicion = (String) dataRuc.get("condicion");
            if (!"HABIDO".equalsIgnoreCase(condicion)) {
                return ValidacionRucResponse.builder().valido(false).mensaje("El RUC no está HABIDO. Condición actual: " + condicion).build();
            }

            // Validar Ubicación
            String provincia = (String) dataRuc.get("provincia");
            String distrito = (String) dataRuc.get("distrito");
            if (!"TRUJILLO".equalsIgnoreCase(provincia) || (distrito != null && !DISTRITOS_PERMITIDOS.contains(distrito.toUpperCase()))) {
                return ValidacionRucResponse.builder().valido(false).mensaje("El RUC no pertenece a la provincia de Trujillo.").build();
            }

            // 3. Consultar Representantes
            Map<String, Object> response2 = restTemplate.postForObject("https://api.json.pe/api/ruc/representantes", request, Map.class);
            if (response2 == null || !(Boolean) response2.get("success")) {
                return ValidacionRucResponse.builder().valido(false).mensaje("Error al obtener representantes.").build();
            }

            List<Map<String, String>> representantes = (List<Map<String, String>>) response2.get("data");
            Map<String, String> gerente = representantes.stream()
                    .filter(r -> r.get("cargo") != null && r.get("cargo").toUpperCase().contains("GERENTE GENERAL"))
                    .findFirst()
                    .orElse(null);

            if (gerente == null) {
                return ValidacionRucResponse.builder().valido(false).mensaje("No se encontró Gerente General registrado.").build();
            }

            // Todo OK. Retornar éxito
            return ValidacionRucResponse.builder()
                    .valido(true)
                    .mensaje("RUC Válido")
                    .ruc(ruc)
                    .razonSocial((String) dataRuc.get("nombre_o_razon_social"))
                    .dniGerente(gerente.get("numero_de_documento"))
                    .nombreGerente(gerente.get("nombre"))
                    .domicilioFiscal((String) dataRuc.get("direccion_completa"))
                    .build();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                return ValidacionRucResponse.builder().valido(false).mensaje("No se encontró el RUC ingresado, verifica el número.").build();
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                System.err.println("CRITICAL ERROR: Token de JSON.pe inválido o vencido.");
                return ValidacionRucResponse.builder().valido(false).mensaje("Servicio de validación no disponible, intenta más tarde.").build();
            }
            return ValidacionRucResponse.builder().valido(false).mensaje("No se pudo validar el RUC en este momento, intenta nuevamente.").build();
        } catch (Exception e) {
            return ValidacionRucResponse.builder().valido(false).mensaje("No se pudo validar el RUC en este momento, intenta nuevamente.").build();
        }
    }
}
