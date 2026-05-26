package com.example.mpct.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tramite_id", nullable = false)
    private Tramite tramite;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private String metodoPago; // MERCADO_PAGO, BANCO_NACION

    @Column(nullable = false)
    private String estadoPago; // PENDIENTE, COMPLETADO, RECHAZADO

    @Column(nullable = true)
    private String pasarelaTransactionId;

    @Column(nullable = true)
    private String archivoVoucherUrl;

    @Lob
    @Column(nullable = true)
    private byte[] archivoVoucher;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaPago;
}
