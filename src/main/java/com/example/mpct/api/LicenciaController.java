package com.example.mpct.api;

import com.example.mpct.model.entity.Licencia;
import com.example.mpct.repository.LicenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1/licencias")
@RequiredArgsConstructor
public class LicenciaController {

    private final LicenciaRepository licenciaRepository;
    
    @Value("${app.storage.path:./uploads}")
    private String storagePath;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'INSPECTOR', 'ADMIN')")
    public ResponseEntity<Licencia> getLicencia(@PathVariable UUID id) {
        return ResponseEntity.ok(licenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Licencia no encontrada")));
    }

    @GetMapping("/{id}/descargar")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'INSPECTOR', 'ADMIN')")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id) {
        Licencia licencia = licenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Licencia no encontrada"));

        byte[] pdfBytes = licencia.getPdfArchivo();
        if (pdfBytes == null) {
            throw new RuntimeException("El archivo de la licencia no está disponible.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + licencia.getNumeroLicencia() + ".pdf\"")
                .body(pdfBytes);
    }
}
