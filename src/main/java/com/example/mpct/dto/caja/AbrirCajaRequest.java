package com.example.mpct.dto.caja;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Digits;

public record AbrirCajaRequest(
    @NotNull(message = "El monto inicial es obligatorio")
    @Min(value = 100, message = "El fondo inicial debe ser de al menos 100 (3 dígitos)")
    @Max(value = 999, message = "El fondo inicial no puede ser mayor a 999 (3 dígitos)")
    @Digits(integer = 3, fraction = 0, message = "El fondo inicial debe ser un número entero de 3 dígitos")
    BigDecimal montoInicial
) {}
