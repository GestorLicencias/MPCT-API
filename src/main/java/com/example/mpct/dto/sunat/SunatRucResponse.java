package com.example.mpct.dto.sunat;

public record SunatRucResponse(
        String ruc,
        String razonSocial,
        String estado,
        String condicion,
        String domicilioFiscal,
        String rubro
) {
}
