package com.MuhasebePlus.demo.cheque.dto.request;

import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * InvoicePaymentRequestDto içine gömülü optional alan.
 * paymentMethod=check seçildiğinde bu DTO doldurulmalıdır.
 */
public record ChequeDetailsDto(
        @NotBlank(message = "Çek numarası zorunludur")
        @Size(max = 50)
        String chequeNumber,

        @NotNull(message = "Çek türü zorunludur")
        ChequeKind chequeKind,

        @Size(max = 100)
        String drawerBank,

        @Size(max = 100)
        String drawerBranch,

        @Size(max = 50)
        String drawerAccountNumber,

        @NotNull(message = "Düzenlenme tarihi zorunludur")
        LocalDate issueDate,

        @NotNull(message = "Vade tarihi zorunludur")
        LocalDate dueDate
) {}
