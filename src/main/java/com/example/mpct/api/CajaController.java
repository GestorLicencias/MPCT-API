package com.example.mpct.api;

import com.example.mpct.dto.caja.AbrirCajaRequest;
import com.example.mpct.dto.caja.CajaEstadoResponse;
import com.example.mpct.dto.caja.PagoPresencialRequest;
import com.example.mpct.service.CajaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/caja")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CAJERO') or hasRole('ADMIN')")
public class CajaController {

    private final CajaService cajaService;

    @PostMapping("/abrir")
    public ResponseEntity<?> abrirCaja(@jakarta.validation.Valid @RequestBody AbrirCajaRequest request, Principal principal) {
        cajaService.abrirCaja(principal.getName(), request);
        return ResponseEntity.ok(java.util.Map.of("message", "Caja abierta exitosamente"));
    }

    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrarCaja(@RequestBody java.util.Map<String, java.math.BigDecimal> request, Principal principal) {
        cajaService.cerrarCaja(principal.getName(), request.get("montoFisico"));
        return ResponseEntity.ok(java.util.Map.of("message", "Caja cerrada exitosamente"));
    }

    @GetMapping("/estado")
    public ResponseEntity<CajaEstadoResponse> estadoCaja(Principal principal) {
        return ResponseEntity.ok(cajaService.obtenerEstadoCaja(principal.getName()));
    }

    @PostMapping("/pago-presencial")
    public ResponseEntity<?> pagoPresencial(@jakarta.validation.Valid @RequestBody PagoPresencialRequest request, Principal principal) {
        return ResponseEntity.ok(cajaService.registrarPagoPresencial(principal.getName(), request));
    }

    @GetMapping("/licencias-alertas")
    public ResponseEntity<?> obtenerAlertasLicencias() {
        return ResponseEntity.ok(cajaService.obtenerAlertasLicencias());
    }

    @PostMapping("/licencias-alertas/{ruc}/recordatorio")
    public ResponseEntity<?> enviarRecordatorio(@PathVariable String ruc) {
        cajaService.enviarRecordatorioLicencia(ruc);
        return ResponseEntity.ok(java.util.Map.of("message", "Recordatorio enviado exitosamente"));
    }

    @PostMapping("/pagos/{id}/validar")
    @PreAuthorize("hasRole('CAJERO')")
    public ResponseEntity<?> validarPago(@PathVariable java.util.UUID id, @RequestParam("aprobado") boolean aprobado, @RequestParam(value = "motivo", required = false) String motivo, Principal principal) {
        try {
            String mensaje = cajaService.validarPagoCajero(id, aprobado, principal.getName(), motivo);
            return ResponseEntity.ok(java.util.Map.of("message", mensaje));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
