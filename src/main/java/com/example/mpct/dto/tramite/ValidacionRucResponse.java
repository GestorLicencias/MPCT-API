package com.example.mpct.dto.tramite;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidacionRucResponse {
    private boolean valido;
    private String mensaje;
    private String ruc;
    private String razonSocial;
    private String dniGerente;
    private String nombreGerente;
    private String domicilioFiscal;
}
