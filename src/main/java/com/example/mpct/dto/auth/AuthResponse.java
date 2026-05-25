package com.example.mpct.dto.auth;

public record AuthResponse(
        String token,
        String email,
        String role
) {
}
