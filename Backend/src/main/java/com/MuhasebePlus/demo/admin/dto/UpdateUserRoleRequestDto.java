package com.MuhasebePlus.demo.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequestDto(
        @NotBlank(message = "Rol boş olamaz")
        String role
) {
}
