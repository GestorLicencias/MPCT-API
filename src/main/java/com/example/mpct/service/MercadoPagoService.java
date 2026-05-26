package com.example.mpct.service;

import com.example.mpct.model.entity.Tramite;

public interface MercadoPagoService {
    String crearPreferenciaPago(Tramite tramite);
    void procesarWebhook(String paymentId);
}
