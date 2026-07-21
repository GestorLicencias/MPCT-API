package com.example.mpct.dto.caja;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ForzarCierreRequest(
        @NotNull(message = "El monto físico es obligatorio") BigDecimal montoFisico,
        @NotNull(message = "El motivo es obligatorio") String motivo
) {
}
