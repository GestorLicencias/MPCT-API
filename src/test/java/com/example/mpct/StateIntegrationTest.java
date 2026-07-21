package com.example.mpct;

import com.example.mpct.model.entity.Inspeccion;
import com.example.mpct.model.entity.Tramite;
import com.example.mpct.model.enums.EstadoInspeccion;
import com.example.mpct.model.enums.EstadoTramite;
import com.example.mpct.model.enums.TipoTramite;
import com.example.mpct.repository.InspeccionRepository;
import com.example.mpct.repository.TramiteRepository;
import com.example.mpct.service.TramiteService;
import com.example.mpct.service.impl.ScheduledJobsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local") // Usa la BD Postgres local en puerto 5434
public class StateIntegrationTest {

    @Autowired
    private TramiteService tramiteService;

    @Autowired
    private TramiteRepository tramiteRepository;

    @Autowired
    private InspeccionRepository inspeccionRepository;

    @Autowired
    private ScheduledJobsService scheduledJobsService;

    @Test
    @Transactional
    public void testTramitePagadoSinInspeccion() {
        // Preparar
        Tramite tramite = Tramite.builder()
                .ruc("20123456789")
                .razonSocial("Test Empresa")
                .domicilioFiscal("Av Test 123")
                .representanteLegal("Juan Perez")
                .rubro("Comercio")
                .estado(EstadoTramite.PENDIENTE_PAGO)
                .requiereInspeccion(false)
                .email("test@example.com")
                .archivoPlano(new byte[0])
                .montoCobrado(new java.math.BigDecimal("150.00"))
                .build();
        tramite = tramiteRepository.save(tramite);

        // Actuar: Forzar transición a pagado (que dispara la lógica)
        tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.PAGADO, null);

        // Verificar
        Tramite guardado = tramiteRepository.findById(tramite.getId()).orElseThrow();
        assertThat(guardado.getEstado()).isEqualTo(EstadoTramite.PAGADO);
    }

    @Test
    @Transactional
    public void testCronNoAbandonaSiHaySegundaInspeccionProgramada() {
        // Preparar
        Tramite tramite = Tramite.builder()
                .ruc("20987654321")
                .razonSocial("Test Empresa 2")
                .domicilioFiscal("Av Test 456")
                .representanteLegal("Maria Gomez")
                .rubro("Servicios")
                .estado(EstadoTramite.OBSERVADO)
                .fechaLimiteSubsanacion(LocalDateTime.now().minusDays(1)) // Vencido hace 1 día
                .email("test@example.com")
                .archivoPlano(new byte[0])
                .montoCobrado(new java.math.BigDecimal("150.00"))
                .build();
        tramite = tramiteRepository.save(tramite);

        Inspeccion inspeccion2 = Inspeccion.builder()
                .tramite(tramite)
                .numeroInspeccion(2)
                .estado(EstadoInspeccion.PROGRAMADA) // 2da inspección en cola
                .fechaProgramada(LocalDateTime.now().plusDays(2))
                .build();
        inspeccionRepository.save(inspeccion2);

        // Actuar: Ejecutar el cron
        scheduledJobsService.vencerTramitesObservados();

        // Verificar
        Tramite guardado = tramiteRepository.findById(tramite.getId()).orElseThrow();
        assertThat(guardado.getEstado()).isEqualTo(EstadoTramite.OBSERVADO); // No cambió a ABANDONADO
    }

    @Test
    @Transactional
    public void testCronAbandonaSiNoHaySegundaInspeccion() {
        // Preparar
        Tramite tramite = Tramite.builder()
                .ruc("20555555555")
                .razonSocial("Test Empresa 3")
                .domicilioFiscal("Av Test 789")
                .representanteLegal("Carlos Perez")
                .rubro("Produccion")
                .estado(EstadoTramite.OBSERVADO)
                .fechaLimiteSubsanacion(LocalDateTime.now().minusDays(1)) // Vencido
                .email("test@example.com")
                .archivoPlano(new byte[0])
                .montoCobrado(new java.math.BigDecimal("150.00"))
                .build();
        tramite = tramiteRepository.save(tramite);

        // No guardamos ninguna inspección programada

        // Actuar
        scheduledJobsService.vencerTramitesObservados();

        // Verificar
        Tramite guardado = tramiteRepository.findById(tramite.getId()).orElseThrow();
        assertThat(guardado.getEstado()).isEqualTo(EstadoTramite.ABANDONADO); // Cambió correctamente
    }
}
