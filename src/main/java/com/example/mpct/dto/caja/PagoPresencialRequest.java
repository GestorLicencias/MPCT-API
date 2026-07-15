package com.example.mpct.dto.caja;

import jakarta.validation.constraints.NotBlank;

public record PagoPresencialRequest(
    @NotBlank(message = "El RUC del trámite es obligatorio")
    String ruc,
    @NotBlank(message = "El método de pago es obligatorio (EFECTIVO o TARJETA)")
    String metodoPago
) {}
