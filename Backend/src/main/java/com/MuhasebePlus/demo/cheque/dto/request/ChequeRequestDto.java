package com.MuhasebePlus.demo.cheque.dto.request;

import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.financial.entity.Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ChequeRequestDto(
        @NotBlank(message = "Çek numarası zorunludur")
        @Size(max = 50)
        String chequeNumber,

        @NotNull(message = "Çek tipi zorunludur (RECEIVABLE/PAYABLE)")
        ChequeType chequeType,

        @NotNull(message = "Çek türü zorunludur (CHEQUE/PROMISSORY_NOTE)")
        ChequeKind chequeKind,

        Long customerId,

        @Size(max = 100)
        String drawerBank,

        @Size(max = 100)
        String drawerBranch,

        @Size(max = 50)
        String drawerAccountNumber,

        @NotNull(message = "Tutar zorunludur")
        @Positive(message = "Tutar pozitif olmalıdır")
        BigDecimal amount,

        Currency currency,

        @NotNull(message = "Düzenlenme tarihi zorunludur")
        LocalDate issueDate,

        @NotNull(message = "Vade tarihi zorunludur")
        LocalDate dueDate,

        @Size(max = 500)
        String notes
) {}
