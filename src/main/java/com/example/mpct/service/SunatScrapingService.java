package com.example.mpct.service;

import com.example.mpct.dto.sunat.SunatRucResponse;

public interface SunatScrapingService {
    SunatRucResponse validarRuc(String ruc);
}
