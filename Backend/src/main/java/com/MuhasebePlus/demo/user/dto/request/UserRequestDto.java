package com.MuhasebePlus.demo.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UserRequestDto(

        @NotBlank(message = "First name cannot be blank")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        String lastName,

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Phone number cannot be blank")
        String phoneNumber,

        @NotNull(message = "Birth date cannot be null")
        LocalDate birthDate,

        @NotBlank(message = "Company name cannot be blank")
        String companyName,

        @NotBlank(message = "Company tax number cannot be blank")
        String companyTaxNumber
) {
}
