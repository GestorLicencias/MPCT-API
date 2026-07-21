package com.example.mpct.model.entity;

import com.example.mpct.model.enums.EstadoCaja;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cajas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private LocalDateTime fechaApertura;

    @Column(nullable = true)
    private LocalDateTime fechaCierre;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial;

    @Column(nullable = true, precision = 10, scale = 2)
    private BigDecimal montoFinal; // Expected

    @Column(nullable = true, precision = 10, scale = 2)
    private BigDecimal montoDeclarado; // Counted by cashier

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCaja estado; // ABIERTA, CERRADA

    @Column(nullable = true)
    private String motivoCierreForzado;

    @Column(nullable = true)
    private String cerradoPorAdmin;
}
