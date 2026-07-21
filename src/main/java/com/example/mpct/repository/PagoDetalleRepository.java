package com.example.mpct.repository;

import com.example.mpct.model.entity.PagoDetalle;
import com.example.mpct.model.enums.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PagoDetalleRepository extends JpaRepository<PagoDetalle, UUID> {
    boolean existsByMetodoAndReferencia(MetodoPago metodo, String referencia);
}
