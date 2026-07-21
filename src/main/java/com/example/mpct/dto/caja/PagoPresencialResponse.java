package com.example.mpct.dto.caja;

import com.example.mpct.dto.tramite.TramiteResponse;

import java.math.BigDecimal;

public record PagoPresencialResponse(
    TramiteResponse tramite,
    BigDecimal vueltoTotal,
    String mensaje
) {}
