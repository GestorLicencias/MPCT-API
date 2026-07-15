package com.example.mpct.service;

import com.example.mpct.dto.caja.AbrirCajaRequest;
import com.example.mpct.dto.caja.CajaEstadoResponse;
import com.example.mpct.dto.caja.PagoPresencialRequest;
import com.example.mpct.dto.tramite.TramiteResponse;

public interface CajaService {
    void abrirCaja(String email, AbrirCajaRequest request);
    void cerrarCaja(String email, java.math.BigDecimal montoFisico);
    CajaEstadoResponse obtenerEstadoCaja(String email);
    TramiteResponse registrarPagoPresencial(String email, PagoPresencialRequest request);
    java.util.List<java.util.Map<String, Object>> obtenerAlertasLicencias();
    void enviarRecordatorioLicencia(String ruc);
}
