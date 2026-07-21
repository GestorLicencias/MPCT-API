package com.example.mpct.api;

import com.example.mpct.dto.tramite.TramiteResponse;
import com.example.mpct.model.enums.TipoTramite;
import com.example.mpct.service.TramiteService;
import com.example.mpct.service.LicenciaService;
import com.example.mpct.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/tramites")
@RequiredArgsConstructor
public class TramiteController {

    private final TramiteService tramiteService;
    private final LicenciaService licenciaService;
    private final TramiteRepository tramiteRepository;
    private final com.example.mpct.service.MercadoPagoService mercadoPagoService;
    private final com.example.mpct.service.DniScrapingService dniScrapingService;
    private final com.example.mpct.service.RucValidationService rucValidationService;

    @GetMapping("/ruc/{ruc}/validar")
    public ResponseEntity<com.example.mpct.dto.tramite.ValidacionRucResponse> validarRuc(@PathVariable String ruc, HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ResponseEntity.ok(rucValidationService.validarRuc(ruc, ip));
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<java.util.Map<String, String>> consultarDni(@PathVariable String dni) {
        String nombreCompleto = dniScrapingService.obtenerNombresPorDni(dni);
        return ResponseEntity.ok(java.util.Map.of("nombreCompleto", nombreCompleto));
    }

    @PostMapping("/{ruc}/mercadopago")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<java.util.Map<String, String>> generarPreferenciaMercadoPago(@PathVariable String ruc) {
        var tramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));
        
        if (tramite.getEstado() != com.example.mpct.model.enums.EstadoTramite.PENDIENTE_PAGO) {
            throw new RuntimeException("El trámite no está pendiente de pago");
        }

        String initPoint = mercadoPagoService.crearPreferenciaPago(tramite);
        return ResponseEntity.ok(java.util.Map.of("initPoint", initPoint));
    }

    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<?> recibirWebhookMercadoPago(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestBody(required = false) String payload) {
        
        // MercadoPago envía notificaciones de varios tipos. Solo nos interesa 'payment'
        if ("payment".equals(type) && dataId != null) {
            mercadoPagoService.procesarWebhook(dataId);
        }
        return ResponseEntity.ok().build(); // Siempre responder 200 OK para que MP deje de enviar la notificación
    }

    @GetMapping("/{ruc}")
    public ResponseEntity<TramiteResponse> obtenerTramite(@PathVariable String ruc) {
        return ResponseEntity.ok(tramiteService.obtenerTramitePorRuc(ruc));
    }

    @GetMapping("/{ruc}/certificado")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarCertificado(@PathVariable String ruc) {
        byte[] pdfBytes = licenciaService.generarCertificadoPorRuc(ruc);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificado-" + ruc + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/{ruc}/archivos/plano")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<byte[]> verPlano(@PathVariable String ruc, @RequestParam(value = "download", defaultValue = "false") boolean download) {
        var tramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));
        return serveFile(tramite.getArchivoPlano(), "plano.pdf", download);
    }



    private ResponseEntity<byte[]> serveFile(byte[] fileBytes, String defaultName, boolean download) {
        if (fileBytes == null || fileBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
        String extension = "";

        if (fileBytes.length > 4) {
            if (fileBytes[0] == 0x25 && fileBytes[1] == 0x50 && fileBytes[2] == 0x44 && fileBytes[3] == 0x46) {
                mediaType = org.springframework.http.MediaType.APPLICATION_PDF;
                extension = ".pdf";
            } else if ((fileBytes[0] & 0xFF) == 0xFF && (fileBytes[1] & 0xFF) == 0xD8 && (fileBytes[2] & 0xFF) == 0xFF) {
                mediaType = org.springframework.http.MediaType.IMAGE_JPEG;
                extension = ".jpg";
            } else if ((fileBytes[0] & 0xFF) == 0x89 && fileBytes[1] == 0x50 && fileBytes[2] == 0x4E && fileBytes[3] == 0x47) {
                mediaType = org.springframework.http.MediaType.IMAGE_PNG;
                extension = ".png";
            }
        }

        if (extension.isEmpty()) {
            if (defaultName.endsWith(".pdf")) {
                mediaType = org.springframework.http.MediaType.APPLICATION_PDF;
                extension = ".pdf";
            } else {
                mediaType = org.springframework.http.MediaType.IMAGE_JPEG;
                extension = ".jpg";
            }
        }

        String filename = defaultName.contains(".") ? defaultName.substring(0, defaultName.lastIndexOf('.')) + extension : defaultName + extension;

        org.springframework.http.ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);
        if (download) {
            responseBuilder.header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        }

        return responseBuilder.body(fileBytes);
    }



    @PostMapping
    public ResponseEntity<TramiteResponse> crearTramite(
            @RequestParam("ruc") String ruc,
            @RequestParam("representanteLegal") String representanteLegal,
            @RequestParam("rubro") String rubro,
            @RequestParam("dni") String dni,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "direccion", required = false) String direccion,
            @RequestParam("area") java.math.BigDecimal area,
            @RequestParam("tipo") TipoTramite tipo,
            @RequestParam(value = "plano", required = false) MultipartFile plano
    ) {
        return ResponseEntity.ok(tramiteService.crearTramite(ruc, representanteLegal, rubro, dni, email, direccion, area, tipo, plano));
    }

    @PostMapping("/{ruc}/pagar")
    public ResponseEntity<TramiteResponse> pagarTramite(
            @PathVariable String ruc,
            @RequestParam("metodoPago") String metodoPago,
            @RequestParam(value = "voucher", required = false) MultipartFile voucher,
            @RequestParam(value = "transactionId", required = false) String transactionId,
            @RequestParam(value = "numeroComprobante", required = false) String numeroComprobante
    ) {
        return ResponseEntity.ok(tramiteService.pagarTramite(ruc, metodoPago, voucher, transactionId, numeroComprobante));
    }

    @PatchMapping("/{ruc}/archivos")
    public ResponseEntity<TramiteResponse> actualizarArchivos(
            @PathVariable String ruc,
            @RequestParam(value = "plano", required = false) MultipartFile plano
    ) {
        return ResponseEntity.ok(tramiteService.actualizarArchivos(ruc, plano));
    }

    @GetMapping("/validar/{numeroLicencia}")
    public ResponseEntity<java.util.Map<String, Object>> validarLicencia(@PathVariable String numeroLicencia) {
        var licenciaOpt = licenciaService.obtenerPorNumero(numeroLicencia);
        if (licenciaOpt.isEmpty()) {
            return ResponseEntity.status(404).body(java.util.Map.of("valida", false, "mensaje", "Licencia no encontrada"));
        }
        var licencia = licenciaOpt.get();
        boolean isVigente = licencia.getEstado() == com.example.mpct.model.enums.EstadoLicencia.VIGENTE;
        
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("valida", isVigente);
        data.put("estado", licencia.getEstado().name());
        data.put("razonSocial", licencia.getTramite().getRazonSocial());
        data.put("ruc", licencia.getTramite().getRuc());
        data.put("direccion", licencia.getTramite().getDomicilioFiscal());
        data.put("rubro", licencia.getTramite().getRubro());
        data.put("fechaEmision", licencia.getFechaEmision().toString());
        data.put("fechaVencimiento", licencia.getFechaVencimiento().toString());
        
        return ResponseEntity.ok(data);
    }
}
