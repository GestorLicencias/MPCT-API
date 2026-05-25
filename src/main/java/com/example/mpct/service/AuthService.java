package com.example.mpct.service;

import com.example.mpct.dto.auth.AuthRequest;
import com.example.mpct.dto.auth.AuthResponse;
import com.example.mpct.dto.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
}
