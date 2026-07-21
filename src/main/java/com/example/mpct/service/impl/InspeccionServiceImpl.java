package com.example.mpct.service.impl;

import com.example.mpct.service.*;

import com.example.mpct.model.entity.*;
import com.example.mpct.model.enums.*;
import com.example.mpct.repository.InspeccionRepository;
import com.example.mpct.repository.FeriadoRepository;
import com.example.mpct.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class InspeccionServiceImpl implements InspeccionService {

    private final InspeccionRepository inspeccionRepository;
    private final TramiteRepository tramiteRepository;
    private final LicenciaService licenciaService;
    private final com.example.mpct.service.TramiteService tramiteService;
    private final FeriadoRepository feriadoRepository;
    private final InspeccionSchedulingService inspeccionSchedulingService;

    public InspeccionServiceImpl(InspeccionRepository inspeccionRepository, TramiteRepository tramiteRepository, LicenciaService licenciaService, @org.springframework.context.annotation.Lazy com.example.mpct.service.TramiteService tramiteService, FeriadoRepository feriadoRepository, @org.springframework.context.annotation.Lazy InspeccionSchedulingService inspeccionSchedulingService) {
        this.inspeccionRepository = inspeccionRepository;
        this.tramiteRepository = tramiteRepository;
        this.licenciaService = licenciaService;
        this.tramiteService = tramiteService;
        this.feriadoRepository = feriadoRepository;
        this.inspeccionSchedulingService = inspeccionSchedulingService;
    }

    @Transactional
    public Inspeccion programarInspeccionInicial(Tramite tramite) {
        // Se programa para 3 días hábiles después del pago por defecto
        LocalDateTime fechaProgramada = sumarDiasHabiles(LocalDateTime.now(), 3);
        
        Inspeccion inspeccion = Inspeccion.builder()
                .tramite(tramite)
                .numeroInspeccion(1)
                .estado(EstadoInspeccion.PROGRAMADA)
                .fechaProgramada(fechaProgramada)
                .build();
        
        return inspeccionRepository.save(inspeccion);
    }

    @Override
    @Transactional
    public Inspeccion evaluarInspeccion(User inspector, UUID inspeccionId, boolean conforme, String observaciones, String archivosObservados) {
        Inspeccion inspeccion = inspeccionRepository.findById(inspeccionId)
                .orElseThrow(() -> new RuntimeException("Inspección no encontrada"));

        if (inspeccion.getEstado() != EstadoInspeccion.PROGRAMADA) {
            throw new RuntimeException("La inspección ya fue evaluada.");
        }

        inspeccion.setInspector(inspector);
        inspeccion.setFechaRealizada(LocalDateTime.now());
        inspeccion.setObservaciones(observaciones);

        Tramite tramite = inspeccion.getTramite();

        if (conforme) {
            inspeccion.setEstado(EstadoInspeccion.CONFORME);
            tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.APROBADO, null);
            tramite.setObservacionesGenerales(null);
            tramite.setArchivosObservados(null);
            
            // Generar licencia automáticamente
            licenciaService.generarLicencia(tramite);
            
        } else {
            inspeccion.setEstado(EstadoInspeccion.OBSERVADA);
            tramite.setObservacionesGenerales(observaciones);
            tramite.setArchivosObservados(archivosObservados);
            
            if (inspeccion.getNumeroInspeccion() == 1) {
                tramite.setFechaLimiteSubsanacion(sumarDiasHabiles(LocalDateTime.now(), 30));
                tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.OBSERVADO, observaciones);
                // La 2da visita se programará automáticamente cuando el usuario subsane (actualice archivos)
            } else {
                // Segunda inspección desaprobada = Trámite Rechazado
                tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.RECHAZADO, "Segunda inspección desaprobada.");
            }
        }

        return inspeccionRepository.save(inspeccion);
    }

    public LocalDateTime sumarDiasHabiles(LocalDateTime fecha, int dias) {
        LocalDateTime result = fecha;
        int addedDays = 0;
        
        while (addedDays < dias) {
            result = result.plusDays(1);
            boolean isWeekend = result.getDayOfWeek() == java.time.DayOfWeek.SATURDAY || result.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            boolean isFeriado = feriadoRepository.existsByFecha(result.toLocalDate());
            
            if (!isWeekend && !isFeriado) {
                addedDays++;
            }
        }
        return result;
    }

    @Override
    public java.util.List<Inspeccion> obtenerInspeccionesPendientes() {
        return inspeccionRepository.findByEstado(EstadoInspeccion.PROGRAMADA);
    }

    @Override
    public java.util.List<Inspeccion> obtenerTodasInspeccionesPendientes(User inspector) {
        return inspeccionRepository.findByInspectorIdAndEstado(inspector.getId(), EstadoInspeccion.PROGRAMADA);
    }

    @Override
    public java.util.List<Inspeccion> obtenerInspeccionesDelDia(User inspector) {
        return inspeccionRepository.findByEstadoAndFechaProgramada(EstadoInspeccion.PROGRAMADA, LocalDateTime.now());
    }

    @Override
    public Inspeccion obtenerInspeccionPorId(UUID id) {
        return inspeccionRepository.findById(id).orElseThrow(() -> new RuntimeException("Inspección no encontrada"));
    }
}
