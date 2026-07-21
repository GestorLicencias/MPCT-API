package com.example.mpct.config;

import com.example.mpct.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ConfiguracionRepository configuracionRepository;
    private final com.example.mpct.repository.TramiteRepository tramiteRepository;
    private final com.example.mpct.service.InspeccionSchedulingService inspeccionSchedulingService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Inicializar Configuraciones base
        if (configuracionRepository.findByClave("PRECIO_LICENCIA").isEmpty()) {
            com.example.mpct.model.entity.Configuracion confLicencia = com.example.mpct.model.entity.Configuracion.builder()
                    .clave("PRECIO_LICENCIA")
                    .valor(new java.math.BigDecimal("180.00"))
                    .descripcion("Precio base para la Licencia de Funcionamiento Nueva")
                    .build();
            configuracionRepository.save(confLicencia);
        }
        if (configuracionRepository.findByClave("PRECIO_RENOVACION").isEmpty()) {
            com.example.mpct.model.entity.Configuracion confRenovacion = com.example.mpct.model.entity.Configuracion.builder()
                    .clave("PRECIO_RENOVACION")
                    .valor(new java.math.BigDecimal("90.00"))
                    .descripcion("Precio para la Renovación de Licencia")
                    .build();
            configuracionRepository.save(confRenovacion);
        }
        if (configuracionRepository.findByClave("PRECIO_MODIFICACION").isEmpty()) {
            com.example.mpct.model.entity.Configuracion confModificacion = com.example.mpct.model.entity.Configuracion.builder()
                    .clave("PRECIO_MODIFICACION")
                    .valor(new java.math.BigDecimal("120.00"))
                    .descripcion("Precio para la Modificación de Solicitud/Licencia")
                    .build();
            configuracionRepository.save(confModificacion);
        }

        // AUTO-FIX: Migrar trámites PAGADOS que no tengan inspección (por el bug anterior)
        java.util.List<com.example.mpct.model.entity.Tramite> tramitesPagados = tramiteRepository.findByEstado(com.example.mpct.model.enums.EstadoTramite.PAGADO);
        for (com.example.mpct.model.entity.Tramite t : tramitesPagados) {
            if (t.getInspecciones() == null || t.getInspecciones().isEmpty()) {
                System.out.println("Auto-Fixing: Programando inspección faltante para RUC " + t.getRuc());
                inspeccionSchedulingService.programarInspeccion(t, 1, com.example.mpct.service.InspeccionSchedulingService.MINIMO_DIAS_HABILES_PRIMERA_VISITA);
            }
        }
    }
}
