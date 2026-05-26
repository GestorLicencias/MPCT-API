package com.example.mpct.dto.tramite;

import com.example.mpct.model.enums.EstadoTramite;
import com.example.mpct.model.enums.TipoTramite;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TramiteResponse(
        UUID id,
        String ruc,
        String razonSocial,
        String domicilioFiscal,
        String representanteLegal,
        String rubro,
        String dni,
        BigDecimal area,
        TipoTramite tipo,
        EstadoTramite estado,
        BigDecimal montoCobrado,
        String archivoPlanoUrl,
        String archivoFotoUrl,
        String archivoFoto2Url,
        String archivoFoto3Url,
        String archivoFoto4Url,
        String certificadoUrl,
        String observacionesGenerales,
        String archivosObservados,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean pagoRechazado
) {
}
