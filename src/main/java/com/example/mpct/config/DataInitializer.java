package com.example.mpct.config;

import com.example.mpct.model.entity.User;
import com.example.mpct.model.enums.Role;
import com.example.mpct.repository.UserRepository;
import com.example.mpct.repository.ConfiguracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracionRepository configuracionRepository;
    private final com.example.mpct.repository.TramiteRepository tramiteRepository;
    private final com.example.mpct.service.InspeccionService inspeccionService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Crear Administrador por defecto si no existe
        if (userRepository.findByEmail("admin@mpct.gob.pe").isEmpty()) {
            User admin = User.builder()
                    .email("admin@mpct.gob.pe")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
        }

        // Crear Inspector por defecto si no existe
        if (userRepository.findByEmail("inspector@mpct.gob.pe").isEmpty()) {
            User inspector = User.builder()
                    .email("inspector@mpct.gob.pe")
                    .passwordHash(passwordEncoder.encode("inspector123"))
                    .role(Role.INSPECTOR)
                    .isActive(true)
                    .build();
            userRepository.save(inspector);
        }

        // Inicializar Configuraciones base
        if (configuracionRepository.findByClave("PRECIO_LICENCIA").isEmpty()) {
            com.example.mpct.model.entity.Configuracion confLicencia = com.example.mpct.model.entity.Configuracion.builder()
                    .clave("PRECIO_LICENCIA")
                    .valor(new java.math.BigDecimal("180.00"))
                    .descripcion("Precio base para la Licencia de Funcionamiento")
                    .build();
            configuracionRepository.save(confLicencia);
        }

        // AUTO-FIX: Migrar trámites PAGADOS que no tengan inspección (por el bug anterior)
        java.util.List<com.example.mpct.model.entity.Tramite> tramitesPagados = tramiteRepository.findByEstado(com.example.mpct.model.enums.EstadoTramite.PAGADO);
        for (com.example.mpct.model.entity.Tramite t : tramitesPagados) {
            if (t.getInspecciones() == null || t.getInspecciones().isEmpty()) {
                System.out.println("Auto-Fixing: Programando inspección faltante para RUC " + t.getRuc());
                inspeccionService.programarInspeccionInicial(t);
            }
        }
    }
}
