package com.MuhasebePlus.demo.sharelink.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;

public record PublicInvoiceDto(
        Long invoiceId,
        String invoiceNumber,
        String customerName,
        InvoiceType invoiceType,
        LocalDate invoiceDate,
        LocalDate dueDate,
        PaymentStatus paymentStatus,
        BigDecimal subtotal,
        BigDecimal vatAmount,
        BigDecimal totalAmount,
        Currency currency,
        boolean cancelled
) {}
