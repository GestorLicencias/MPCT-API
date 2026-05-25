package com.example.mpct.service.impl;

import com.example.mpct.service.*;

import com.example.mpct.dto.auth.AuthRequest;
import com.example.mpct.dto.auth.AuthResponse;
import com.example.mpct.dto.auth.RegisterRequest;
import com.example.mpct.dto.sunat.SunatRucResponse;
import com.example.mpct.model.enums.Role;
import com.example.mpct.model.entity.User;
import com.example.mpct.model.entity.UserProfile;
import com.example.mpct.repository.UserProfileRepository;
import com.example.mpct.repository.UserRepository;
import com.example.mpct.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final SunatScrapingService sunatScrapingService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        if (userProfileRepository.findByRuc(request.ruc()).isPresent()) {
            throw new RuntimeException("El RUC ya está registrado en el sistema");
        }

        // Validación web scraping a SUNAT
        SunatRucResponse sunatData = sunatScrapingService.validarRuc(request.ruc());

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.SOLICITANTE) // Por defecto es Solicitante
                .isActive(true)
                .build();
        
        user = userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .ruc(sunatData.ruc())
                .razonSocial(sunatData.razonSocial()) // Solo lectura, extraído de SUNAT
                .domicilioFiscal(sunatData.domicilioFiscal()) // Solo lectura, extraído de SUNAT
                .representanteLegal(request.representanteLegal())
                .build();
        
        userProfileRepository.save(profile);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }
}
