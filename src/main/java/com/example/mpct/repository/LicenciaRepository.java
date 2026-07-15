package com.example.mpct.repository;

import com.example.mpct.model.entity.Licencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LicenciaRepository extends JpaRepository<Licencia, UUID> {
    Optional<Licencia> findByTramiteId(UUID tramiteId);
    Optional<Licencia> findByNumeroLicencia(String numeroLicencia);
    java.util.List<Licencia> findByEstado(com.example.mpct.model.enums.EstadoLicencia estado);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM licencias WHERE tramite_id IN (SELECT id FROM tramites WHERE ruc = :ruc) AND estado IN ('VIGENTE', 'VENCIDA') ORDER BY fecha_emision DESC LIMIT 1", nativeQuery = true)
    Optional<Licencia> findByTramiteRuc(@org.springframework.data.repository.query.Param("ruc") String ruc);

    @org.springframework.data.jpa.repository.Query(value = "SELECT pdf_archivo FROM licencias WHERE tramite_id IN (SELECT id FROM tramites WHERE ruc = :ruc) ORDER BY fecha_emision DESC LIMIT 1", nativeQuery = true)
    Optional<byte[]> findPdfArchivoByTramiteRuc(@org.springframework.data.repository.query.Param("ruc") String ruc);
}
