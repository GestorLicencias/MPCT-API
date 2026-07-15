package com.example.mpct.service;

import com.example.mpct.dto.auth.AuthRequest;
import com.example.mpct.dto.auth.AuthResponse;

public interface AuthService {
    AuthResponse login(AuthRequest request);
    void changePassword(String email, com.example.mpct.dto.auth.ChangePasswordRequest request);
}
