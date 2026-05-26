package com.example.mpct.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import com.example.mpct.model.enums.EstadoLicencia;

@Entity
@Table(name = "licencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Licencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tramite_id", nullable = false)
    private Tramite tramite;

    @Column(nullable = false, unique = true)
    private String numeroLicencia;

    @Column(columnDefinition = "TEXT")
    private String qrData;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] pdfArchivo;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @Column(nullable = false)
    private LocalDateTime fechaVencimiento;

    @Column
    private String codigoCatastral;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoLicencia estado = EstadoLicencia.ACTIVA;
}
