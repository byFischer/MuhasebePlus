package com.MuhasebePlus.demo.sharelink.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicInvoiceDto;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareInfoDto;
import com.MuhasebePlus.demo.sharelink.dto.response.PublicShareSummaryDto;
import com.MuhasebePlus.demo.sharelink.service.PublicShareService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public/share")
@RequiredArgsConstructor
public class PublicShareController {

    private final PublicShareService publicShareService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicShareInfoDto> getInfo(@PathVariable String token) {
        return ResponseEntity.ok(publicShareService.getInfo(token));
    }

    @GetMapping("/{token}/invoices")
    public ResponseEntity<Page<PublicInvoiceDto>> getInvoices(
            @PathVariable String token,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) InvoiceType invoiceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "invoiceDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(publicShareService.getInvoices(token, paymentStatus, invoiceType, startDate, endDate, search, pageable));
    }

    @GetMapping("/{token}/summary")
    public ResponseEntity<PublicShareSummaryDto> getSummary(
            @PathVariable String token,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(publicShareService.getSummary(token, startDate, endDate));
    }

    @GetMapping("/{token}/invoices/{invoiceId}/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable String token, @PathVariable Long invoiceId) {
        byte[] pdf = publicShareService.getInvoicePdf(token, invoiceId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"fatura-" + invoiceId + ".pdf\"")
                .body(pdf);
    }
}
