package com.example.mpct.dto.pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoResponse(
        UUID id,
        String rucTramite,
        String razonSocial,
        BigDecimal monto,
        String metodoPago,
        String estadoPago,
        LocalDateTime fechaPago
) {
}
