package com.MuhasebePlus.demo.sharelink.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PublicShareService {

    private final InvoiceShareLinkRepository shareLinkRepository;
    private final InvoiceRepository invoiceRepository;
    private final PdfInvoiceService pdfInvoiceService;

    @Transactional(readOnly = true)
    public PublicShareInfoDto getInfo(String token) {
        InvoiceShareLink link = resolveActiveLink(token);
        return new PublicShareInfoDto(link.getCompany().getCompanyName());
    }

    public Page<PublicInvoiceDto> getInvoices(String token, PaymentStatus paymentStatus, InvoiceType invoiceType,
                                              LocalDate startDate, LocalDate endDate,
                                              String search, Pageable pageable) {
        InvoiceShareLink link = resolveActiveLink(token);
        Long companyId = link.getCompany().getCompanyId();

        Page<Invoice> page;
        if (search != null && !search.isBlank()) {
            page = invoiceRepository.searchPublicShareInvoices(companyId, search, pageable);
        } else {
            page = invoiceRepository.findPublicShareInvoices(companyId, paymentStatus, invoiceType, startDate, endDate, pageable);
        }

        shareLinkRepository.recordAccess(link.getId(), LocalDateTime.now());

        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public PublicShareSummaryDto getSummary(String token, LocalDate startDate, LocalDate endDate) {
        InvoiceShareLink link = resolveActiveLink(token);
        Long companyId = link.getCompany().getCompanyId();

        List<PublicShareSummaryDto.TypeSummary> summaries = invoiceRepository
                .summarizePublicShareByType(companyId, startDate, endDate).stream()
                .map(row -> new PublicShareSummaryDto.TypeSummary(
                        (InvoiceType) row[0],
                        (Long) row[1],
                        (BigDecimal) row[2],
                        (BigDecimal) row[3],
                        (BigDecimal) row[4]))
                .toList();

        return new PublicShareSummaryDto(summaries);
    }

    @Transactional(readOnly = true)
    public byte[] getInvoicePdf(String token, Long invoiceId) {
        InvoiceShareLink link = resolveActiveLink(token);
        Long companyId = link.getCompany().getCompanyId();

        Invoice invoice = invoiceRepository.findByInvoiceIdAndCompanyCompanyId(invoiceId, companyId)
                .filter(inv -> !inv.isDeleted() && inv.getPaymentStatus() != PaymentStatus.draft)
                .orElseThrow(ShareLinkNotFoundException::new);

        return pdfInvoiceService.generatePdf(invoice.getInvoiceId(), companyId);
    }

    private InvoiceShareLink resolveActiveLink(String token) {
        if (token == null || token.isBlank()) {
            throw new ShareLinkNotFoundException();
        }
        return shareLinkRepository.findByTokenAndIsActiveTrue(token)
                .orElseThrow(ShareLinkNotFoundException::new);
    }

    private PublicInvoiceDto toDto(Invoice invoice) {
        return new PublicInvoiceDto(
                invoice.getInvoiceId(),
                invoice.getInvoiceNumber(),
                invoice.getCustomer() != null ? invoice.getCustomer().getName() : null,
                invoice.getInvoiceType(),
                invoice.getInvoiceDate(),
                invoice.getDueDate(),
                invoice.getPaymentStatus(),
                invoice.getSubtotal(),
                invoice.getVatAmount(),
                invoice.getTotalAmount(),
                invoice.getCurrency(),
                invoice.isCancelled()
        );
    }
}
