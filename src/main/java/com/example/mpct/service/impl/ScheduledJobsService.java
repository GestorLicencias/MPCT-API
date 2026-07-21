package com.example.mpct.service.impl;

import com.example.mpct.model.entity.Inspeccion;
import com.example.mpct.model.entity.Licencia;
import com.example.mpct.model.enums.EstadoInspeccion;
import com.example.mpct.model.enums.EstadoLicencia;
import com.example.mpct.repository.InspeccionRepository;
import com.example.mpct.repository.LicenciaRepository;
import com.example.mpct.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduledJobsService {

    private final InspeccionRepository inspeccionRepository;
    private final LicenciaRepository licenciaRepository;
    private final com.example.mpct.repository.TramiteRepository tramiteRepository;
    private final com.example.mpct.service.TramiteService tramiteService;
    private final NotificacionService notificacionService;

    // Ejecuta todos los días a las 08:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void notificarInspeccionesManana() {
        LocalDate manana = LocalDate.now().plusDays(1);
        
        List<Inspeccion> inspecciones = inspeccionRepository.findByEstado(EstadoInspeccion.PROGRAMADA);
        
        for (Inspeccion insp : inspecciones) {
            if (insp.getFechaProgramada() != null && insp.getFechaProgramada().toLocalDate().isEqual(manana)) {
                String ruc = insp.getTramite().getRuc();
                String negocioEmail = insp.getTramite().getEmail();
                String inspectorEmail = insp.getInspector() != null ? insp.getInspector().getEmail() : "admin@mpct.gob.pe";
                
                String asunto = "Recordatorio de Inspección Programada - " + ruc;
                String mensaje = "Se le recuerda que tiene una inspección programada para mañana a las " 
                        + insp.getFechaProgramada().toLocalTime() + ".\n"
                        + "Trámite RUC: " + ruc + "\n"
                        + "Dirección: " + insp.getTramite().getDomicilioFiscal();

                if (negocioEmail != null && !negocioEmail.trim().isEmpty()) {
                    notificacionService.enviarEmail(negocioEmail, asunto, mensaje, insp.getTramite().getId());
                } else {
                    System.err.println("Trámite " + insp.getTramite().getId() + " sin email registrado, no se pudo notificar al negocio.");
                }
                
                if (insp.getInspector() != null) {
                    notificacionService.enviarEmail(inspectorEmail, asunto, mensaje, insp.getTramite().getId());
                }
            }
        }
    }

    // Ejecuta todos los días a la 01:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void vencerLicenciasExpiradas() {
        LocalDate hoy = LocalDate.now();
        List<Licencia> licencias = licenciaRepository.findAll();
        
        for (Licencia lic : licencias) {
            if (lic.getEstado() == EstadoLicencia.VIGENTE && lic.getFechaVencimiento().toLocalDate().isBefore(hoy.plusDays(1))) {
                lic.setEstado(EstadoLicencia.VENCIDA);
                licenciaRepository.save(lic);
                
                String ruc = lic.getTramite().getRuc();
                String negocioEmail = lic.getTramite().getEmail();
                String asunto = "Su Licencia de Funcionamiento ha Vencido - " + ruc;
                String mensaje = "Le informamos que su licencia de funcionamiento Nro " + lic.getNumeroLicencia() 
                        + " ha vencido el día de hoy. Por favor inicie el trámite de renovación.";
                
                if (negocioEmail != null && !negocioEmail.trim().isEmpty()) {
                    notificacionService.enviarEmail(negocioEmail, asunto, mensaje, lic.getTramite().getId());
                } else {
                    System.err.println("Trámite " + lic.getTramite().getId() + " sin email registrado, no se pudo notificar vencimiento.");
                }
            }
        }
    }

    // Ejecuta todos los días a las 07:00 AM
    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional
    public void notificarLicenciasPorVencer() {
        LocalDate limite = LocalDate.now().plusDays(30);
        List<Licencia> licencias = licenciaRepository.findAll();
        
        for (Licencia lic : licencias) {
            if (lic.getEstado() == EstadoLicencia.VIGENTE && lic.getFechaVencimiento().toLocalDate().isEqual(limite)) {
                String ruc = lic.getTramite().getRuc();
                String negocioEmail = lic.getTramite().getEmail();
                String asunto = "Aviso: Su Licencia de Funcionamiento está por vencer - " + ruc;
                String mensaje = "Le informamos que su licencia de funcionamiento Nro " + lic.getNumeroLicencia() 
                        + " vencerá en 30 días. Le sugerimos iniciar el trámite de renovación pronto.";
                if (negocioEmail != null && !negocioEmail.trim().isEmpty()) {
                    notificacionService.enviarEmail(negocioEmail, asunto, mensaje, lic.getTramite().getId());
                } else {
                    System.err.println("Trámite " + lic.getTramite().getId() + " sin email registrado, no se pudo notificar por vencer.");
                }
            }
        }
    }

    // Ejecuta todos los días a las 02:00 AM
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void vencerTramitesObservados() {
        java.time.LocalDateTime hoy = java.time.LocalDateTime.now();
        List<com.example.mpct.model.entity.Tramite> tramites = tramiteRepository.findByEstado(com.example.mpct.model.enums.EstadoTramite.OBSERVADO);
        
        for (com.example.mpct.model.entity.Tramite tramite : tramites) {
            if (tramite.getFechaLimiteSubsanacion() != null && tramite.getFechaLimiteSubsanacion().isBefore(hoy)) {
                
                boolean tieneInspeccionProgramada = tramite.getInspecciones().stream()
                        .anyMatch(i -> i.getEstado() == EstadoInspeccion.PROGRAMADA);

                if (!tieneInspeccionProgramada) {
                    tramiteService.actualizarEstadoTramite(tramite, com.example.mpct.model.enums.EstadoTramite.ABANDONADO, null);
                }
            }
        }
    }
}
