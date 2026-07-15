package com.example.mpct.service.impl;

import com.example.mpct.model.entity.Inspeccion;
import com.example.mpct.model.entity.Tramite;
import com.example.mpct.model.entity.User;
import com.example.mpct.model.enums.EstadoInspeccion;
import com.example.mpct.model.enums.EstadoTramite;
import com.example.mpct.model.enums.Role;
import com.example.mpct.repository.InspeccionRepository;
import com.example.mpct.repository.TramiteRepository;
import com.example.mpct.repository.UserRepository;
import com.example.mpct.service.InspeccionSchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspeccionSchedulingServiceImpl implements InspeccionSchedulingService {

    private final InspeccionRepository inspeccionRepository;
    private final UserRepository userRepository;
    private final TramiteRepository tramiteRepository;

    @Value("${app.inspecciones.max-por-dia-inspector:5}")
    private int maxInspeccionesPorDia;

    @Override
    @Transactional
    public Inspeccion programarInspeccion(Tramite tramite, int numeroVisita, int diasMinimosHabiles) {
        List<User> inspectores = userRepository.findByRole(Role.INSPECTOR);
        if (inspectores.isEmpty()) {
            // Si no hay inspectores, programamos sin asignar a nadie para no bloquear el flujo
            return programarSinInspector(tramite, numeroVisita, diasMinimosHabiles);
        }

        LocalDateTime fechaPropuesta = sumarDiasHabiles(LocalDateTime.now(), diasMinimosHabiles);
        
        // Buscar el primer slot disponible
        // Máximo buscamos hasta 30 días adelante para evitar bucles infinitos
        for (int i = 0; i < 30; i++) {
            if (fechaPropuesta.getDayOfWeek() == DayOfWeek.SATURDAY || fechaPropuesta.getDayOfWeek() == DayOfWeek.SUNDAY) {
                fechaPropuesta = fechaPropuesta.plusDays(1);
                continue;
            }

            for (User inspector : inspectores) {
                long count = inspeccionRepository.countByInspectorIdAndFechaProgramada(inspector.getId(), fechaPropuesta);
                if (count < maxInspeccionesPorDia) {
                    // Encontrado slot
                    return asignarSlot(tramite, inspector, fechaPropuesta, numeroVisita);
                }
            }
            fechaPropuesta = fechaPropuesta.plusDays(1);
        }

        // Fallback: programar sin inspector si todos están llenos el próximo mes
        return programarSinInspector(tramite, numeroVisita, diasMinimosHabiles);
    }

    private Inspeccion asignarSlot(Tramite tramite, User inspector, LocalDateTime fecha, int numeroVisita) {
        Inspeccion inspeccion = Inspeccion.builder()
                .tramite(tramite)
                .inspector(inspector)
                .numeroInspeccion(numeroVisita)
                .estado(EstadoInspeccion.PROGRAMADA)
                .fechaProgramada(fecha)
                .build();
        inspeccionRepository.save(inspeccion);

        tramite.setEstado(EstadoTramite.PROGRAMADO);
        tramiteRepository.save(tramite);

        return inspeccion;
    }

    private Inspeccion programarSinInspector(Tramite tramite, int numeroVisita, int diasMinimos) {
        LocalDateTime fechaProgramada = sumarDiasHabiles(LocalDateTime.now(), diasMinimos);
        Inspeccion inspeccion = Inspeccion.builder()
                .tramite(tramite)
                .numeroInspeccion(numeroVisita)
                .estado(EstadoInspeccion.PROGRAMADA)
                .fechaProgramada(fechaProgramada)
                .build();
        inspeccionRepository.save(inspeccion);

        tramite.setEstado(EstadoTramite.PROGRAMADO);
        tramiteRepository.save(tramite);
        return inspeccion;
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
}
