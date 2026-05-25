package com.example.mpct.service.impl;

import com.example.mpct.service.*;

import com.example.mpct.dto.tramite.TramiteResponse;
import com.example.mpct.model.entity.*;
import com.example.mpct.model.enums.*;
import com.example.mpct.repository.ConfiguracionRepository;
import com.example.mpct.repository.PagoRepository;
import com.example.mpct.repository.TramiteRepository;
import com.example.mpct.repository.LicenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TramiteServiceImpl implements TramiteService {

    private final TramiteRepository tramiteRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final StorageService storageService;
    private final PagoRepository pagoRepository;
    private final InspeccionService inspeccionService;
    private final LicenciaRepository licenciaRepository;

    @Transactional
    public TramiteResponse crearTramite(User user, TipoTramite tipo, Boolean declaracionSinCambios, Double area, MultipartFile plano, MultipartFile foto) {
        
        // Verificar si ya tiene una licencia activa
        if (licenciaRepository.existsByTramite_Solicitante_IdAndFechaVencimientoAfter(user.getId(), LocalDateTime.now())) {
            throw new RuntimeException("Usted ya cuenta con una licencia activa válida.");
        }

        // Validar si es renovación y sin cambios
        if (tipo == TipoTramite.RENOVACION && (declaracionSinCambios == null || !declaracionSinCambios)) {
            throw new RuntimeException("Para una renovación con cambios, debe iniciar un Trámite NUEVO.");
        }

        if (area == null || area <= 0) {
            throw new RuntimeException("El área del local debe ser mayor a 0.");
        }

        byte[] planoBytes;
        byte[] fotoBytes;
        try {
            planoBytes = plano.getBytes();
            fotoBytes = foto.getBytes();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error al leer los archivos: " + e.getMessage());
        }

        BigDecimal monto;
        if (tipo == TipoTramite.NUEVO) {
            monto = configuracionRepository.findByClave("PRECIO_NUEVO")
                    .map(Configuracion::getValor)
                    .orElse(new BigDecimal("380.00"));
        } else {
            monto = configuracionRepository.findByClave("PRECIO_RENOVACION")
                    .map(Configuracion::getValor)
                    .orElse(new BigDecimal("180.00"));
        }

        Tramite tramite = Tramite.builder()
                .solicitante(user)
                .tipo(tipo)
                .estado(EstadoTramite.PENDIENTE)
                .montoCobrado(monto)
                .archivoPlano(planoBytes)
                .archivoFoto(fotoBytes)
                .declaracionJuradaSinCambios(declaracionSinCambios != null && declaracionSinCambios)
                .area(area)
                .build();

        tramite = tramiteRepository.save(tramite);
        return mapToResponse(tramite);
    }

    @Transactional
    public TramiteResponse pagarTramite(User user, UUID tramiteId, String mockTransactionId) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (!tramite.getSolicitante().getId().equals(user.getId())) {
            throw new RuntimeException("No tiene permisos sobre este trámite");
        }

        if (tramite.getEstado() != EstadoTramite.PENDIENTE) {
            throw new RuntimeException("El trámite no está en estado PENDIENTE");
        }

        Pago pago = Pago.builder()
                .tramite(tramite)
                .monto(tramite.getMontoCobrado())
                .pasarelaTransactionId(mockTransactionId)
                .build();
        
        pagoRepository.save(pago);

        tramite.setEstado(EstadoTramite.PAGADO);
        tramiteRepository.save(tramite);
        
        inspeccionService.programarInspeccionInicial(tramite);

        return mapToResponse(tramite);
    }

    @Transactional
    public TramiteResponse actualizarArchivos(User user, UUID tramiteId, MultipartFile plano, MultipartFile foto) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new RuntimeException("Trámite no encontrado"));

        if (!tramite.getSolicitante().getId().equals(user.getId())) {
            throw new RuntimeException("No tiene permisos sobre este trámite");
        }

        if (tramite.getEstado() != EstadoTramite.OBSERVADO) {
            throw new RuntimeException("Solo se pueden actualizar archivos si el trámite está OBSERVADO");
        }

        try {
            if (plano != null && !plano.isEmpty()) {
                tramite.setArchivoPlano(plano.getBytes());
            }
            if (foto != null && !foto.isEmpty()) {
                tramite.setArchivoFoto(foto.getBytes());
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error al leer los archivos: " + e.getMessage());
        }

        // Vuelve a pagado para que el inspector lo vuelva a revisar
        tramite.setEstado(EstadoTramite.PAGADO);
        tramiteRepository.save(tramite);

        return mapToResponse(tramite);
    }

    public List<TramiteResponse> misTramites(User user) {
        return tramiteRepository.findBySolicitanteId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TramiteResponse mapToResponse(Tramite t) {
        return new TramiteResponse(
                t.getId(), t.getTipo(), t.getEstado(), t.getMontoCobrado(),
                "/api/v1/tramites/" + t.getId() + "/archivos/plano",
                "/api/v1/tramites/" + t.getId() + "/archivos/foto",
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}
