package com.MuhasebePlus.demo.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateProfileRequestDto(

        @NotBlank(message = "First name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName,

        @NotBlank(message = "Phone number cannot be blank")
        String phoneNumber,

        @NotNull(message = "Birth date cannot be null")
        LocalDate birthDate
) {
}
