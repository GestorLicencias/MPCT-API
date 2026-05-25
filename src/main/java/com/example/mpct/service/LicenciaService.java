package com.example.mpct.service;

import com.example.mpct.model.entity.Licencia;
import com.example.mpct.model.entity.Tramite;

public interface LicenciaService {
    Licencia generarLicencia(Tramite tramite);
}
