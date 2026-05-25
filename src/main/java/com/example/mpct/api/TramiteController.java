package com.example.mpct.api;

import com.example.mpct.dto.tramite.TramiteResponse;
import com.example.mpct.model.enums.TipoTramite;
import com.example.mpct.model.entity.User;
import com.example.mpct.repository.UserRepository;
import com.example.mpct.service.TramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tramites")
@RequiredArgsConstructor
public class TramiteController {

    private final TramiteService tramiteService;
    private final UserRepository userRepository;
    private final com.example.mpct.service.MercadoPagoService mercadoPagoService;
    private final com.example.mpct.repository.TramiteRepository tramiteRepository;
    private final com.example.mpct.repository.LicenciaRepository licenciaRepository;

    @org.springframework.beans.factory.annotation.Value("${app.storage.path:./uploads}")
    private String storagePath;

    @GetMapping("/{id}/licencia/pdf")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'INSPECTOR', 'ADMIN')")
    public ResponseEntity<byte[]> descargarLicenciaPorTramite(@PathVariable UUID id) {
        User user = getCurrentUser();
        var tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (!tramite.getSolicitante().getId().equals(user.getId()) && 
            !user.getRole().name().equals("ADMIN") && 
            !user.getRole().name().equals("INSPECTOR")) {
            throw new RuntimeException("No tiene permisos para ver esta licencia");
        }

        var licencia = licenciaRepository.findByTramiteId(id)
                .orElseThrow(() -> new RuntimeException("Licencia no generada para este trámite aún"));

        byte[] pdfBytes = licencia.getPdfArchivo();
        if (pdfBytes == null) {
            throw new RuntimeException("El archivo de la licencia no está disponible.");
        }

        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + licencia.getNumeroLicencia() + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/{id}/archivos/plano")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'INSPECTOR', 'ADMIN')")
    public ResponseEntity<byte[]> verPlano(@PathVariable UUID id) {
        var tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));
        return serveFile(tramite.getArchivoPlano(), "plano.pdf");
    }

    @GetMapping("/{id}/archivos/foto")
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'INSPECTOR', 'ADMIN')")
    public ResponseEntity<byte[]> verFoto(@PathVariable UUID id) {
        var tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));
        return serveFile(tramite.getArchivoFoto(), "foto.jpg");
    }

    private ResponseEntity<byte[]> serveFile(byte[] fileBytes, String defaultName) {
        if (fileBytes == null) {
            return ResponseEntity.notFound().build();
        }
        // Determinar tipo mime simple por defecto
        org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        if (defaultName.endsWith(".pdf")) {
            mediaType = org.springframework.http.MediaType.APPLICATION_PDF;
        } else if (defaultName.endsWith(".jpg") || defaultName.endsWith(".png") || defaultName.endsWith(".jpeg")) {
            mediaType = org.springframework.http.MediaType.IMAGE_JPEG;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(fileBytes);
    }

    @PostMapping("/{id}/preferencia")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ResponseEntity<java.util.Map<String, String>> generarPreferenciaPago(@PathVariable UUID id) {
        User user = getCurrentUser();
        var tramite = tramiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (!tramite.getSolicitante().getId().equals(user.getId())) {
            throw new RuntimeException("No tiene permisos sobre este trámite");
        }

        if (tramite.getEstado() != com.example.mpct.model.enums.EstadoTramite.PENDIENTE) {
            throw new RuntimeException("El trámite no está pendiente de pago");
        }

        String initPoint = mercadoPagoService.crearPreferenciaPago(tramite);
        return ResponseEntity.ok(java.util.Map.of("url", initPoint));
    }

    @PostMapping
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ResponseEntity<TramiteResponse> crearTramite(
            @RequestParam("tipo") TipoTramite tipo,
            @RequestParam(value = "declaracionSinCambios", required = false) Boolean declaracionSinCambios,
            @RequestParam("area") Double area,
            @RequestParam("plano") MultipartFile plano,
            @RequestParam("foto") MultipartFile foto
    ) {
        return ResponseEntity.ok(tramiteService.crearTramite(getCurrentUser(), tipo, declaracionSinCambios, area, plano, foto));
    }

    @PostMapping("/{id}/pagar")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ResponseEntity<TramiteResponse> pagarTramite(
            @PathVariable UUID id,
            @RequestParam("transactionId") String transactionId
    ) {
        return ResponseEntity.ok(tramiteService.pagarTramite(getCurrentUser(), id, transactionId));
    }

    @PutMapping("/{id}/archivos")
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ResponseEntity<TramiteResponse> actualizarArchivos(
            @PathVariable UUID id,
            @RequestParam(value = "plano", required = false) MultipartFile plano,
            @RequestParam(value = "foto", required = false) MultipartFile foto
    ) {
        return ResponseEntity.ok(tramiteService.actualizarArchivos(getCurrentUser(), id, plano, foto));
    }

    @GetMapping
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ResponseEntity<List<TramiteResponse>> misTramites() {
        return ResponseEntity.ok(tramiteService.misTramites(getCurrentUser()));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }
}
