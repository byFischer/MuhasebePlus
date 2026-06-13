package com.MuhasebePlus.demo.invoice;

import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.NewProductRequestDto;
import com.MuhasebePlus.demo.invoice.mapper.InvoiceMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceDtoMapperTest {

    @Test
    void lineItemRequestValidatesEitherExistingProductOrNewProductOnly() {
        NewProductRequestDto newProduct = new NewProductRequestDto(
                "BRC-1", "Yeni Urun", "Aciklama", "ADET",
                new BigDecimal("100.00"), new BigDecimal("20.00"),
                new BigDecimal("60.00"), 1);

        InvoiceLineItemRequestDto existingProduct = new InvoiceLineItemRequestDto(
                1, 2, null, new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
        InvoiceLineItemRequestDto createdProduct = new InvoiceLineItemRequestDto(
                null, 2, newProduct, new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
        InvoiceLineItemRequestDto both = new InvoiceLineItemRequestDto(
                1, 2, newProduct, new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);
        InvoiceLineItemRequestDto neither = new InvoiceLineItemRequestDto(
                null, 2, null, new BigDecimal("100.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, null, null);

        assertThat(existingProduct.isValidProductReference()).isTrue();
        assertThat(createdProduct.isValidProductReference()).isTrue();
        assertThat(both.isValidProductReference()).isFalse();
        assertThat(neither.isValidProductReference()).isFalse();
        assertThat(new InvoiceMapper()).isNotNull();
    }
}
