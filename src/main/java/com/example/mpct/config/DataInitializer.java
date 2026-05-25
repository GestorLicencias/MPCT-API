package com.example.mpct.config;

import com.example.mpct.model.entity.User;
import com.example.mpct.model.entity.UserProfile;
import com.example.mpct.model.enums.Role;
import com.example.mpct.repository.UserProfileRepository;
import com.example.mpct.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.mpct.repository.ConfiguracionRepository configuracionRepository;

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
            
            UserProfile adminProfile = UserProfile.builder()
                    .user(admin)
                    .ruc("00000000001")
                    .razonSocial("MUNICIPALIDAD PROVINCIAL - ADMIN")
                    .domicilioFiscal("Palacio Municipal")
                    .representanteLegal("Alcalde")
                    .build();
            userProfileRepository.save(adminProfile);
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

            UserProfile inspectorProfile = UserProfile.builder()
                    .user(inspector)
                    .ruc("00000000002")
                    .razonSocial("MUNICIPALIDAD PROVINCIAL - INSPECTOR")
                    .domicilioFiscal("Área de Fiscalización")
                    .representanteLegal("Jefe de Inspecciones")
                    .build();
            userProfileRepository.save(inspectorProfile);
        }

        // Inicializar Configuraciones base
        if (configuracionRepository.findByClave("PRECIO_NUEVO").isEmpty()) {
            com.example.mpct.model.entity.Configuracion confNuevo = com.example.mpct.model.entity.Configuracion.builder()
                    .clave("PRECIO_NUEVO")
                    .valor(new java.math.BigDecimal("380.00"))
                    .descripcion("Precio base para trámite Nuevo")
                    .build();
            configuracionRepository.save(confNuevo);
        }
        
        if (configuracionRepository.findByClave("PRECIO_RENOVACION").isEmpty()) {
            com.example.mpct.model.entity.Configuracion confRenovacion = com.example.mpct.model.entity.Configuracion.builder()
                    .clave("PRECIO_RENOVACION")
                    .valor(new java.math.BigDecimal("180.00"))
                    .descripcion("Precio base para trámite Renovación")
                    .build();
            configuracionRepository.save(confRenovacion);
        }
    }
}
