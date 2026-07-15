package com.example.mpct.service;

import com.example.mpct.dto.auth.ChangePasswordRequest;
import com.example.mpct.model.entity.User;
import com.example.mpct.repository.UserRepository;
import com.example.mpct.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void testChangePassword_IncorrectCurrentPassword() {
        User user = new User();
        user.setEmail("admin@mpct.gob.pe");
        user.setPasswordHash("hashed123");

        when(userRepository.findByEmail("admin@mpct.gob.pe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed123")).thenReturn(false);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword("admin@mpct.gob.pe", new ChangePasswordRequest("wrong", "newpass123"));
        });

        assertTrue(exception.getMessage().contains("contraseña actual es incorrecta"));
    }

    @Test
    void testChangePassword_NewPasswordTooShort() {
        User user = new User();
        user.setEmail("admin@mpct.gob.pe");
        user.setPasswordHash("hashed123");

        when(userRepository.findByEmail("admin@mpct.gob.pe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed123")).thenReturn(true);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword("admin@mpct.gob.pe", new ChangePasswordRequest("correct", "1234567"));
        });

        assertTrue(exception.getMessage().contains("al menos 8 caracteres"));
    }

    @Test
    void testChangePassword_Success() {
        User user = new User();
        user.setEmail("admin@mpct.gob.pe");
        user.setPasswordHash("hashed123");

        when(userRepository.findByEmail("admin@mpct.gob.pe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed123")).thenReturn(true);
        when(passwordEncoder.encode("newpass123")).thenReturn("newHashed");

        authService.changePassword("admin@mpct.gob.pe", new ChangePasswordRequest("correct", "newpass123"));

        verify(userRepository, times(1)).save(argThat(u -> u.getPasswordHash().equals("newHashed")));
    }
}
