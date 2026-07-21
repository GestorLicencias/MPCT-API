package com.example.mpct.model.entity;

import com.example.mpct.model.enums.MetodoPago;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pago_detalles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"metodo", "referencia"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false)
    @ToString.Exclude
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetodoPago metodo;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = true)
    private BigDecimal montoEntregado;

    @Column(nullable = true)
    private String referencia;
}
