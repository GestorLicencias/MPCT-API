package com.example.mpct.service;

import com.example.mpct.model.entity.Inspeccion;
import com.example.mpct.model.entity.Tramite;
import com.example.mpct.model.entity.User;
import com.example.mpct.model.enums.EstadoInspeccion;
import com.example.mpct.model.enums.EstadoTramite;
import com.example.mpct.repository.InspeccionRepository;
import com.example.mpct.repository.TramiteRepository;
import com.example.mpct.service.impl.InspeccionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TramiteStateMachineTest {

    @Mock
    private InspeccionRepository inspeccionRepository;

    @Mock
    private TramiteRepository tramiteRepository;

    @Mock
    private com.example.mpct.service.TramiteService tramiteService;

    @Mock
    private com.example.mpct.repository.FeriadoRepository feriadoRepository;

    @Mock
    private InspeccionSchedulingService inspeccionSchedulingService;

    @Mock
    private LicenciaService licenciaService;

    @InjectMocks
    private InspeccionServiceImpl inspeccionService;

    @Test
    void testPrimerObservacionCambiaAObservadoYProgramaSubsanacion() {
        Tramite tramite = new Tramite();
        tramite.setId(UUID.randomUUID());
        tramite.setEstado(EstadoTramite.PROGRAMADO);

        Inspeccion inspeccion = new Inspeccion();
        inspeccion.setId(UUID.randomUUID());
        inspeccion.setTramite(tramite);
        inspeccion.setNumeroInspeccion(1);
        inspeccion.setEstado(EstadoInspeccion.PROGRAMADA);

        when(inspeccionRepository.findById(inspeccion.getId())).thenReturn(Optional.of(inspeccion));
        when(inspeccionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(feriadoRepository.existsByFecha(any())).thenReturn(false);

        inspeccionService.evaluarInspeccion(new User(), inspeccion.getId(), false, "Falta extintor", "foto.jpg");

        verify(tramiteService, times(1)).actualizarEstadoTramite(eq(tramite), eq(EstadoTramite.OBSERVADO), eq("Falta extintor"));
        assertEquals(EstadoInspeccion.OBSERVADA, inspeccion.getEstado());
        verify(inspeccionSchedulingService, times(1)).programarInspeccion(eq(tramite), eq(2), eq(30));
    }

    @Test
    void testSegundaObservacionTerminaElTramiteComoTerminado() {
        Tramite tramite = new Tramite();
        tramite.setId(UUID.randomUUID());
        tramite.setEstado(EstadoTramite.EN_SUBSANACION);

        Inspeccion inspeccion = new Inspeccion();
        inspeccion.setId(UUID.randomUUID());
        inspeccion.setTramite(tramite);
        inspeccion.setNumeroInspeccion(2);
        inspeccion.setEstado(EstadoInspeccion.PROGRAMADA);

        when(inspeccionRepository.findById(inspeccion.getId())).thenReturn(Optional.of(inspeccion));
        when(inspeccionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        inspeccionService.evaluarInspeccion(new User(), inspeccion.getId(), false, "Sigue sin extintor", "foto2.jpg");

        // Regla: 2 observaciones -> TERMINADO (Rechazado)
        verify(tramiteService, times(1)).actualizarEstadoTramite(eq(tramite), eq(EstadoTramite.RECHAZADO), eq("Segunda inspección desaprobada."));
        assertEquals(EstadoInspeccion.OBSERVADA, inspeccion.getEstado());
        
        // No se debe haber programado una 3era
        verify(inspeccionSchedulingService, never()).programarInspeccion(any(), anyInt(), anyInt());
    }
}
