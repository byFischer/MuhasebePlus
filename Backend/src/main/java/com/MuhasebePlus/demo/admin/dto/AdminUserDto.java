package com.MuhasebePlus.demo.admin.dto;

import com.MuhasebePlus.demo.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUserDto(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String role,
        String phoneNumber,
        LocalDate birthDate,
        boolean isActive,
        boolean isLocked,
        int failedLoginAttempts,
        LocalDateTime createdAt,
        Long companyId,
        String companyName
) {

    public static AdminUserDto fromEntity(User user) {
        return new AdminUserDto(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getPhoneNumber(),
                user.getBirthDate(),
                user.isActive(),
                user.isLocked(),
                user.getFailedLoginAttempts(),
                user.getCreatedAt(),
                user.getCompany() != null ? user.getCompany().getCompanyId() : null,
                user.getCompany() != null ? user.getCompany().getCompanyName() : null
        );
    }
}
