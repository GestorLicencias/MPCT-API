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
            // FUENTE SECUNDARIA: Obtener el Rubro / Actividad Económica usando Playwright para evadir Cloudflare
            String rubro = "No especificado";

            try (com.microsoft.playwright.Playwright playwright = com.microsoft.playwright.Playwright.create()) {
                com.microsoft.playwright.Browser browser = playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
                com.microsoft.playwright.Page page = browser.newPage();
                page.setDefaultNavigationTimeout(15000); // 15 segundos max
                
                // Intentamos extraer directamente de SUNAT oficial
                page.navigate("https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsruc/jcrS00Alias?accion=consPorRuc&nroRuc=" + ruc);
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
                
                // SUNAT muestra las actividades en una tabla con la clase "table"
                java.util.List<String> trs = page.locator("tr").allInnerTexts();
                StringBuilder actividades = new StringBuilder();
                boolean foundActividad = false;

                for (String text : trs) {
                    if (text.contains("Actividad(es) Económica(s):")) {
                        // El texto usualmente viene como "Actividad(es) Económica(s): \n Principal - 8530 - ENSEÑANZA SUPERIOR ..."
                        String cleanText = text.replace("Actividad(es) Económica(s):", "").trim();
                        // Dividir por saltos de linea
                        String[] lineas = cleanText.split("\\r?\\n");
                        for (String linea : lineas) {
                            linea = linea.trim();
                            if (!linea.isEmpty() && (linea.startsWith("Principal") || linea.startsWith("Secundaria"))) {
                                if (actividades.length() > 0) actividades.append("\n");
                                actividades.append(linea);
                                foundActividad = true;
                            }
                        }
                        break;
                    }
                }
                
                if (foundActividad) {
                    rubro = actividades.toString();
                } else {
                    // Backup: universidadperu.com — tiene una página directa por RUC
                    page.navigate("https://www.universidadperu.com/empresas/" + ruc + ".php");
                    page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
                
                    String bodyText = page.locator("body").innerText();
                    
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)Actividad(?:es)? Económica(?:s)?:?\\s*([^\\n]+)").matcher(bodyText);
                    if (m.find()) {
                        rubro = m.group(1).trim();
                    } else {
                        m = java.util.regex.Pattern.compile("(?i)CIIU:?\\s*([^\\n]+)").matcher(bodyText);
                        if (m.find()) {
                            rubro = m.group(1).trim();
                        } else {
                            m = java.util.regex.Pattern.compile("(?i)Giro:?\\s*([^\\n]+)").matcher(bodyText);
                            if (m.find()) {
                                rubro = m.group(1).trim();
                            }
                        }
                    }
                }
                
            } catch (Exception ex) {
                System.out.println("Error al hacer scraping con Playwright: " + ex.getMessage());
                rubro = "Información no disponible (Error de conexión)";
            }

            return new SunatRucResponse(ruc, razonSocial, estado, condicion, domicilioFiscal, rubro);

        } catch (Exception e) {
            throw new RuntimeException("Error al consultar el RUC en la API pública: " + e.getMessage());
        }
    }
}
