package com.example.mpct.service;

import com.example.mpct.model.entity.Inspeccion;
import com.example.mpct.model.entity.Tramite;

public interface InspeccionSchedulingService {
    Inspeccion programarInspeccion(Tramite tramite, int numeroVisita, int diasMinimosHabiles);
}
