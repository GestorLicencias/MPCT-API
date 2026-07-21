package com.example.mpct.service;

import com.example.mpct.dto.caja.PagoPresencialRequest;
import com.example.mpct.dto.caja.PagoPresencialResponse;
import com.example.mpct.model.entity.*;
import com.example.mpct.model.enums.*;
import com.example.mpct.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("local")
public class CajaServiceIntegrationTest {

    @Autowired
    private CajaService cajaService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CajaRepository cajaRepository;
    @Autowired
    private TramiteRepository tramiteRepository;
    @Autowired
    private PagoDetalleRepository pagoDetalleRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private MovimientoCajaRepository movimientoCajaRepository;
    @Autowired
    private ComprobanteRepository comprobanteRepository;

    private User cajero;
    private Tramite tramite;

    @BeforeEach
    void setUp() {
        // Setup User and Caja
        cajero = userRepository.findByEmail("admin@mpct.gob.pe").orElseGet(() -> {
            User u = new User();
            u.setEmail("admin@mpct.gob.pe");
            u.setRole(Role.ADMIN);
            return userRepository.save(u);
        });

        comprobanteRepository.deleteAll();
        pagoDetalleRepository.deleteAll();
        pagoRepository.deleteAll();
        movimientoCajaRepository.deleteAll();
        cajaRepository.deleteAll();
        Caja caja = new Caja();
        caja.setUsuario(cajero);
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setFechaApertura(java.time.LocalDateTime.now());
        caja.setMontoInicial(new BigDecimal("100.00"));
        cajaRepository.save(caja);

        tramiteRepository.deleteAll();
        tramite = new Tramite();
        tramite.setRuc("20123456789");
        tramite.setEstado(EstadoTramite.PENDIENTE_PAGO);
        tramite.setMontoCobrado(new BigDecimal("150.00"));
        tramite.setRequiereInspeccion(false);
        tramite.setArchivoFoto(new byte[]{1, 2, 3});
        tramite.setArchivoPlano(new byte[]{1, 2, 3});
        tramite.setArea(new BigDecimal("50.00"));
        tramite.setDni("12345678");
        tramite.setDomicilioFiscal("Av Test");
        tramite.setEmail("test@mpct.gob.pe");
        tramite.setRazonSocial("Test SA");
        tramite.setRepresentanteLegal("Juan Perez");
        tramite.setRubro("Restaurante");
        tramite.setTipo(TipoTramite.NUEVO);
        tramiteRepository.save(tramite);
    }

    @Test
    void testPagoUnicoEfectivoConVuelto() {
        PagoPresencialRequest req = new PagoPresencialRequest(
                "20123456789",
                List.of(
                        new PagoPresencialRequest.PagoDetalleDTO(MetodoPago.EFECTIVO, new BigDecimal("150.00"), new BigDecimal("200.00"), null)
                )
        );

        PagoPresencialResponse res = cajaService.registrarPagoPresencial("admin@mpct.gob.pe", req);

        assertEquals(new BigDecimal("50.00"), res.vueltoTotal());
    }

    @Test
    void testPagoDivididoValidoEfectivoYape() {
        PagoPresencialRequest req = new PagoPresencialRequest(
                "20123456789",
                List.of(
                        new PagoPresencialRequest.PagoDetalleDTO(MetodoPago.EFECTIVO, new BigDecimal("50.00"), new BigDecimal("50.00"), null),
                        new PagoPresencialRequest.PagoDetalleDTO(MetodoPago.YAPE, new BigDecimal("100.00"), null, "REF-YAPE-123")
                )
        );

        assertDoesNotThrow(() -> cajaService.registrarPagoPresencial("admin@mpct.gob.pe", req));
        
        List<MovimientoCaja> movimientos = movimientoCajaRepository.findAll();
        long movCount = movimientos.stream().filter(m -> "EFECTIVO".equals(m.getMetodoPago()) || "YAPE".equals(m.getMetodoPago())).count();
        assertEquals(2, movCount);
    }

    @Test
    void testPagoDivididoInvalidoYapeTarjeta() {
        PagoPresencialRequest req = new PagoPresencialRequest(
                "20123456789",
                List.of(
                        new PagoPresencialRequest.PagoDetalleDTO(MetodoPago.YAPE, new BigDecimal("50.00"), null, "REF-YAPE-123"),
                        new PagoPresencialRequest.PagoDetalleDTO(MetodoPago.TARJETA, new BigDecimal("100.00"), null, "REF-TARJ-456")
                )
        );

        Exception e = assertThrows(RuntimeException.class, () -> cajaService.registrarPagoPresencial("admin@mpct.gob.pe", req));
        assertTrue(e.getMessage().contains("Solo se permite dividir el pago entre efectivo y Yape, o entre efectivo y tarjeta."));
    }

    @Test
    void testUniqueConstraintEnBaseDeDatos() {
        // Guardamos un pago detalle previo con YAPE y REFERENCIA-TEST
        Pago pago = new Pago();
        pago.setTramite(tramite);
        pago.setMonto(new BigDecimal("150.00"));
        pago.setMetodoPago("TEST");
        pago.setEstadoPago("TEST");
        pago = pagoRepository.save(pago);
        
        PagoDetalle pd = new PagoDetalle();
        pd.setPago(pago);
        pd.setMetodo(MetodoPago.YAPE);
        pd.setMonto(new BigDecimal("150.00"));
        pd.setReferencia("REF-UNIQUE-999");
        pagoDetalleRepository.saveAndFlush(pd);

        PagoPresencialRequest req = new PagoPresencialRequest(
                "20123456789",
                List.of(
                        new PagoPresencialRequest.PagoDetalleDTO(MetodoPago.YAPE, new BigDecimal("150.00"), null, "REF-UNIQUE-999")
                )
        );

        Exception e = assertThrows(RuntimeException.class, () -> cajaService.registrarPagoPresencial("admin@mpct.gob.pe", req));
        assertTrue(e.getMessage().contains("ya ha sido registrada previamente"));
    }
}
