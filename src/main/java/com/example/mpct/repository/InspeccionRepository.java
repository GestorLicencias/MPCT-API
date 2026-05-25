package com.example.mpct.repository;

import com.example.mpct.model.entity.Inspeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InspeccionRepository extends JpaRepository<Inspeccion, UUID> {
    List<Inspeccion> findByTramiteId(UUID tramiteId);
    List<Inspeccion> findByInspectorId(UUID inspectorId);
    List<Inspeccion> findByEstado(com.example.mpct.model.enums.EstadoInspeccion estado);
}
