package com.example.mpct.service;

import com.example.mpct.service.impl.InspeccionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class InspeccionServiceTest {

    @InjectMocks
    private InspeccionServiceImpl inspeccionService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testSumarDiasHabiles_ExcluyeFinesDeSemanaYFeriados() {
        // 24 de julio de 2026 es viernes
        LocalDateTime viernes = LocalDateTime.of(2026, 7, 24, 10, 0);

        // Si sumamos 3 días hábiles:
        // Viernes 24
        // Sabado 25 (fin de semana) -> salta
        // Domingo 26 (fin de semana) -> salta
        // Lunes 27 (Día 1)
        // Martes 28 (Feriado) -> salta
        // Miercoles 29 (Feriado) -> salta
        // Jueves 30 (Día 2)
        // Viernes 31 (Día 3)
        LocalDateTime resultado = inspeccionService.sumarDiasHabiles(viernes, 3);
        
        assertEquals(31, resultado.getDayOfMonth());
        assertEquals(7, resultado.getMonthValue());
        assertEquals(2026, resultado.getYear());
    }

    @Test
    void testSumar30DiasHabiles() {
        // Lunes 1 de junio 2026
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 10, 0);
        
        // Sumamos 30 días hábiles
        // Junio tiene 30 días. Fines de semana en Junio 2026: 6, 7, 13, 14, 20, 21, 27, 28 (8 días)
        // Días hábiles en junio: 30 - 8 = 22 días.
        // Faltan 8 días hábiles.
        // Julio 2026 empieza en Miércoles.
        // 1 al 8 de julio (Día 1 a 8). Fines de semana: 4, 5 (2 días).
        // Por lo tanto, 8 días hábiles nos llevan al Viernes 10 de Julio.
        LocalDateTime resultado = inspeccionService.sumarDiasHabiles(inicio, 30);

        assertEquals(13, resultado.getDayOfMonth());
        assertEquals(7, resultado.getMonthValue());
    }
}
