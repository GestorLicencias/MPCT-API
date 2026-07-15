package com.example.mpct.service.impl;

import com.example.mpct.dto.caja.AbrirCajaRequest;
import com.example.mpct.dto.caja.CajaEstadoResponse;
import com.example.mpct.dto.caja.PagoPresencialRequest;
import com.example.mpct.dto.tramite.TramiteResponse;
import com.example.mpct.model.entity.*;
import com.example.mpct.model.enums.EstadoCaja;
import com.example.mpct.model.enums.EstadoTramite;
import com.example.mpct.model.enums.TipoMovimiento;
import com.example.mpct.repository.*;
import com.example.mpct.service.CajaService;
import com.example.mpct.service.InspeccionService;
import com.example.mpct.service.InspeccionSchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CajaServiceImpl implements CajaService {

    private final CajaRepository cajaRepository;
    private final MovimientoCajaRepository movimientoCajaRepository;
    private final UserRepository userRepository;
    private final TramiteRepository tramiteRepository;
    private final PagoRepository pagoRepository;
    private final InspeccionService inspeccionService;
    private final InspeccionSchedulingService inspeccionSchedulingService;
    private final LicenciaRepository licenciaRepository;
    private final com.example.mpct.service.NotificacionService notificacionService;

    @Override
    @Transactional
    public void abrirCaja(String email, AbrirCajaRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (cajaRepository.findByUsuarioIdAndEstado(user.getId(), EstadoCaja.ABIERTA).isPresent()) {
            throw new RuntimeException("El usuario ya tiene una caja abierta");
        }

        Caja caja = Caja.builder()
                .usuario(user)
                .fechaApertura(LocalDateTime.now())
                .montoInicial(request.montoInicial())
                .estado(EstadoCaja.ABIERTA)
                .build();
        
        cajaRepository.save(caja);
    }

    @Override
    @Transactional
    public void cerrarCaja(String email, BigDecimal montoFisico) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Caja caja = cajaRepository.findByUsuarioIdAndEstado(user.getId(), EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RuntimeException("No hay caja abierta para este usuario"));

        List<MovimientoCaja> movimientos = movimientoCajaRepository.findByCajaIdOrderByCreatedAtAsc(caja.getId());
        
        BigDecimal ingresos = movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimiento.INGRESO)
                .map(MovimientoCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal egresos = movimientos.stream()
                .filter(m -> m.getTipo() == TipoMovimiento.EGRESO)
                .map(MovimientoCaja::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoFinal = caja.getMontoInicial().add(ingresos).subtract(egresos);

        caja.setMontoFinal(montoFinal);
        caja.setMontoDeclarado(montoFisico != null ? montoFisico : montoFinal);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado(EstadoCaja.CERRADA);
        
        cajaRepository.save(caja);
    }

    @Override
    @Transactional(readOnly = true)
    public CajaEstadoResponse obtenerEstadoCaja(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        return cajaRepository.findByUsuarioIdAndEstado(user.getId(), EstadoCaja.ABIERTA)
                .map(caja -> {
                    List<MovimientoCaja> movimientos = movimientoCajaRepository.findByCajaIdOrderByCreatedAtAsc(caja.getId());
                    BigDecimal ingresos = movimientos.stream()
                            .filter(m -> m.getTipo() == TipoMovimiento.INGRESO)
                            .map(MovimientoCaja::getMonto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal egresos = movimientos.stream()
                            .filter(m -> m.getTipo() == TipoMovimiento.EGRESO)
                            .map(MovimientoCaja::getMonto)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal montoActual = caja.getMontoInicial().add(ingresos).subtract(egresos);
                    
                    return new CajaEstadoResponse(caja.getId(), true, caja.getMontoInicial(), ingresos, egresos, montoActual);
                })
                .orElse(new CajaEstadoResponse(null, false, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Override
    @Transactional
    public TramiteResponse registrarPagoPresencial(String email, PagoPresencialRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Caja caja = cajaRepository.findByUsuarioIdAndEstado(user.getId(), EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RuntimeException("Debe abrir la caja antes de registrar pagos"));

        Tramite tramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(request.ruc())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (tramite.getEstado() != EstadoTramite.PENDIENTE_PAGO) {
            throw new RuntimeException("El trámite no está en estado PENDIENTE_PAGO");
        }

        // Crear Pago en la BD
        Pago pago = pagoRepository.findByTramiteId(tramite.getId()).orElse(Pago.builder()
                .tramite(tramite)
                .monto(tramite.getMontoCobrado())
                .build());

        pago.setMetodoPago("CAJA_PRESENCIAL");
        pago.setEstadoPago("COMPLETADO");
        pago.setNumeroComprobante("CAJA-" + System.currentTimeMillis()); // Generate a voucher number for Caja
        pagoRepository.save(pago);

        // Crear Movimiento de Caja
        MovimientoCaja movimiento = MovimientoCaja.builder()
                .caja(caja)
                .tipo(TipoMovimiento.INGRESO)
                .monto(tramite.getMontoCobrado())
                .metodoPago(request.metodoPago()) // EFECTIVO o TARJETA
                .descripcion("Pago trámite " + tramite.getTipo().name() + " - RUC: " + tramite.getRuc())
                .tramiteId(tramite.getId())
                .build();
        movimientoCajaRepository.save(movimiento);

        // Actualizar Tramite
        if (tramite.getRequiereInspeccion()) {
            tramite.setEstado(EstadoTramite.PENDIENTE_REVISION);
        } else {
            tramite.setEstado(EstadoTramite.PROGRAMADO);
            inspeccionSchedulingService.programarInspeccion(tramite, 1, 3);
        }
        tramiteRepository.save(tramite);

        return mapToResponse(tramite);
    }

    private TramiteResponse mapToResponse(Tramite t) {
        String certUrl = null;
        if (t.getEstado() == EstadoTramite.APROBADO) {
            certUrl = "/api/v1/tramites/" + t.getRuc() + "/certificado";
        }
        
        boolean pagoRechazado = pagoRepository.findByTramiteId(t.getId())
                .map(p -> "RECHAZADO".equals(p.getEstadoPago()))
                .orElse(false);

        return new TramiteResponse(
                t.getId(), t.getRuc(), t.getRazonSocial(), t.getDomicilioFiscal(), t.getRepresentanteLegal(), t.getRubro(),
                t.getDni(), t.getEmail(), t.getArea(),
                t.getTipo(), t.getEstado(), t.getMontoCobrado(),
                "/api/v1/tramites/" + t.getRuc() + "/archivos/plano",
                "/api/v1/tramites/" + t.getRuc() + "/archivos/foto",
                t.getArchivoFoto2() != null ? "/api/v1/tramites/" + t.getRuc() + "/archivos/foto2" : null,
                t.getArchivoFoto3() != null ? "/api/v1/tramites/" + t.getRuc() + "/archivos/foto3" : null,
                t.getArchivoFoto4() != null ? "/api/v1/tramites/" + t.getRuc() + "/archivos/foto4" : null,
                certUrl,
                t.getObservacionesGenerales(),
                t.getArchivosObservados(),
                t.getCreatedAt(), t.getUpdatedAt(),
                pagoRechazado
        );
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> obtenerAlertasLicencias() {
        List<java.util.Map<String, Object>> alertas = new java.util.ArrayList<>();
        
        List<Licencia> vencidas = licenciaRepository.findByEstado(com.example.mpct.model.enums.EstadoLicencia.VENCIDA);
        for(Licencia l : vencidas) {
             alertas.add(java.util.Map.of(
                 "ruc", l.getTramite().getRuc(),
                 "razonSocial", l.getTramite().getRazonSocial(),
                 "numeroLicencia", l.getNumeroLicencia(),
                 "fechaVencimiento", l.getFechaVencimiento().toString(),
                 "estado", "VENCIDA"
             ));
        }
        
        java.time.LocalDateTime limite = java.time.LocalDateTime.now().plusDays(30);
        List<Licencia> vigentes = licenciaRepository.findByEstado(com.example.mpct.model.enums.EstadoLicencia.VIGENTE);
        for(Licencia l : vigentes) {
             if (l.getFechaVencimiento().isBefore(limite)) {
                 alertas.add(java.util.Map.of(
                     "ruc", l.getTramite().getRuc(),
                     "razonSocial", l.getTramite().getRazonSocial(),
                     "numeroLicencia", l.getNumeroLicencia(),
                     "fechaVencimiento", l.getFechaVencimiento().toString(),
                     "estado", "POR_VENCER"
                 ));
             }
        }
        
        return alertas;
    }

    @Override
    @Transactional
    public void enviarRecordatorioLicencia(String ruc) {
        Licencia licencia = licenciaRepository.findByTramiteRuc(ruc)
                .orElseThrow(() -> new RuntimeException("Licencia no encontrada para el RUC proporcionado"));

        String email = licencia.getTramite().getEmail() != null ? licencia.getTramite().getEmail() : ruc + "@tramite.com";
        String asunto = "URGENTE: Recordatorio de Licencia de Funcionamiento";
        String estadoMsg = licencia.getEstado() == com.example.mpct.model.enums.EstadoLicencia.VENCIDA ? "ha VENCIDO" : "está PRÓXIMA A VENCER";
        
        String mensaje = "Estimado contribuyente (" + licencia.getTramite().getRazonSocial() + "),\n\n"
                + "La Municipalidad Provincial de Trujillo le recuerda que su Licencia de Funcionamiento Nro " 
                + licencia.getNumeroLicencia() + " " + estadoMsg + ".\n\n"
                + "Le solicitamos acercarse a las oficinas o iniciar el trámite de renovación en nuestra plataforma web a la brevedad "
                + "para evitar sanciones o la clausura del local.\n\n"
                + "Atentamente,\nÁrea de Licencias.";

        notificacionService.enviarEmail(email, asunto, mensaje, licencia.getTramite().getId());
    }
}
