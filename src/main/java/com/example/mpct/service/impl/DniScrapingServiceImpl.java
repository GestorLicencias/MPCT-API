package com.example.mpct.service.impl;

import com.example.mpct.service.DniScrapingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DniScrapingServiceImpl implements DniScrapingService {

    @Override
    public String obtenerNombresPorDni(String dni) {
        if (dni == null || dni.length() != 8) {
            throw new RuntimeException("El DNI debe tener 8 dígitos");
        }

        try (com.microsoft.playwright.Playwright playwright = com.microsoft.playwright.Playwright.create()) {
            com.microsoft.playwright.Browser browser = playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
            com.microsoft.playwright.Page page = browser.newPage();
            page.setDefaultNavigationTimeout(20000); // 20 segundos máximo

            // Navegar a la página principal
            page.navigate("https://eldni.com/pe/buscar-datos-por-dni");
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);

            // Llenar el DNI y presionar Enter para enviar el formulario directamente
            page.fill("#dni", dni);
            page.locator("#dni").press("Enter");
            
            // Esperar explícitamente a que aparezca la celda con el DNI en lugar de NETWORKIDLE (que falla por anuncios)
            page.locator("td:has-text('" + dni + "')").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(10000));

            // Extraer resultados (eldni.com muestra una tabla horizontal con DNI, Nombres, Paterno, Materno)
            List<String> tds = page.locator("td").allInnerTexts();
            String nombreCompleto = "";
            
            for (int i = 0; i < tds.size(); i++) {
                if (tds.get(i).trim().equals(dni)) {
                    if (i + 3 < tds.size()) {
                        String nombres = tds.get(i + 1).trim();
                        String paterno = tds.get(i + 2).trim();
                        String materno = tds.get(i + 3).trim();
                        nombreCompleto = (nombres + " " + paterno + " " + materno).trim();
                    }
                    break;
                }
            }

            if (nombreCompleto.isEmpty()) {
                page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setPath(java.nio.file.Paths.get("eldni_debug.png")));
                throw new RuntimeException("No se encontraron datos para el DNI especificado en eldni.com");
            }

            return nombreCompleto;

        } catch (Exception ex) {
            System.out.println("Error al hacer scraping en eldni.com con Playwright: " + ex.getMessage());
            throw new RuntimeException("No se pudo obtener la información de eldni.com (Error de conexión o bloqueo)");
        }
    }
}
