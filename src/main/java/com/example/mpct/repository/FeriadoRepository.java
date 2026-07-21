package com.example.mpct.repository;

import com.example.mpct.model.entity.Feriado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface FeriadoRepository extends JpaRepository<Feriado, UUID> {
    boolean existsByFecha(LocalDate fecha);
}
