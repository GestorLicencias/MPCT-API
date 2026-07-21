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
    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inspeccion i JOIN FETCH i.tramite WHERE i.estado = :estado")
    List<Inspeccion> findByEstado(com.example.mpct.model.enums.EstadoInspeccion estado);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM Inspeccion i WHERE i.inspector.id = :inspectorId AND CAST(i.fechaProgramada AS date) = CAST(:fecha AS date)")
    long countByInspectorIdAndFechaProgramada(@org.springframework.data.repository.query.Param("inspectorId") java.util.UUID inspectorId, @org.springframework.data.repository.query.Param("fecha") java.time.LocalDateTime fecha);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inspeccion i JOIN FETCH i.tramite t WHERE i.inspector.id = :inspectorId AND i.estado = :estado AND t.estado NOT IN (com.example.mpct.model.enums.EstadoTramite.RECHAZADO, com.example.mpct.model.enums.EstadoTramite.ABANDONADO)")
    List<Inspeccion> findByInspectorIdAndEstado(@org.springframework.data.repository.query.Param("inspectorId") java.util.UUID inspectorId, @org.springframework.data.repository.query.Param("estado") com.example.mpct.model.enums.EstadoInspeccion estado);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Inspeccion i JOIN FETCH i.tramite t WHERE i.inspector.id = :inspectorId AND i.estado = :estado AND CAST(i.fechaProgramada AS date) <= CAST(:fecha AS date) AND t.estado NOT IN (com.example.mpct.model.enums.EstadoTramite.RECHAZADO, com.example.mpct.model.enums.EstadoTramite.ABANDONADO)")
    List<Inspeccion> findByInspectorIdAndEstadoAndFechaProgramada(@org.springframework.data.repository.query.Param("inspectorId") java.util.UUID inspectorId, @org.springframework.data.repository.query.Param("estado") com.example.mpct.model.enums.EstadoInspeccion estado, @org.springframework.data.repository.query.Param("fecha") java.time.LocalDateTime fecha);
}
