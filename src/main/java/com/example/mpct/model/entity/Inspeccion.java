package com.example.mpct.model.entity;

import com.example.mpct.model.enums.EstadoInspeccion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inspecciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inspeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tramite_id", nullable = false)
    private Tramite tramite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id")
    private User inspector; // Nullable al inicio, hasta que se asigne

    @Column(nullable = false)
    private Integer numeroInspeccion; // 1 o 2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInspeccion estado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    private LocalDateTime fechaProgramada;

    private LocalDateTime fechaRealizada;
}
