package com.example.mpct.dto.caja;

import com.example.mpct.model.enums.MetodoPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PagoPresencialRequest(
    @NotBlank(message = "El RUC del trámite es obligatorio")
    String ruc,
    
    @NotNull(message = "Debe especificar los detalles del pago")
    List<PagoDetalleDTO> detalles
) {
    public record PagoDetalleDTO(
        @NotNull(message = "El método es obligatorio")
        MetodoPago metodo,
        
        @NotNull(message = "El monto es obligatorio")
        BigDecimal monto,
        
        BigDecimal montoEntregado,
        String referencia
    ) {}
}
