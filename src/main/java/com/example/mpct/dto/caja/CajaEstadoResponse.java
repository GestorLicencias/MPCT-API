package com.example.mpct.dto.caja;

import java.math.BigDecimal;
import java.util.UUID;

public record CajaEstadoResponse(
    UUID cajaId,
    boolean abierta,
    BigDecimal montoInicial,
    BigDecimal ingresos,
    BigDecimal egresos,
    BigDecimal montoActual
) {}
