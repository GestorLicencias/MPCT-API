package com.example.mpct.service;

import java.util.UUID;

public interface NotificacionService {
    void enviarEmail(String destinatario, String asunto, String mensaje, UUID referenciaTramiteId);
    void enviarEmailConAdjuntos(String destinatario, String asunto, String mensaje, UUID referenciaTramiteId, byte[] adjuntoCertificado, byte[] adjuntoComprobante);
}
