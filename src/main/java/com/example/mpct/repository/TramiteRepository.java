package com.example.mpct.repository;

import com.example.mpct.model.entity.Tramite;
import com.example.mpct.model.enums.EstadoTramite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TramiteRepository extends JpaRepository<Tramite, UUID> {
    List<Tramite> findBySolicitanteId(UUID solicitanteId);
    List<Tramite> findByEstado(EstadoTramite estado);
}
