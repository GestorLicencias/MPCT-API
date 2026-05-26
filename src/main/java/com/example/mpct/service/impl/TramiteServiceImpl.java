package com.example.mpct.service.impl;

import com.example.mpct.service.*;
import com.example.mpct.dto.sunat.SunatRucResponse;
import com.example.mpct.dto.tramite.TramiteResponse;
import com.example.mpct.model.entity.*;
import com.example.mpct.model.enums.*;
import com.example.mpct.repository.PagoRepository;
import com.example.mpct.repository.TramiteRepository;
import com.example.mpct.repository.LicenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TramiteServiceImpl implements TramiteService {

    private final TramiteRepository tramiteRepository;
    private final SunatScrapingService sunatScrapingService;
    private final PagoRepository pagoRepository;
    private final InspeccionService inspeccionService;
    private final LicenciaRepository licenciaRepository;
    private final LicenciaService licenciaService;
    private final com.example.mpct.repository.ConfiguracionRepository configuracionRepository;

    @Transactional
    public TramiteResponse crearTramite(String ruc, String representanteLegal, String rubro, String dni, BigDecimal area, TipoTramite tipo, MultipartFile plano, java.util.List<MultipartFile> fotos) {
        
        // --- Validación por tipo de trámite (NUEVO vs RENOVACION) ---
        java.util.Optional<Licencia> licenciaPreviaOpt = licenciaRepository.findByTramiteRuc(ruc);
        
        if (tipo == TipoTramite.RENOVACION || tipo == TipoTramite.MODIFICACION || tipo == TipoTramite.TRASLADO) {
            // Para estos trámites debe existir una licencia anterior
            if (licenciaPreviaOpt.isEmpty()) {
                throw new RuntimeException("No se encontró una licencia activa para este RUC. Si es la primera vez, seleccione el tipo 'NUEVO'.");
            }
            Licencia licenciaPrevia = licenciaPreviaOpt.get();
            
            if (tipo == TipoTramite.RENOVACION) {
                if (licenciaPrevia.getFechaVencimiento().isAfter(LocalDateTime.now().plusDays(30))) {
                    throw new RuntimeException("La licencia actual aún está vigente por más de 30 días. Debe esperar a que falte menos de 1 mes para renovarla.");
                }
                // Licencia vencida o próxima a vencer encontrada → limpiar trámite y licencia anteriores para crear el nuevo
                Tramite tramiteAnterior = licenciaPrevia.getTramite();
                licenciaRepository.delete(licenciaPrevia);
                tramiteRepository.delete(tramiteAnterior);
            } else {
                // MODIFICACION o TRASLADO
                java.util.Optional<Tramite> existingTramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc);
                if (existingTramite.isPresent()) {
                    Tramite t = existingTramite.get();
                    // Evitar que haya un trámite en curso
                    if (t.getEstado() != EstadoTramite.APROBADO && t.getEstado() != EstadoTramite.DENEGADO && !t.getId().equals(licenciaPrevia.getTramite().getId())) {
                        throw new RuntimeException("Ya existe un trámite en curso para este RUC (Estado: " + t.getEstado() + ").");
                    }
                }
            }
        } else {
            // Para NUEVO: NO debe existir ninguna licencia previa
            if (licenciaPreviaOpt.isPresent()) {
                Licencia licenciaPrevia = licenciaPreviaOpt.get();
                throw new RuntimeException("El RUC ya cuenta con una licencia. Debe seleccionar otro tipo de trámite.");
            }
            // Para NUEVO sin licencia previa: verificar que no haya un trámite en curso
            java.util.Optional<Tramite> existingTramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc);
            if (existingTramite.isPresent()) {
                Tramite t = existingTramite.get();
                if (t.getEstado() == EstadoTramite.DENEGADO) {
                    tramiteRepository.delete(t);
                } else {
                    throw new RuntimeException("Ya existe un trámite en curso para este RUC (Estado: " + t.getEstado() + ").");
                }
            }
        }

        SunatRucResponse sunatData = sunatScrapingService.validarRuc(ruc);

        byte[] planoBytes = null;
        byte[] fotoBytes = null;
        byte[] foto2Bytes = null;
        byte[] foto3Bytes = null;
        byte[] foto4Bytes = null;

        String finalDomicilio = sunatData.domicilioFiscal();
        BigDecimal finalArea = area;

        if (tipo == TipoTramite.RENOVACION) {
            Tramite anterior = licenciaPreviaOpt.get().getTramite();
            planoBytes = anterior.getArchivoPlano();
            fotoBytes = anterior.getArchivoFoto();
            foto2Bytes = anterior.getArchivoFoto2();
            foto3Bytes = anterior.getArchivoFoto3();
            foto4Bytes = anterior.getArchivoFoto4();
            finalArea = anterior.getArea();
            finalDomicilio = anterior.getDomicilioFiscal();
        } else {
            try {
                if (plano != null) planoBytes = plano.getBytes();
                if (fotos != null && !fotos.isEmpty()) {
                    fotoBytes = fotos.get(0).getBytes();
                    if (fotos.size() > 1) foto2Bytes = fotos.get(1).getBytes();
                    if (fotos.size() > 2) foto3Bytes = fotos.get(2).getBytes();
                    if (fotos.size() > 3) foto4Bytes = fotos.get(3).getBytes();
                }
            } catch (java.io.IOException e) {
                throw new RuntimeException("Error al leer los archivos: " + e.getMessage());
            }
        }

        BigDecimal precio = configuracionRepository.findByClave("PRECIO_LICENCIA")
                .map(com.example.mpct.model.entity.Configuracion::getValor)
                .orElse(new BigDecimal("180.00"));

        Tramite tramite = Tramite.builder()
                .ruc(ruc)
                .razonSocial(sunatData.razonSocial())
                .domicilioFiscal(finalDomicilio)
                .representanteLegal(representanteLegal)
                .dni(dni)
                .area(finalArea)
                .rubro(rubro)
                .tipo(tipo)
                .estado(EstadoTramite.PENDIENTE_PAGO)
                .montoCobrado(precio)
                .archivoPlano(planoBytes)
                .archivoFoto(fotoBytes)
                .archivoFoto2(foto2Bytes)
                .archivoFoto3(foto3Bytes)
                .archivoFoto4(foto4Bytes)
                .requiereInspeccion(tipo == TipoTramite.MODIFICACION || tipo == TipoTramite.TRASLADO)
                .build();

        tramite = tramiteRepository.save(tramite);
        return mapToResponse(tramite);
    }

    @Transactional(readOnly = true)
    public TramiteResponse obtenerTramitePorRuc(String ruc) {
        Tramite tramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc)
                .orElseThrow(() -> new RuntimeException("No se encontró trámite para el RUC: " + ruc));
        return mapToResponse(tramite);
    }

    @Override
    @Transactional
    public TramiteResponse actualizarArchivos(String ruc, MultipartFile plano, MultipartFile foto, MultipartFile foto2, MultipartFile foto3, MultipartFile foto4) {
        Tramite tramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (tramite.getEstado() != EstadoTramite.OBSERVADO) {
            throw new RuntimeException("Solo se pueden actualizar archivos de trámites observados");
        }

        try {
            if (plano != null && !plano.isEmpty()) {
                tramite.setArchivoPlano(plano.getBytes());
            }
            if (foto != null && !foto.isEmpty()) {
                tramite.setArchivoFoto(foto.getBytes());
            }
            if (foto2 != null && !foto2.isEmpty()) {
                tramite.setArchivoFoto2(foto2.getBytes());
            }
            if (foto3 != null && !foto3.isEmpty()) {
                tramite.setArchivoFoto3(foto3.getBytes());
            }
            if (foto4 != null && !foto4.isEmpty()) {
                tramite.setArchivoFoto4(foto4.getBytes());
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error procesando archivos: " + e.getMessage());
        }

        tramite.setEstado(EstadoTramite.SUBSANADO);
        tramite.setObservacionesGenerales(null);
        tramite.setArchivosObservados(null);

        tramiteRepository.save(tramite);
        return mapToResponse(tramite);
    }

    @Transactional
    public TramiteResponse pagarTramite(String ruc, String metodoPago, MultipartFile voucher, String transactionId) {
        Tramite tramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (tramite.getEstado() != EstadoTramite.PENDIENTE_PAGO) {
            throw new RuntimeException("El trámite no está en estado PENDIENTE_PAGO");
        }

        Pago pago = Pago.builder()
                .tramite(tramite)
                .monto(tramite.getMontoCobrado())
                .metodoPago(metodoPago)
                .build();

        if ("MERCADO_PAGO".equals(metodoPago)) {
            pago.setPasarelaTransactionId(transactionId);
            pago.setEstadoPago("COMPLETADO");
            pagoRepository.save(pago);
            if (tramite.getRequiereInspeccion()) {
                tramite.setEstado(EstadoTramite.PENDIENTE_REVISION);
            } else {
                tramite.setEstado(EstadoTramite.PAGADO);
                inspeccionService.programarInspeccionInicial(tramite);
            }
            tramiteRepository.save(tramite);
        } else if ("BANCO_NACION".equals(metodoPago) && voucher != null) {
            pago.setEstadoPago("PENDIENTE");
            try {
                pago.setArchivoVoucher(voucher.getBytes());
            } catch (java.io.IOException e) {
                throw new RuntimeException("Error guardando el voucher");
            }
            pagoRepository.save(pago);
            if (tramite.getRequiereInspeccion()) {
                tramite.setEstado(EstadoTramite.PENDIENTE_REVISION);
            } else {
                tramite.setEstado(EstadoTramite.VALIDANDO_PAGO);
            }
            tramiteRepository.save(tramite);
        } else {
            throw new RuntimeException("Método de pago inválido o falta voucher");
        }

        return mapToResponse(tramite);
    }

    @Transactional
    public TramiteResponse aprobarTramiteRevision(String ruc) {
        Tramite tramite = tramiteRepository.findTopByRucOrderByCreatedAtDesc(ruc)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (tramite.getEstado() != EstadoTramite.PENDIENTE_REVISION) {
            throw new RuntimeException("El trámite no está en estado PENDIENTE_REVISION");
        }

        tramite.setEstado(EstadoTramite.APROBADO);
        tramiteRepository.save(tramite);
        
        // Cambiar la licencia anterior a HISTORICA (si existe)
        java.util.Optional<Licencia> licenciaPreviaOpt = licenciaRepository.findByTramiteRuc(ruc);
        if (licenciaPreviaOpt.isPresent()) {
            Licencia licenciaPrevia = licenciaPreviaOpt.get();
            licenciaPrevia.setEstado(com.example.mpct.model.enums.EstadoLicencia.HISTORICA);
            licenciaRepository.save(licenciaPrevia);
        }

        // Generar la nueva licencia
        licenciaService.generarLicencia(tramite);
        
        return mapToResponse(tramite);
    }

    private TramiteResponse mapToResponse(Tramite t) {
        String certUrl = null;
        if (t.getEstado() == EstadoTramite.APROBADO) {
            certUrl = "/api/v1/tramites/" + t.getRuc() + "/certificado";
        }
        return new TramiteResponse(
                t.getId(), t.getRuc(), t.getRazonSocial(), t.getDomicilioFiscal(), t.getRepresentanteLegal(), t.getRubro(),
                t.getDni(), t.getArea(),
                t.getTipo(), t.getEstado(), t.getMontoCobrado(),
                "/api/v1/tramites/" + t.getRuc() + "/archivos/plano",
                "/api/v1/tramites/" + t.getRuc() + "/archivos/foto",
                t.getArchivoFoto2() != null ? "/api/v1/tramites/" + t.getRuc() + "/archivos/foto2" : null,
                t.getArchivoFoto3() != null ? "/api/v1/tramites/" + t.getRuc() + "/archivos/foto3" : null,
                t.getArchivoFoto4() != null ? "/api/v1/tramites/" + t.getRuc() + "/archivos/foto4" : null,
                certUrl,
                t.getObservacionesGenerales(),
                t.getArchivosObservados(),
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
