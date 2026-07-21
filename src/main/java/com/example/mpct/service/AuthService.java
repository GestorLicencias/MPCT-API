package com.example.mpct.service;

import com.example.mpct.dto.auth.AuthRequest;
import com.example.mpct.dto.auth.AuthResponse;
import com.example.mpct.dto.auth.ChangePasswordRequest;

public interface AuthService {
    AuthResponse login(AuthRequest request);
    void changePassword(String email, ChangePasswordRequest request);
    void forgotPassword(String email, String ip);
    void resetPassword(String token, String newPassword);
}
