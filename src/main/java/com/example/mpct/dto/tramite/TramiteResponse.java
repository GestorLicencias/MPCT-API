package com.example.mpct.dto.tramite;

import com.example.mpct.model.enums.EstadoLicencia;
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
        String email,
        BigDecimal area,
        TipoTramite tipo,
        EstadoTramite estado,
        BigDecimal montoCobrado,
        String archivoPlanoUrl,
        String certificadoUrl,
        String observacionesGenerales,
        String archivosObservados,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean pagoRechazado,
        EstadoLicencia estadoLicencia,
        LocalDateTime fechaVencimientoLicencia
) {
}
