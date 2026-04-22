package com.MuhasebePlus.demo.invoice.dto.response;

import java.math.BigDecimal;

public record InvoiceLineItemResponseDto(
    Integer lineItemId,
    Integer productId,
    String productName,
    String barcode,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal vatRate,
    BigDecimal lineTotal
) {
}
