package com.MuhasebePlus.demo.customer.dto.request;

import com.MuhasebePlus.demo.customer.entity.CustomerType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequestDto(
        @NotBlank(message = "Name is required")
        @Size(max = 180)
        String name,

        @NotBlank(message = "Tax number is required")
        @Pattern(regexp = "\\d{10,11}", message = "Tax number must be 10 or 11 digits")
        String taxNumber,

        @Size(max = 255)
        String address,

        @Size(max = 100)
        String city,

        @Pattern(regexp = "^(\\+90)?[1-9][0-9]{9}$", message = "Phone number must be valid Turkish format")
        String phoneNumber,

        @NotNull(message = "Type is required")
        CustomerType type,

        @jakarta.validation.constraints.Email(message = "Email must be valid")
        @Size(max = 255)
        String email
) {
}
