package com.example.mpct.service.impl;

import com.example.mpct.service.AuthService;

import com.example.mpct.dto.auth.AuthRequest;
import com.example.mpct.dto.auth.AuthResponse;
import com.example.mpct.model.entity.User;
import com.example.mpct.repository.UserRepository;
import com.example.mpct.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.example.mpct.repository.CajaRepository cajaRepository;
    private final com.example.mpct.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    private final com.example.mpct.service.NotificacionService notificacionService;

    private final java.util.concurrent.ConcurrentHashMap<String, io.github.bucket4j.Bucket> resetBuckets = new java.util.concurrent.ConcurrentHashMap<>();

    private io.github.bucket4j.Bucket getResetBucket(String ip) {
        return resetBuckets.computeIfAbsent(ip, k -> io.github.bucket4j.Bucket.builder()
                .addLimit(io.github.bucket4j.Bandwidth.classic(3, io.github.bucket4j.Refill.greedy(3, java.time.Duration.ofHours(1))))
                .build());
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al cifrar token", e);
        }
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        boolean cajaAbierta = false;
        if (user.getRole() == com.example.mpct.model.enums.Role.CAJERO) {
            cajaAbierta = cajaRepository.findByUsuarioIdAndEstado(user.getId(), com.example.mpct.model.enums.EstadoCaja.ABIERTA).isPresent();
        }

        return new AuthResponse(token, user.getEmail(), user.getRole().name(), cajaAbierta);
    }



    @Override
    public void changePassword(String email, com.example.mpct.dto.auth.ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        
        if (request.newPassword().length() < 8) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres");
        }
        
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void forgotPassword(String email, String ip) {
        if (!getResetBucket(ip).tryConsume(1)) {
            throw new RuntimeException("Demasiadas peticiones de recuperación. Intente más tarde.");
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUsuario(user);

            String rawToken = java.util.UUID.randomUUID().toString();
            String hashedToken = hashToken(rawToken);

            com.example.mpct.model.entity.PasswordResetToken resetToken = com.example.mpct.model.entity.PasswordResetToken.builder()
                    .tokenHash(hashedToken)
                    .usuario(user)
                    .fechaExpiracion(java.time.LocalDateTime.now().plusMinutes(30))
                    .build();

            passwordResetTokenRepository.save(resetToken);

            String resetLink = "http://localhost:3000/reset-password?token=" + rawToken;
            String mensaje = "Ha solicitado restablecer su contraseña. Haga clic en el siguiente enlace:\n\n" + resetLink + "\n\nEste enlace expirará en 30 minutos.";
            notificacionService.enviarEmail(user.getEmail(), "Recuperación de Contraseña", mensaje, null);
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (newPassword.length() < 8) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres");
        }

        String hashedToken = hashToken(rawToken);
        com.example.mpct.model.entity.PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new RuntimeException("Token inválido o expirado"));

        if (resetToken.getFechaExpiracion().isBefore(java.time.LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("El token ha expirado");
        }

        User user = resetToken.getUsuario();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        notificacionService.enviarEmail(user.getEmail(), "Contraseña Actualizada", "Su contraseña ha sido modificada exitosamente. Si no fue usted, contacte soporte de inmediato.", null);
    }
}
