package com.example.mpct.service.impl;

import com.example.mpct.model.entity.Tramite;
import com.example.mpct.service.MercadoPagoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class MercadoPagoServiceImpl implements MercadoPagoService {

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

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
                    .title("Licencia de Funcionamiento - Trámite #" + tramite.getId().toString().substring(0, 8))
                    .description("Pago por trámite de " + tramite.getTipo().name())
                    .quantity(1)
                    .unitPrice(tramite.getMontoCobrado())
                    .currencyId("PEN")
                    .build();
            items.add(item);

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(frontendUrl + "/dashboard/solicitante")
                    .pending(frontendUrl + "/dashboard/solicitante")
                    .failure(frontendUrl + "/dashboard/solicitante")
                    .build();

            PreferenceRequest request = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(tramite.getId().toString())
                    .build();

            Preference preference = client.create(request);
            
            // Retornamos la URL al frontend para redirigir (init_point para pasarela real de producción)
            return preference.getInitPoint();

        } catch (com.mercadopago.exceptions.MPApiException apiEx) {
            String details = apiEx.getApiResponse() != null ? apiEx.getApiResponse().getContent() : apiEx.getMessage();
            throw new RuntimeException("Mercado Pago API Error: " + details, apiEx);
        } catch (Exception e) {
            throw new RuntimeException("Error interno al comunicarse con Mercado Pago: " + e.getMessage(), e);
        }
    }
}
