package com.MuhasebePlus.demo.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(

        @NotBlank(message = "Current password cannot be blank")
        String currentPassword,

        @NotBlank(message = "New password cannot be blank")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword
) {
}
