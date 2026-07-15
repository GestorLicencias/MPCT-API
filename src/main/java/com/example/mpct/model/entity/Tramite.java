package com.example.mpct.model.entity;

import com.example.mpct.model.enums.EstadoTramite;
import com.example.mpct.model.enums.TipoTramite;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tramites")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Tramite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(nullable = false)
    private String razonSocial;

    @Column(nullable = false)
    private String domicilioFiscal;

    @Column(nullable = false)
    private String representanteLegal;

    @Column(nullable = false)
    private String rubro;

    @Column(nullable = true, length = 8)
    private String dni;
    
    @Column(nullable = true)
    private String email;

    @Column(nullable = true, precision = 10, scale = 2)
    private BigDecimal area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private TipoTramite tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTramite estado;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private byte[] archivoPlano;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private byte[] archivoFoto;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private byte[] archivoFoto2;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private byte[] archivoFoto3;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private byte[] archivoFoto4;

    @OneToMany(mappedBy = "tramite", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<Inspeccion> inspecciones = new java.util.ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String observacionesGenerales;

    @Column(columnDefinition = "TEXT")
    private String archivosObservados;

    @Column
    private LocalDateTime fechaLimiteSubsanacion;

    @Column(nullable = false)
    private BigDecimal montoCobrado;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean requiereInspeccion = false;
}
