package com.MuhasebePlus.demo.sharelink;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicInvoiceDto;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareInfoDto;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareSummaryDto;
import com.MuhasebePlus.demo.sharelink.dto.response.ShareLinkResponseDto;
import com.MuhasebePlus.demo.sharelink.entity.InvoiceShareLink;
import com.MuhasebePlus.demo.sharelink.exception.ShareLinkNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShareLinkDtoEntityTest {

    @Test
    void responseDtosExposeValuesAndEmptyShareLinkState() {
        LocalDate invoiceDate = LocalDate.of(2026, 1, 10);
        PublicInvoiceDto invoice = new PublicInvoiceDto(
                1L, "FTR-1", "ABC Ltd", InvoiceType.sale, invoiceDate, invoiceDate.plusDays(15),
                PaymentStatus.pending, new BigDecimal("100.00"), new BigDecimal("20.00"),
                new BigDecimal("120.00"), Currency.TRY, false);
        PublicShareInfoDto info = new PublicShareInfoDto("Test A.S.");
        PublicShareSummaryDto.TypeSummary typeSummary = new PublicShareSummaryDto.TypeSummary(
                InvoiceType.sale, 2L, new BigDecimal("200.00"),
                new BigDecimal("40.00"), new BigDecimal("240.00"));
        PublicShareSummaryDto summary = new PublicShareSummaryDto(List.of(typeSummary));
        ShareLinkResponseDto empty = ShareLinkResponseDto.empty();

        assertThat(invoice.invoiceNumber()).isEqualTo("FTR-1");
        assertThat(invoice.currency()).isEqualTo(Currency.TRY);
        assertThat(info.companyName()).isEqualTo("Test A.S.");
        assertThat(summary.summaries()).containsExactly(typeSummary);
        assertThat(empty.exists()).isFalse();
        assertThat(empty.accessCount()).isZero();
    }

    @Test
    void invoiceShareLinkBuilderAndExceptionExposeExpectedValues() {
        Company company = new Company();
        company.setCompanyId(1L);
        LocalDateTime revokedAt = LocalDateTime.of(2026, 2, 1, 12, 0);

        InvoiceShareLink link = InvoiceShareLink.builder()
                .id(5L)
                .company(company)
                .token("token")
                .isActive(false)
                .createdBy(10L)
                .revokedAt(revokedAt)
                .lastAccessedAt(revokedAt.plusDays(1))
                .accessCount(3)
                .build();

        assertThat(link.getId()).isEqualTo(5L);
        assertThat(link.getCompany().getCompanyId()).isEqualTo(1L);
        assertThat(link.getToken()).isEqualTo("token");
        assertThat(link.isActive()).isFalse();
        assertThat(link.getCreatedBy()).isEqualTo(10L);
        assertThat(link.getRevokedAt()).isEqualTo(revokedAt);
        assertThat(link.getAccessCount()).isEqualTo(3);
        assertThat(new ShareLinkNotFoundException().getMessage()).contains("bulunamad");
    }
}
