package com.MuhasebePlus.demo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequestDto(
        @NotBlank(message = "Yeni şifre boş olamaz")
        @Size(min = 8, max = 100, message = "Şifre en az 8 karakter olmalıdır")
        String newPassword
) {
}
