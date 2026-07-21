package com.MuhasebePlus.demo.sharelink.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.MuhasebePlus.demo.invoice.entity.InvoiceType;

public record PublicShareSummaryDto(
        List<TypeSummary> summaries
) {
    public record TypeSummary(
            InvoiceType invoiceType,
            long count,
            BigDecimal subtotal,
            BigDecimal vatAmount,
            BigDecimal totalAmount
    ) {}
}
