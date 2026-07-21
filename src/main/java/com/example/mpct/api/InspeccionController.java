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
    private final com.example.mpct.service.TaskLockService taskLockService;

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('INSPECTOR')")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getInspeccionesPendientes(
            @RequestParam(value = "soloHoy", defaultValue = "true") boolean soloHoy
    ) {
        var inspecciones = soloHoy ? 
                inspeccionService.obtenerInspeccionesDelDia(getCurrentUser()) : 
                inspeccionService.obtenerInspeccionesPendientes();
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
        try {
            if (!conforme && (observaciones == null || observaciones.trim().isEmpty())) {
                throw new RuntimeException("Las observaciones son obligatorias si la inspección no es conforme.");
            }
            var insp = inspeccionService.evaluarInspeccion(getCurrentUser(), id, conforme, observaciones, archivosObservados);
            return ResponseEntity.ok(mapInspeccionToDto(insp));
        } finally {
            taskLockService.unlockTask(id.toString(), getCurrentUser().getEmail());
        }
    }

    @PostMapping("/{id}/lock")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<?> lockInspeccion(@PathVariable UUID id) {
        boolean success = taskLockService.lockTask(id.toString(), getCurrentUser().getEmail());
        if (success) {
            return ResponseEntity.ok(java.util.Map.of("message", "Inspección bloqueada exitosamente."));
        } else {
            String owner = taskLockService.getLockOwner(id.toString()).orElse("desconocido");
            return ResponseEntity.status(409).body(java.util.Map.of("message", "La inspección está siendo evaluada por: " + owner));
        }
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<?> unlockInspeccion(@PathVariable UUID id) {
        taskLockService.unlockTask(id.toString(), getCurrentUser().getEmail());
        return ResponseEntity.ok(java.util.Map.of("message", "Inspección desbloqueada exitosamente."));
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
        dto.put("lockedBy", taskLockService.getLockOwner(insp.getId().toString()).orElse(null));

        java.util.Map<String, Object> tramiteDto = new java.util.HashMap<>();
        tramiteDto.put("id", insp.getTramite().getId());
        tramiteDto.put("ruc", insp.getTramite().getRuc());
        tramiteDto.put("razonSocial", insp.getTramite().getRazonSocial());
        tramiteDto.put("rubro", insp.getTramite().getRubro());
        tramiteDto.put("domicilioFiscal", insp.getTramite().getDomicilioFiscal());
        tramiteDto.put("representanteLegal", insp.getTramite().getRepresentanteLegal());
        tramiteDto.put("dni", insp.getTramite().getDni());
        
        java.util.Map<String, Object> solicitanteDto = new java.util.HashMap<>();
        solicitanteDto.put("email", insp.getTramite().getEmail());
        tramiteDto.put("solicitante", solicitanteDto);
        
        tramiteDto.put("area", insp.getTramite().getArea());
        tramiteDto.put("tipo", insp.getTramite().getTipo());
        tramiteDto.put("observacionesGenerales", insp.getTramite().getObservacionesGenerales());
        tramiteDto.put("archivosObservados", insp.getTramite().getArchivosObservados());
        


        dto.put("tramite", tramiteDto);
        return dto;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }
}
