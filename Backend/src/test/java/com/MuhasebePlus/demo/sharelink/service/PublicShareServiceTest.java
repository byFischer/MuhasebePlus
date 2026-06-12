package com.MuhasebePlus.demo.sharelink.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.invoice.service.PdfInvoiceService;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicInvoiceDto;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareInfoDto;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareSummaryDto;
import com.MuhasebePlus.demo.sharelink.entity.InvoiceShareLink;
import com.MuhasebePlus.demo.sharelink.exception.ShareLinkNotFoundException;
import com.MuhasebePlus.demo.sharelink.repository.InvoiceShareLinkRepository;

class PublicShareServiceTest {

    private InvoiceShareLinkRepository shareLinkRepository;
    private InvoiceRepository invoiceRepository;
    private PdfInvoiceService pdfInvoiceService;
    private PublicShareService publicShareService;

    private InvoiceShareLink activeLink;

    @BeforeEach
    void setUp() {
        shareLinkRepository = mock(InvoiceShareLinkRepository.class);
        invoiceRepository = mock(InvoiceRepository.class);
        pdfInvoiceService = mock(PdfInvoiceService.class);
        publicShareService = new PublicShareService(shareLinkRepository, invoiceRepository, pdfInvoiceService);

        Company company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.Ş.");
        activeLink = InvoiceShareLink.builder().id(5L).token("valid-token").isActive(true).company(company).build();
        when(shareLinkRepository.findByTokenAndIsActiveTrue("valid-token")).thenReturn(Optional.of(activeLink));
    }

    @Test
    void getInfoReturnsCompanyName() {
        PublicShareInfoDto info = publicShareService.getInfo("valid-token");
        assertEquals("Test A.Ş.", info.companyName());
    }

    @Test
    void unknownTokenThrowsNotFound() {
        when(shareLinkRepository.findByTokenAndIsActiveTrue("revoked")).thenReturn(Optional.empty());
        assertThrows(ShareLinkNotFoundException.class, () -> publicShareService.getInfo("revoked"));
        assertThrows(ShareLinkNotFoundException.class, () -> publicShareService.getInfo(null));
        assertThrows(ShareLinkNotFoundException.class, () -> publicShareService.getInfo(" "));
    }

    @Test
    void getInvoicesWhenSearchProvidedUsesSearchQueryAndRecordsAccess() {
        Pageable pageable = PageRequest.of(0, 20);
        Invoice invoice = invoice(11L, "FTR-11", InvoiceType.sale, PaymentStatus.pending, "ABC Ltd");
        when(invoiceRepository.searchPublicShareInvoices(1L, "abc", pageable))
                .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));

        Page<PublicInvoiceDto> result = publicShareService.getInvoices(
                "valid-token", null, null, null, null, "abc", pageable);

        assertEquals(1, result.getTotalElements());
        PublicInvoiceDto dto = result.getContent().get(0);
        assertEquals(11L, dto.invoiceId());
        assertEquals("FTR-11", dto.invoiceNumber());
        assertEquals("ABC Ltd", dto.customerName());
        assertEquals(InvoiceType.sale, dto.invoiceType());
        assertEquals(PaymentStatus.pending, dto.paymentStatus());
        assertEquals(Currency.TRY, dto.currency());
        verify(invoiceRepository, never()).findPublicShareInvoices(any(), any(), any(), any(), any(), any());
        verify(shareLinkRepository).recordAccess(eq(5L), any());
    }

    @Test
    void getInvoicesWhenSearchBlankUsesFilteredPublicShareQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Invoice invoice = invoice(12L, "FTR-12", InvoiceType.purchase, PaymentStatus.paid, null);
        when(invoiceRepository.findPublicShareInvoices(1L, PaymentStatus.paid, InvoiceType.purchase, start, end, pageable))
                .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));

        Page<PublicInvoiceDto> result = publicShareService.getInvoices(
                "valid-token", PaymentStatus.paid, InvoiceType.purchase, start, end, " ", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(null, result.getContent().get(0).customerName());
        assertEquals(true, result.getContent().get(0).cancelled());
        verify(invoiceRepository, never()).searchPublicShareInvoices(any(), any(), any());
        verify(shareLinkRepository).recordAccess(eq(5L), any());
    }

    @Test
    void getSummaryMapsRepositoryRowsToTypeSummaries() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        when(invoiceRepository.summarizePublicShareByType(1L, start, end)).thenReturn(List.of(
                new Object[]{InvoiceType.sale, 2L, new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00")},
                new Object[]{InvoiceType.purchase, 1L, new BigDecimal("50.00"), new BigDecimal("10.00"), new BigDecimal("60.00")}
        ));

        PublicShareSummaryDto result = publicShareService.getSummary("valid-token", start, end);

        assertEquals(2, result.summaries().size());
        assertEquals(InvoiceType.sale, result.summaries().get(0).invoiceType());
        assertEquals(2L, result.summaries().get(0).count());
        assertEquals(new BigDecimal("120.00"), result.summaries().get(0).totalAmount());
        assertEquals(InvoiceType.purchase, result.summaries().get(1).invoiceType());
    }

    @Test
    void pdfRejectsInvoiceOfAnotherCompany() {
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(ShareLinkNotFoundException.class, () -> publicShareService.getInvoicePdf("valid-token", 99L));
        verify(pdfInvoiceService, never()).generatePdf(any(), any());
    }

    @Test
    void pdfRejectsDraftInvoice() {
        Invoice draft = new Invoice();
        draft.setInvoiceId(7L);
        draft.setPaymentStatus(PaymentStatus.draft);
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(7L, 1L)).thenReturn(Optional.of(draft));

        assertThrows(ShareLinkNotFoundException.class, () -> publicShareService.getInvoicePdf("valid-token", 7L));
        verify(pdfInvoiceService, never()).generatePdf(any(), any());
    }

    @Test
    void pdfRejectsDeletedInvoice() {
        Invoice deleted = new Invoice();
        deleted.setInvoiceId(7L);
        deleted.setPaymentStatus(PaymentStatus.pending);
        deleted.setDeleted(true);
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(7L, 1L)).thenReturn(Optional.of(deleted));

        assertThrows(ShareLinkNotFoundException.class, () -> publicShareService.getInvoicePdf("valid-token", 7L));
        verify(pdfInvoiceService, never()).generatePdf(any(), any());
    }

    @Test
    void pdfGeneratedForOwnedInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(7L);
        invoice.setPaymentStatus(PaymentStatus.pending);
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(7L, 1L)).thenReturn(Optional.of(invoice));
        when(pdfInvoiceService.generatePdf(eq(7L), eq(1L))).thenReturn(new byte[]{1, 2});

        assertArrayEquals(new byte[]{1, 2}, publicShareService.getInvoicePdf("valid-token", 7L));
    }

    private Invoice invoice(Long id, String number, InvoiceType type, PaymentStatus status, String customerName) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(id);
        invoice.setInvoiceNumber(number);
        invoice.setInvoiceType(type);
        invoice.setInvoiceDate(LocalDate.of(2026, 1, 10));
        invoice.setDueDate(LocalDate.of(2026, 1, 25));
        invoice.setPaymentStatus(status);
        invoice.setSubtotal(new BigDecimal("100.00"));
        invoice.setVatAmount(new BigDecimal("20.00"));
        invoice.setTotalAmount(new BigDecimal("120.00"));
        invoice.setCurrency(Currency.TRY);
        invoice.setCancelled(customerName == null);
        if (customerName != null) {
            Customer customer = new Customer();
            customer.setName(customerName);
            invoice.setCustomer(customer);
        }
        return invoice;
    }
}
