package com.example.mpct.repository;

import com.example.mpct.model.entity.Caja;
import com.example.mpct.model.enums.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CajaRepository extends JpaRepository<Caja, UUID> {
    Optional<Caja> findByUsuarioIdAndEstado(UUID usuarioId, EstadoCaja estado);
}
