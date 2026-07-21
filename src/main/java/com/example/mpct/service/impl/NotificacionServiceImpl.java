package com.example.mpct.service.impl;

import com.example.mpct.model.entity.Notificacion;
import com.example.mpct.repository.NotificacionRepository;
import com.example.mpct.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final JavaMailSender mailSender;
    private final NotificacionRepository notificacionRepository;

    @Override
    public void enviarEmail(String destinatario, String asunto, String mensaje, UUID referenciaTramiteId) {
        Notificacion notificacion = Notificacion.builder()
                .destinatario(destinatario)
                .canal("EMAIL")
                .asunto(asunto)
                .mensaje(mensaje)
                .estado("PENDIENTE")
                .referenciaTramiteId(referenciaTramiteId)
                .build();
        
        notificacion = notificacionRepository.save(notificacion);

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(destinatario);
            mailMessage.setSubject(asunto);
            mailMessage.setText(mensaje);
            mailMessage.setFrom("no-reply@mpct.gob.pe");

            mailSender.send(mailMessage);

            notificacion.setEstado("ENVIADA");
            notificacion.setFechaEnvio(LocalDateTime.now());
        } catch (Exception e) {
            notificacion.setEstado("FALLIDA");
            System.err.println("Error enviando email a " + destinatario + ": " + e.getMessage());
        }

        notificacionRepository.save(notificacion);
    }

    @Override
    public void enviarEmailConAdjuntos(String destinatario, String asunto, String mensaje, UUID referenciaTramiteId, byte[] adjuntoCertificado, byte[] adjuntoComprobante) {
        Notificacion notificacion = Notificacion.builder()
                .destinatario(destinatario)
                .canal("EMAIL")
                .asunto(asunto)
                .mensaje(mensaje)
                .estado("PENDIENTE")
                .referenciaTramiteId(referenciaTramiteId)
                .build();
        
        notificacion = notificacionRepository.save(notificacion);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(mensaje);
            helper.setFrom("no-reply@mpct.gob.pe");

            if (adjuntoCertificado != null && adjuntoCertificado.length > 0) {
                helper.addAttachment("Certificado.pdf", new org.springframework.core.io.ByteArrayResource(adjuntoCertificado));
            }
            if (adjuntoComprobante != null && adjuntoComprobante.length > 0) {
                helper.addAttachment("Comprobante.pdf", new org.springframework.core.io.ByteArrayResource(adjuntoComprobante));
            }

            mailSender.send(mimeMessage);

            notificacion.setEstado("ENVIADA");
            notificacion.setFechaEnvio(LocalDateTime.now());
        } catch (Exception e) {
            notificacion.setEstado("FALLIDA");
            System.err.println("Error enviando email con adjuntos a " + destinatario + ": " + e.getMessage());
        }

        notificacionRepository.save(notificacion);
    }
}
