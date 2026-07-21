package com.MuhasebePlus.demo.cheque.dto.response;

import com.MuhasebePlus.demo.cheque.entity.ChequeKind;
import com.MuhasebePlus.demo.cheque.entity.ChequeStatus;
import com.MuhasebePlus.demo.cheque.entity.ChequeType;
import com.MuhasebePlus.demo.financial.entity.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ChequeResponseDto(
        Long chequeId,
        String chequeNumber,
        ChequeType chequeType,
        ChequeKind chequeKind,
        Long customerId,
        String customerName,
        String drawerBank,
        String drawerBranch,
        String drawerAccountNumber,
        BigDecimal amount,
        Currency currency,
        LocalDate issueDate,
        LocalDate dueDate,
        ChequeStatus status,
        Long bankAccountId,
        Long transactionId,
        Long invoicePaymentId,
        Long endorsedToCustomerId,
        String notes,
        List<ChequeMovementResponseDto> movements
) {}
