package com.example.mpct.service;

import com.example.mpct.model.entity.Inspeccion;
import com.example.mpct.model.entity.Tramite;

public interface InspeccionSchedulingService {

    int MINIMO_DIAS_HABILES_PRIMERA_VISITA = 1;

    Inspeccion programarInspeccion(Tramite tramite, int numeroVisita, int diasMinimosHabiles);
}
