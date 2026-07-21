package com.example.mpct.repository;

import com.example.mpct.model.entity.Comprobante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, UUID> {
    @Query(value = "SELECT nextval('seq_factura_interna')", nativeQuery = true)
    Long getNextFacturaCorrelativo();

    @Query(value = "SELECT nextval('seq_boleta_interna')", nativeQuery = true)
    Long getNextBoletaCorrelativo();
}
