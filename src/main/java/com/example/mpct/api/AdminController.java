package com.example.mpct.api;

import com.example.mpct.model.entity.Configuracion;
import com.example.mpct.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ConfiguracionRepository configuracionRepository;

    @GetMapping("/configuraciones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Configuracion>> getAllConfiguraciones() {
        return ResponseEntity.ok(configuracionRepository.findAll());
    }

    @PutMapping("/configuraciones/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Configuracion> updatePrecio(
            @PathVariable String clave,
            @RequestParam("valor") BigDecimal valor
    ) {
        Configuracion conf = configuracionRepository.findByClave(clave)
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada"));
        
        conf.setValor(valor);
        return ResponseEntity.ok(configuracionRepository.save(conf));
    }
}
