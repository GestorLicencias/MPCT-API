package com.example.mpct.service;

import com.example.mpct.dto.caja.AbrirCajaRequest;
import com.example.mpct.dto.caja.PagoPresencialRequest;
import com.example.mpct.model.entity.Caja;
import com.example.mpct.model.entity.User;
import com.example.mpct.model.enums.EstadoCaja;
import com.example.mpct.repository.CajaRepository;
import com.example.mpct.repository.MovimientoCajaRepository;
import com.example.mpct.repository.UserRepository;
import com.example.mpct.service.impl.CajaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CajaServiceTest {

    @Mock
    private CajaRepository cajaRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MovimientoCajaRepository movimientoCajaRepository;

    @InjectMocks
    private CajaServiceImpl cajaService;

    private User testUser;
    private Caja cajaAbierta;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("admin@mpct.gob.pe");

        cajaAbierta = new Caja();
        cajaAbierta.setId(UUID.randomUUID());
        cajaAbierta.setUsuario(testUser);
        cajaAbierta.setEstado(EstadoCaja.ABIERTA);
        cajaAbierta.setMontoInicial(new BigDecimal("100.00"));
    }

    @Test
    void testAbrirCajaDoble() {
        when(userRepository.findByEmail("admin@mpct.gob.pe")).thenReturn(Optional.of(testUser));
        when(cajaRepository.findByUsuarioIdAndEstado(testUser.getId(), EstadoCaja.ABIERTA)).thenReturn(Optional.of(cajaAbierta));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            cajaService.abrirCaja("admin@mpct.gob.pe", new AbrirCajaRequest(new BigDecimal("50.00")));
        });
        assertTrue(exception.getMessage().contains("ya tiene una caja abierta"));
    }

    @Test
    void testPagarSinCajaAbierta() {
        when(userRepository.findByEmail("admin@mpct.gob.pe")).thenReturn(Optional.of(testUser));
        when(cajaRepository.findByUsuarioIdAndEstado(testUser.getId(), EstadoCaja.ABIERTA)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            cajaService.registrarPagoPresencial("admin@mpct.gob.pe", new PagoPresencialRequest("20123456789", java.util.List.of(
                new PagoPresencialRequest.PagoDetalleDTO(com.example.mpct.model.enums.MetodoPago.EFECTIVO, new java.math.BigDecimal("100.00"), new java.math.BigDecimal("100.00"), null)
            )));
        });
        assertTrue(exception.getMessage().contains("Debe abrir la caja antes de registrar pagos"));
    }

    @Test
    void testCerrarCajaCalculoDiferencia() {
        when(userRepository.findByEmail("admin@mpct.gob.pe")).thenReturn(Optional.of(testUser));
        when(cajaRepository.findByUsuarioIdAndEstado(testUser.getId(), EstadoCaja.ABIERTA)).thenReturn(Optional.of(cajaAbierta));
        when(movimientoCajaRepository.findByCajaIdOrderByCreatedAtAsc(cajaAbierta.getId())).thenReturn(List.of());

        // Al no haber movimientos, monto final
        // Act
        cajaService.cerrarCaja("admin@mpct.gob.pe", java.math.BigDecimal.valueOf(100.00));

        assertEquals(EstadoCaja.CERRADA, cajaAbierta.getEstado());
        assertEquals(0, new BigDecimal("100.00").compareTo(cajaAbierta.getMontoFinal()));
        verify(cajaRepository, times(1)).save(cajaAbierta);
    }
}
