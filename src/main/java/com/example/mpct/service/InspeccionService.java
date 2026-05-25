package com.example.mpct.service;

import com.example.mpct.model.entity.Inspeccion;
import com.example.mpct.model.entity.Tramite;
import com.example.mpct.model.entity.User;

import java.util.UUID;

public interface InspeccionService {
    Inspeccion programarInspeccionInicial(Tramite tramite);
    Inspeccion evaluarInspeccion(User inspector, UUID inspeccionId, boolean conforme, String observaciones);
    java.util.List<Inspeccion> obtenerInspeccionesPendientes();
    Inspeccion obtenerInspeccionPorId(UUID id);
}
