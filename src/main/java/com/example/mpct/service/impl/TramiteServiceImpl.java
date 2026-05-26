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
    private final com.example.mpct.repository.ConfiguracionRepository configuracionRepository;

    @Transactional
    public TramiteResponse crearTramite(String ruc, String representanteLegal, String rubro, String dni, BigDecimal area, TipoTramite tipo, MultipartFile plano, java.util.List<MultipartFile> fotos) {
        
        if (tramiteRepository.findByRuc(ruc).isPresent()) {
            throw new RuntimeException("Ya existe un trámite asociado a este RUC.");
        }

        SunatRucResponse sunatData = sunatScrapingService.validarRuc(ruc);

        byte[] planoBytes;
        byte[] fotoBytes = null;
        byte[] foto2Bytes = null;
        byte[] foto3Bytes = null;
        byte[] foto4Bytes = null;

        try {
            planoBytes = plano.getBytes();
            if (fotos != null && !fotos.isEmpty()) {
                fotoBytes = fotos.get(0).getBytes();
                if (fotos.size() > 1) foto2Bytes = fotos.get(1).getBytes();
                if (fotos.size() > 2) foto3Bytes = fotos.get(2).getBytes();
                if (fotos.size() > 3) foto4Bytes = fotos.get(3).getBytes();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error al leer los archivos: " + e.getMessage());
        }

        BigDecimal precio = configuracionRepository.findByClave("PRECIO_LICENCIA")
                .map(com.example.mpct.model.entity.Configuracion::getValor)
                .orElse(new BigDecimal("180.00"));

        Tramite tramite = Tramite.builder()
                .ruc(ruc)
                .razonSocial(sunatData.razonSocial())
                .domicilioFiscal(sunatData.domicilioFiscal())
                .representanteLegal(representanteLegal)
                .dni(dni)
                .area(area)
                .rubro(rubro)
                .tipo(tipo)
                .estado(EstadoTramite.PENDIENTE_PAGO)
                .montoCobrado(precio)
                .archivoPlano(planoBytes)
                .archivoFoto(fotoBytes)
                .archivoFoto2(foto2Bytes)
                .archivoFoto3(foto3Bytes)
                .archivoFoto4(foto4Bytes)
                .build();

        tramite = tramiteRepository.save(tramite);
        return mapToResponse(tramite);
    }

    @Transactional(readOnly = true)
    public TramiteResponse obtenerTramitePorRuc(String ruc) {
        Tramite tramite = tramiteRepository.findByRuc(ruc)
                .orElseThrow(() -> new RuntimeException("No se encontró trámite para el RUC: " + ruc));
        return mapToResponse(tramite);
    }

    @Override
    @Transactional
    public TramiteResponse actualizarArchivos(String ruc, MultipartFile plano, MultipartFile foto, MultipartFile foto2, MultipartFile foto3, MultipartFile foto4) {
        Tramite tramite = tramiteRepository.findByRuc(ruc)
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
        Tramite tramite = tramiteRepository.findByRuc(ruc)
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
            tramite.setEstado(EstadoTramite.PAGADO);
            tramiteRepository.save(tramite);
            inspeccionService.programarInspeccionInicial(tramite);
        } else if ("BANCO_NACION".equals(metodoPago) && voucher != null) {
            pago.setEstadoPago("PENDIENTE");
            try {
                pago.setArchivoVoucher(voucher.getBytes());
            } catch (java.io.IOException e) {
                throw new RuntimeException("Error guardando el voucher");
            }
            pagoRepository.save(pago);
            tramite.setEstado(EstadoTramite.VALIDANDO_PAGO);
            tramiteRepository.save(tramite);
        } else {
            throw new RuntimeException("Método de pago inválido o falta voucher");
        }

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
