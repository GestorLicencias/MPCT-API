package com.example.mpct.api;

import com.example.mpct.model.entity.Inspeccion;
import com.example.mpct.model.entity.User;
import com.example.mpct.repository.UserRepository;
import com.example.mpct.service.InspeccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inspecciones")
@RequiredArgsConstructor
public class InspeccionController {

    private final InspeccionService inspeccionService;
    private final UserRepository userRepository;

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('INSPECTOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getInspeccionesPendientes() {
        var inspecciones = inspeccionService.obtenerInspeccionesPendientes();
        var dtos = inspecciones.stream().map(this::mapInspeccionToDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('INSPECTOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<java.util.Map<String, Object>> getInspeccion(@PathVariable UUID id) {
        return ResponseEntity.ok(mapInspeccionToDto(inspeccionService.obtenerInspeccionPorId(id)));
    }

    @PostMapping("/{id}/evaluar")
    @PreAuthorize("hasRole('INSPECTOR')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<java.util.Map<String, Object>> evaluarInspeccion(
            @PathVariable UUID id,
            @RequestParam("conforme") boolean conforme,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            @RequestParam(value = "archivosObservados", required = false) String archivosObservados
    ) {
        if (!conforme && (observaciones == null || observaciones.trim().isEmpty())) {
            throw new RuntimeException("Las observaciones son obligatorias si la inspección no es conforme.");
        }
        var insp = inspeccionService.evaluarInspeccion(getCurrentUser(), id, conforme, observaciones, archivosObservados);
        return ResponseEntity.ok(mapInspeccionToDto(insp));
    }

    private java.util.Map<String, Object> mapInspeccionToDto(Inspeccion insp) {
        java.util.Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", insp.getId());
        dto.put("estado", insp.getEstado());
        dto.put("numeroInspeccion", insp.getNumeroInspeccion());
        dto.put("fechaProgramada", insp.getFechaProgramada());
        dto.put("fechaRealizada", insp.getFechaRealizada());
        dto.put("observaciones", insp.getObservaciones());
        dto.put("createdAt", insp.getFechaProgramada() != null ? insp.getFechaProgramada().toString() : java.time.LocalDateTime.now().toString());

        java.util.Map<String, Object> tramiteDto = new java.util.HashMap<>();
        tramiteDto.put("id", insp.getTramite().getId());
        tramiteDto.put("ruc", insp.getTramite().getRuc());
        tramiteDto.put("razonSocial", insp.getTramite().getRazonSocial());
        tramiteDto.put("rubro", insp.getTramite().getRubro());
        tramiteDto.put("domicilioFiscal", insp.getTramite().getDomicilioFiscal());
        tramiteDto.put("representanteLegal", insp.getTramite().getRepresentanteLegal());
        tramiteDto.put("dni", insp.getTramite().getDni());
        
        java.util.Map<String, Object> solicitanteDto = new java.util.HashMap<>();
        if (insp.getTramite().getSolicitante() != null) {
            solicitanteDto.put("email", insp.getTramite().getSolicitante().getEmail());
        } else {
            solicitanteDto.put("email", "Desconocido");
        }
        tramiteDto.put("solicitante", solicitanteDto);
        
        tramiteDto.put("area", insp.getTramite().getArea());
        tramiteDto.put("tipo", insp.getTramite().getTipo());
        tramiteDto.put("observacionesGenerales", insp.getTramite().getObservacionesGenerales());
        tramiteDto.put("archivosObservados", insp.getTramite().getArchivosObservados());
        
        // Mapear URLs de fotos si existen
        if (insp.getTramite().getArchivoFoto() != null) {
            tramiteDto.put("archivoFotoUrl", "/api/v1/tramites/" + insp.getTramite().getRuc() + "/archivos/foto");
        }
        if (insp.getTramite().getArchivoFoto2() != null) {
            tramiteDto.put("archivoFoto2Url", "/api/v1/tramites/" + insp.getTramite().getRuc() + "/archivos/foto2");
        }
        if (insp.getTramite().getArchivoFoto3() != null) {
            tramiteDto.put("archivoFoto3Url", "/api/v1/tramites/" + insp.getTramite().getRuc() + "/archivos/foto3");
        }
        if (insp.getTramite().getArchivoFoto4() != null) {
            tramiteDto.put("archivoFoto4Url", "/api/v1/tramites/" + insp.getTramite().getRuc() + "/archivos/foto4");
        }

        dto.put("tramite", tramiteDto);
        return dto;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }
}
