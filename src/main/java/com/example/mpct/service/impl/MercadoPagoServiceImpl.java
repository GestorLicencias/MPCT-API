package com.example.mpct.service.impl;

import com.example.mpct.model.entity.Tramite;
import com.example.mpct.model.entity.Pago;
import com.example.mpct.service.MercadoPagoService;
import com.example.mpct.service.InspeccionService;
import com.example.mpct.service.InspeccionSchedulingService;
import com.example.mpct.repository.TramiteRepository;
import com.example.mpct.repository.PagoRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    private final TramiteRepository tramiteRepository;
    private final PagoRepository pagoRepository;
    private final InspeccionService inspeccionService;
    private final InspeccionSchedulingService inspeccionSchedulingService;

    public MercadoPagoServiceImpl(TramiteRepository tramiteRepository, PagoRepository pagoRepository, InspeccionService inspeccionService, InspeccionSchedulingService inspeccionSchedulingService) {
        this.tramiteRepository = tramiteRepository;
        this.pagoRepository = pagoRepository;
        this.inspeccionService = inspeccionService;
        this.inspeccionSchedulingService = inspeccionSchedulingService;
    }

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${app.frontend.url:https://mpct-frontend.vercel.app}")
    private String frontendUrl;

    @Value("${app.backend.url:https://mpct-api-264213836001.us-east1.run.app}")
    private String backendUrl;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    @Override
    public String crearPreferenciaPago(Tramite tramite) {
        try {
            PreferenceClient client = new PreferenceClient();

            List<PreferenceItemRequest> items = new ArrayList<>();
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title("Licencia de Funcionamiento - RUC " + tramite.getRuc())
                    .quantity(1)
                    .unitPrice(tramite.getMontoCobrado())
                    .currencyId("PEN")
                    .build();
            items.add(item);

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(frontendUrl + "/seguimiento/" + tramite.getRuc() + "?status=success")
                    .pending(frontendUrl + "/seguimiento/" + tramite.getRuc() + "?status=pending")
                    .failure(frontendUrl + "/seguimiento/" + tramite.getRuc() + "?status=failure")
                    .build();

            PreferenceRequest request = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .notificationUrl(backendUrl + "/api/v1/tramites/webhook/mercadopago")
                    .externalReference(tramite.getId().toString())
                    .build();

            Preference preference = client.create(request);
            System.out.println("Preferencia creada exitosamente para Trámite RUC " + tramite.getRuc());
            return preference.getInitPoint();

        } catch (com.mercadopago.exceptions.MPApiException apiEx) {
            String details = apiEx.getApiResponse() != null ? apiEx.getApiResponse().getContent() : apiEx.getMessage();
            System.err.println("Mercado Pago API Error: " + details);
            throw new RuntimeException("Mercado Pago API Error: " + details, apiEx);
        } catch (Exception e) {
            System.err.println("Error interno al comunicarse con Mercado Pago: " + e.getMessage());
            throw new RuntimeException("Error interno al comunicarse con Mercado Pago: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void procesarWebhook(String paymentId) {
        try {
            System.out.println("Recibido webhook de pago de MercadoPago. PaymentID: " + paymentId);
            PaymentClient client = new PaymentClient();
            Payment payment = client.get(Long.parseLong(paymentId));
            
            System.out.println("Estado del pago en MercadoPago: " + payment.getStatus());
            
            if ("approved".equals(payment.getStatus())) {
                String externalReference = payment.getExternalReference();
                if (externalReference == null || externalReference.isEmpty()) {
                    System.err.println("El pago " + paymentId + " no tiene external_reference");
                    return;
                }

                UUID tramiteId = UUID.fromString(externalReference);
                Tramite tramite = tramiteRepository.findById(tramiteId).orElse(null);
                
                if (tramite == null) {
                    System.err.println("Trámite no encontrado para ID " + tramiteId);
                    return;
                }

                if (tramite.getEstado() != com.example.mpct.model.enums.EstadoTramite.PENDIENTE_PAGO) {
                    System.out.println("El trámite " + tramite.getRuc() + " ya no está pendiente de pago. Ignorando.");
                    return;
                }

                System.out.println("Pago APROBADO para Trámite RUC " + tramite.getRuc() + ". Actualizando estado.");

                Pago pago = new Pago();
                pago.setTramite(tramite);
                pago.setMonto(tramite.getMontoCobrado());
                pago.setMetodoPago("MERCADO_PAGO");
                pago.setPasarelaTransactionId(paymentId);
                pago.setEstadoPago("COMPLETADO");
                pago.setFechaPago(java.time.LocalDateTime.now());
                pagoRepository.save(pago);
                if (tramite.getRequiereInspeccion()) {
                    tramite.setEstado(com.example.mpct.model.enums.EstadoTramite.PENDIENTE_REVISION);
                } else {
                    tramite.setEstado(com.example.mpct.model.enums.EstadoTramite.PROGRAMADO);
                    inspeccionSchedulingService.programarInspeccion(tramite, 1, 3);
                }
                tramiteRepository.save(tramite);
                
                System.out.println("Trámite RUC " + tramite.getRuc() + " pagado y actualizado exitosamente.");
            }
        } catch (Exception e) {
            System.err.println("Error al procesar el webhook de MercadoPago para paymentId " + paymentId + ": " + e.getMessage());
            throw new RuntimeException("Error procesando Webhook", e);
        }
    }
}
