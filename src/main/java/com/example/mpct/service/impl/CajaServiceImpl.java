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
    private final com.example.mpct.service.ComprobanteService comprobanteService;
    private final com.example.mpct.service.TramiteService tramiteService;
    private final com.example.mpct.repository.PagoDetalleRepository pagoDetalleRepository;

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
    public com.example.mpct.dto.caja.PagoPresencialResponse registrarPagoPresencial(String email, com.example.mpct.dto.caja.PagoPresencialRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        Caja caja = cajaRepository.findByUsuarioIdAndEstado(user.getId(), EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RuntimeException("Debe abrir la caja antes de registrar pagos"));

        Tramite tramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(request.ruc())
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (tramite.getEstado() != EstadoTramite.PENDIENTE_PAGO) {
            throw new RuntimeException("El trámite no está en estado PENDIENTE_PAGO");
        }

        List<com.example.mpct.dto.caja.PagoPresencialRequest.PagoDetalleDTO> dtoList = request.detalles();
        if (dtoList == null || dtoList.isEmpty()) {
            throw new RuntimeException("Debe especificar al menos un método de pago.");
        }
        
        int size = dtoList.size();

        if (size >= 3) {
            throw new RuntimeException("Número de métodos de pago inválido. Se permiten 1 o 2 líneas.");
        }

        if (size == 2) {
            long countEfectivo = dtoList.stream().filter(d -> com.example.mpct.model.enums.MetodoPago.EFECTIVO.equals(d.metodo())).count();
            long countYape = dtoList.stream().filter(d -> com.example.mpct.model.enums.MetodoPago.YAPE.equals(d.metodo())).count();
            long countTarjeta = dtoList.stream().filter(d -> com.example.mpct.model.enums.MetodoPago.TARJETA.equals(d.metodo())).count();

            if (countEfectivo != 1 || (countYape + countTarjeta) != 1) {
                throw new RuntimeException("Solo se permite dividir el pago entre efectivo y Yape, o entre efectivo y tarjeta.");
            }
        }

        BigDecimal totalMonto = dtoList.stream().map(com.example.mpct.dto.caja.PagoPresencialRequest.PagoDetalleDTO::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalMonto.compareTo(tramite.getMontoCobrado()) != 0) {
            throw new RuntimeException("El monto total pagado (" + totalMonto + ") no coincide con el total del trámite (" + tramite.getMontoCobrado() + ").");
        }

        // Crear Pago en la BD
        Pago pago = pagoRepository.findByTramiteId(tramite.getId()).orElse(Pago.builder()
                .tramite(tramite)
                .monto(tramite.getMontoCobrado())
                .build());
                
        if (pago.getDetalles() == null) {
            pago.setDetalles(new java.util.ArrayList<>());
        }

        pago.setMetodoPago("CAJA_PRESENCIAL");
        pago.setEstadoPago("COMPLETADO");
        pago.setNumeroComprobante("CAJA-" + System.currentTimeMillis()); 

        BigDecimal vueltoTotal = BigDecimal.ZERO;

        for (com.example.mpct.dto.caja.PagoPresencialRequest.PagoDetalleDTO d : dtoList) {
            if (!com.example.mpct.model.enums.MetodoPago.EFECTIVO.equals(d.metodo())) {
                if (d.referencia() == null || d.referencia().trim().isEmpty()) {
                    throw new RuntimeException("La referencia es obligatoria para " + d.metodo());
                }
                if (d.montoEntregado() != null) {
                    throw new RuntimeException("El monto entregado debe ser nulo para métodos distintos de efectivo.");
                }
                if (pagoDetalleRepository.existsByMetodoAndReferencia(d.metodo(), d.referencia())) {
                    throw new RuntimeException("La referencia " + d.referencia() + " ya ha sido registrada previamente para " + d.metodo());
                }
            } else {
                if (d.montoEntregado() == null || d.montoEntregado().compareTo(d.monto()) < 0) {
                    throw new RuntimeException("Monto entregado insuficiente para la porción en efectivo.");
                }
                vueltoTotal = vueltoTotal.add(d.montoEntregado().subtract(d.monto()));
            }

            PagoDetalle detalle = PagoDetalle.builder()
                .pago(pago)
                .metodo(d.metodo())
                .monto(d.monto())
                .montoEntregado(d.montoEntregado())
                .referencia(d.referencia())
                .build();
            pago.getDetalles().add(detalle);

            MovimientoCaja movimiento = MovimientoCaja.builder()
                    .caja(caja)
                    .tipo(TipoMovimiento.INGRESO)
                    .monto(d.monto()) 
                    .metodoPago(d.metodo().name())
                    .descripcion("Pago trámite " + tramite.getTipo().name() + " (" + d.metodo() + ") - RUC: " + tramite.getRuc())
                    .tramiteId(tramite.getId())
                    .build();
            movimientoCajaRepository.save(movimiento);
        }

        pago = pagoRepository.save(pago);

        // Generar Comprobante (Factura/Boleta)
        comprobanteService.generarYGuardar(pago);

        // Actualizar Tramite
        if (tramite.getRequiereInspeccion()) {
            tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.PENDIENTE_REVISION, null);
        } else {
            tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.PROGRAMADO, null);
            inspeccionSchedulingService.programarInspeccion(tramite, 1, 3);
        }

        return new com.example.mpct.dto.caja.PagoPresencialResponse(mapToResponse(tramite), vueltoTotal, "Pago registrado exitosamente");
    }

    private TramiteResponse mapToResponse(Tramite t) {
        String certUrl = null;
        if (t.getEstado() == EstadoTramite.APROBADO) {
            certUrl = "/api/v1/tramites/" + t.getRuc() + "/certificado";
        }
        
        boolean pagoRechazado = pagoRepository.findByTramiteId(t.getId())
                .map(p -> "RECHAZADO".equals(p.getEstadoPago()))
                .orElse(false);

        com.example.mpct.model.enums.EstadoLicencia estadoLic = null;
        java.time.LocalDateTime fechaVencimiento = null;
        if (t.getEstado() == EstadoTramite.APROBADO) {
            java.util.Optional<com.example.mpct.model.entity.Licencia> licOpt = licenciaRepository.findByTramiteId(t.getId());
            if (licOpt.isPresent()) {
                estadoLic = licOpt.get().getEstado();
                fechaVencimiento = licOpt.get().getFechaVencimiento();
            }
        }

        return new TramiteResponse(
                t.getId(), t.getRuc(), t.getRazonSocial(), t.getDomicilioFiscal(), t.getRepresentanteLegal(), t.getRubro(),
                t.getDni(), t.getEmail(), t.getArea(),
                t.getTipo(), t.getEstado(), t.getMontoCobrado(),
                "/api/v1/tramites/" + t.getRuc() + "/archivos/plano",
                certUrl,
                t.getObservacionesGenerales(),
                t.getArchivosObservados(),
                t.getCreatedAt(), t.getUpdatedAt(),
                pagoRechazado,
                estadoLic,
                fechaVencimiento
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

        String email = licencia.getTramite().getEmail();
        if (email == null || email.trim().isEmpty()) {
            System.err.println("Trámite " + licencia.getTramite().getId() + " sin email registrado, no se pudo notificar vencimiento.");
            return;
        }
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

    @Override
    @Transactional
    public void forzarCierreCaja(java.util.UUID cajaId, com.example.mpct.dto.caja.ForzarCierreRequest request, String adminEmail) {
        Caja caja = cajaRepository.findById(cajaId)
                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));
        
        if (caja.getEstado() != EstadoCaja.ABIERTA) {
            throw new RuntimeException("La caja ya está cerrada");
        }

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
        caja.setMontoDeclarado(request.montoFisico() != null ? request.montoFisico() : montoFinal);
        caja.setFechaCierre(LocalDateTime.now());
        caja.setEstado(EstadoCaja.CERRADA);
        caja.setMotivoCierreForzado(request.motivo());
        caja.setCerradoPorAdmin(adminEmail);
        
        cajaRepository.save(caja);
    }

    @Override
    @Transactional
    public String validarPagoCajero(java.util.UUID pagoId, boolean aprobado, String cajeroEmail) {
        User user = userRepository.findByEmail(cajeroEmail).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Caja caja = cajaRepository.findByUsuarioIdAndEstado(user.getId(), EstadoCaja.ABIERTA)
                .orElseThrow(() -> new RuntimeException("Debe abrir caja antes de validar pagos"));

        Pago pago = pagoRepository.findById(pagoId).orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        Tramite tramite = pago.getTramite();

        if (aprobado) {
            pago.setEstadoPago("COMPLETADO");
            
            pago = pagoRepository.save(pago);

            if (tramite.getRequiereInspeccion()) {
                tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.PENDIENTE_REVISION, null);
            } else {
                tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.PAGADO, null);
                inspeccionService.programarInspeccionInicial(tramite);
            }
            
            // Generar Comprobante
            comprobanteService.generarYGuardar(pago);

            // Generar Movimiento de Caja por la validación
            MovimientoCaja movimiento = MovimientoCaja.builder()
                    .caja(caja)
                    .tipo(TipoMovimiento.INGRESO)
                    .monto(pago.getMonto())
                    .metodoPago(pago.getMetodoPago())
                    .descripcion("Validación de pago trámite " + tramite.getTipo().name() + " - RUC: " + tramite.getRuc())
                    .tramiteId(tramite.getId())
                    .build();
            movimientoCajaRepository.save(movimiento);

            return "Pago validado y aprobado exitosamente.";
        } else {
            pago.setEstadoPago("RECHAZADO");
            pagoRepository.save(pago);
            tramiteService.actualizarEstadoTramite(tramite, EstadoTramite.PENDIENTE_PAGO, null);
            return "Pago rechazado. Trámite devuelto a pendiente de pago.";
        }
    }
}
