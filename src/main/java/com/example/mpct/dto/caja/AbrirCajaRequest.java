package com.example.mpct.dto.caja;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record AbrirCajaRequest(
    @NotNull(message = "El monto inicial es obligatorio")
    @Min(value = 0, message = "El monto inicial no puede ser negativo")
    BigDecimal montoInicial
) {}
