package com.example.mpct.service;

import com.example.mpct.dto.tramite.TramiteResponse;
import com.example.mpct.model.enums.TipoTramite;
import org.springframework.web.multipart.MultipartFile;

public interface TramiteService {
    TramiteResponse crearTramite(String ruc, String representanteLegal, String rubro, String dni, java.math.BigDecimal area, com.example.mpct.model.enums.TipoTramite tipo, org.springframework.web.multipart.MultipartFile plano, java.util.List<org.springframework.web.multipart.MultipartFile> fotos);
    TramiteResponse obtenerTramitePorRuc(String ruc);
    TramiteResponse actualizarArchivos(String ruc, MultipartFile plano, MultipartFile foto, MultipartFile foto2, MultipartFile foto3, MultipartFile foto4);
    TramiteResponse pagarTramite(String ruc, String metodoPago, MultipartFile voucher, String transactionId);
    TramiteResponse aprobarTramiteRevision(String ruc);
}
