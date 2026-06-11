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

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.invoice.service.PdfInvoiceService;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareInfoDto;
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
    void pdfGeneratedForOwnedInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(7L);
        invoice.setPaymentStatus(PaymentStatus.pending);
        when(invoiceRepository.findByInvoiceIdAndCompanyCompanyId(7L, 1L)).thenReturn(Optional.of(invoice));
        when(pdfInvoiceService.generatePdf(eq(7L), eq(1L))).thenReturn(new byte[]{1, 2});

        assertArrayEquals(new byte[]{1, 2}, publicShareService.getInvoicePdf("valid-token", 7L));
    }
}
