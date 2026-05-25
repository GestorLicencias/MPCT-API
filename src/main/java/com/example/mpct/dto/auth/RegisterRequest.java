package com.example.mpct.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password,

        @NotBlank(message = "El RUC es obligatorio")
        @Size(min = 11, max = 11, message = "El RUC debe tener 11 dígitos")
        String ruc,

        @NotBlank(message = "El representante legal es obligatorio")
        String representanteLegal
) {
}
