package com.example.mpct.service;

import com.example.mpct.dto.tramite.TramiteResponse;
import com.example.mpct.model.entity.User;
import com.example.mpct.model.enums.TipoTramite;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface TramiteService {
    TramiteResponse crearTramite(User user, TipoTramite tipo, Boolean declaracionSinCambios, Double area, MultipartFile plano, MultipartFile foto);
    TramiteResponse pagarTramite(User user, UUID tramiteId, String mockTransactionId);
    TramiteResponse actualizarArchivos(User user, UUID tramiteId, MultipartFile plano, MultipartFile foto);
    List<TramiteResponse> misTramites(User user);
}
