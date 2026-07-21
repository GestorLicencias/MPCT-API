package com.example.mpct.model.entity;

import com.example.mpct.model.enums.TipoComprobante;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comprobantes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"serie", "correlativo"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id", nullable = false, unique = true)
    private Pago pago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoComprobante tipo;

    @Column(nullable = false, length = 4)
    private String serie; 

    @Column(nullable = false)
    private Long correlativo; 

    @Column(nullable = false)
    private BigDecimal montoTotal;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;
    
    // Snapshots para auditoría y reimpresión
    @Column(nullable = false)
    private String razonSocialSnapshot;
    
    @Column(nullable = false)
    private String direccionSnapshot;
    
    @Column(nullable = false)
    private String documentoClienteSnapshot;
}
