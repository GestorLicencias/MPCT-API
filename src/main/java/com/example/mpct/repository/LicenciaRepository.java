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
}
