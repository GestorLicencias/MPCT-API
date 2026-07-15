package com.example.mpct;

import com.example.mpct.dto.auth.AuthRequest;
import com.example.mpct.model.entity.User;
import com.example.mpct.model.entity.Tramite;
import com.example.mpct.model.enums.EstadoTramite;
import com.example.mpct.model.enums.Role;
import com.example.mpct.model.enums.TipoTramite;
import com.example.mpct.repository.TramiteRepository;
import com.example.mpct.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class E2ELegacyFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TramiteRepository tramiteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "legacyuser@mpct.gob.pe";
    private static final String TEST_RUC = "20999999999";

    @BeforeEach
    void setup() {
        if (userRepository.findByEmail(TEST_EMAIL).isEmpty()) {
            User user = User.builder()
                    .email(TEST_EMAIL)
                    .passwordHash(passwordEncoder.encode("password123"))
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(user);
        }

        Tramite tramite = Tramite.builder()
                .ruc(TEST_RUC)
                .razonSocial("Legacy Company")
                .estado(EstadoTramite.APROBADO)
                .tipo(TipoTramite.NUEVO)
                .montoCobrado(new BigDecimal("150.00"))
                .domicilioFiscal("Av. Principal 123")
                .representanteLegal("Juan Perez")
                .dni("12345678")
                .rubro("Comercial")
                .area(new BigDecimal("100.00"))
                .archivoFoto(new byte[0])
                .archivoPlano(new byte[0])
                .build();
        tramiteRepository.save(tramite);
    }

    @AfterEach
    void cleanup() {
        tramiteRepository.findTopByRucOrderByCreatedAtDesc(TEST_RUC).ifPresent(tramiteRepository::delete);
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    void testLegacyLoginFlow() throws Exception {
        String loginPayload = """
                {
                    "email": "%s",
                    "password": "password123"
                }
                """.formatted(TEST_EMAIL);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void testLegacySeguimientoFlow() throws Exception {
        mockMvc.perform(get("/api/v1/tramites/" + TEST_RUC)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value(TEST_RUC))
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.razonSocial").value("Legacy Company"));
    }
}
