package com.MuhasebePlus.demo.user.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponseDto(

        Long userId,
        String firstName,
        String lastName,
        String email,
        String role,
        String phoneNumber,
        LocalDate birthDate,
        LocalDateTime createdAt,
        Long companyId,
        String companyName
) {
}
