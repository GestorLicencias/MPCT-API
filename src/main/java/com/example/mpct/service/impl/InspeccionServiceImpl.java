package com.example.mpct.service.impl;

import com.example.mpct.service.*;

import com.example.mpct.model.entity.*;
import com.example.mpct.model.enums.*;
import com.example.mpct.repository.InspeccionRepository;
import com.example.mpct.repository.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InspeccionServiceImpl implements InspeccionService {

    private final InspeccionRepository inspeccionRepository;
    private final TramiteRepository tramiteRepository;
    private final LicenciaService licenciaService;

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
            tramite.setEstado(EstadoTramite.APROBADO);
            tramite.setObservacionesGenerales(null);
            tramite.setArchivosObservados(null);
            
            // Generar licencia automáticamente
            licenciaService.generarLicencia(tramite);
            
        } else {
            inspeccion.setEstado(EstadoInspeccion.OBSERVADA);
            tramite.setEstado(EstadoTramite.OBSERVADO);
            tramite.setObservacionesGenerales(observaciones);
            tramite.setArchivosObservados(archivosObservados);
            
            if (inspeccion.getNumeroInspeccion() == 1) {
                tramite.setEstado(EstadoTramite.OBSERVADO);
                // Programar 2da inspección para exactamente 30 días hábiles
                LocalDateTime nuevaFecha = sumarDiasHabiles(LocalDateTime.now(), 30);
                
                Inspeccion segundaInspeccion = Inspeccion.builder()
                        .tramite(tramite)
                        .numeroInspeccion(2)
                        .estado(EstadoInspeccion.PROGRAMADA)
                        .fechaProgramada(nuevaFecha)
                        .build();
                inspeccionRepository.save(segundaInspeccion);
                
            } else {
                // Segunda inspección desaprobada = Trámite Denegado
                tramite.setEstado(EstadoTramite.DENEGADO);
            }
        }

        tramiteRepository.save(tramite);
        return inspeccionRepository.save(inspeccion);
    }

    private LocalDateTime sumarDiasHabiles(LocalDateTime fecha, int dias) {
        LocalDateTime result = fecha;
        int addedDays = 0;
        while (addedDays < dias) {
            result = result.plusDays(1);
            if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
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
    public Inspeccion obtenerInspeccionPorId(UUID id) {
        return inspeccionRepository.findById(id).orElseThrow(() -> new RuntimeException("Inspección no encontrada"));
    }
}
