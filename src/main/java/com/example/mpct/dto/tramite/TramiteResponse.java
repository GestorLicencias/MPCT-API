package com.example.mpct.dto.tramite;

import com.example.mpct.model.enums.EstadoTramite;
import com.example.mpct.model.enums.TipoTramite;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TramiteResponse(
        UUID id,
        TipoTramite tipo,
        EstadoTramite estado,
        BigDecimal montoCobrado,
        String archivoPlanoUrl,
        String archivoFotoUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
