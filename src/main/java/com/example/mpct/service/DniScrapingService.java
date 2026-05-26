package com.example.mpct.service;

import com.example.mpct.dto.sunat.SunatRucResponse; // reusing a simple record or creating a new one. Wait, I will just return a String.

public interface DniScrapingService {
    String obtenerNombresPorDni(String dni);
}
