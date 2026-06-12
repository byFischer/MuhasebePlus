package com.MuhasebePlus.demo.sharelink.controller;

import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicInvoiceDto;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareInfoDto;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareSummaryDto;
import com.MuhasebePlus.demo.sharelink.dto.response.ShareLinkResponseDto;
import com.MuhasebePlus.demo.sharelink.service.PublicShareService;
import com.MuhasebePlus.demo.sharelink.service.ShareLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareLinkControllerUnitTest {

    @Mock private ShareLinkService shareLinkService;
    @Mock private PublicShareService publicShareService;

    @InjectMocks
    private ShareLinkController shareLinkController;

    @InjectMocks
    private PublicShareController publicShareController;

    @Test
    void shareLinkManagementEndpoints_delegateToService() {
        ShareLinkResponseDto current = new ShareLinkResponseDto(
                true, "token", true, LocalDateTime.of(2026, 1, 1, 10, 0), null, 2);
        ShareLinkResponseDto regenerated = new ShareLinkResponseDto(
                true, "new-token", true, LocalDateTime.of(2026, 1, 2, 10, 0), null, 0);
        when(shareLinkService.getCurrent()).thenReturn(current);
        when(shareLinkService.regenerate()).thenReturn(regenerated);

        assertThat(shareLinkController.getCurrent().getBody()).isSameAs(current);
        assertThat(shareLinkController.regenerate().getBody()).isSameAs(regenerated);
        assertThat(shareLinkController.revoke().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(shareLinkService).revoke();
    }

    @Test
    void publicShareEndpoints_delegateToServiceAndReturnPdfHeaders() {
        String token = "public-token";
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        PageRequest pageable = PageRequest.of(0, 20);
        PublicShareInfoDto info = new PublicShareInfoDto("Test A.S.");
        PublicInvoiceDto invoice = new PublicInvoiceDto(
                7L, "FTR-7", "ABC Ltd", InvoiceType.sale, start, end,
                PaymentStatus.pending, new BigDecimal("100.00"), new BigDecimal("20.00"),
                new BigDecimal("120.00"), null, false);
        PublicShareSummaryDto summary = new PublicShareSummaryDto(List.of(
                new PublicShareSummaryDto.TypeSummary(
                        InvoiceType.sale, 1L, new BigDecimal("100.00"),
                        new BigDecimal("20.00"), new BigDecimal("120.00"))));
        byte[] pdf = new byte[]{1, 2, 3};
        when(publicShareService.getInfo(token)).thenReturn(info);
        when(publicShareService.getInvoices(token, PaymentStatus.pending, InvoiceType.sale, start, end, "abc", pageable))
                .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));
        when(publicShareService.getSummary(token, start, end)).thenReturn(summary);
        when(publicShareService.getInvoicePdf(token, 7L)).thenReturn(pdf);

        assertThat(publicShareController.getInfo(token).getBody()).isSameAs(info);
        assertThat(publicShareController.getInvoices(token, PaymentStatus.pending, InvoiceType.sale, start, end, "abc", pageable)
                .getBody().getContent()).containsExactly(invoice);
        assertThat(publicShareController.getSummary(token, start, end).getBody()).isSameAs(summary);

        var pdfResponse = publicShareController.getInvoicePdf(token, 7L);
        assertThat(pdfResponse.getBody()).containsExactly(1, 2, 3);
        assertThat(pdfResponse.getHeaders().getFirst("Content-Type")).isEqualTo("application/pdf");
        assertThat(pdfResponse.getHeaders().getFirst("Content-Disposition"))
                .isEqualTo("inline; filename=\"fatura-7.pdf\"");
    }
}
