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
    public ResponseEntity<java.util.List<Inspeccion>> getInspeccionesPendientes() {
        return ResponseEntity.ok(inspeccionService.obtenerInspeccionesPendientes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<Inspeccion> getInspeccion(@PathVariable UUID id) {
        return ResponseEntity.ok(inspeccionService.obtenerInspeccionPorId(id));
    }

    @PostMapping("/{id}/evaluar")
    @PreAuthorize("hasRole('INSPECTOR')")
    public ResponseEntity<Inspeccion> evaluarInspeccion(
            @PathVariable UUID id,
            @RequestParam("conforme") boolean conforme,
            @RequestParam(value = "observaciones", required = false) String observaciones
    ) {
        if (!conforme && (observaciones == null || observaciones.trim().isEmpty())) {
            throw new RuntimeException("Las observaciones son obligatorias si la inspección no es conforme.");
        }
        return ResponseEntity.ok(inspeccionService.evaluarInspeccion(getCurrentUser(), id, conforme, observaciones));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no autenticado"));
    }
}
