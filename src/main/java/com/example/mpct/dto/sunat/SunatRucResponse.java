package com.example.mpct.dto.sunat;

public record SunatRucResponse(
        String ruc,
        String razonSocial,
        String estado, // ACTIVO
        String condicion, // HABIDO
        String domicilioFiscal
) {
}
