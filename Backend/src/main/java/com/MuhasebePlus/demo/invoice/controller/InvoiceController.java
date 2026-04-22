package com.MuhasebePlus.demo.invoice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.service.InvoiceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> createInvoice(@Valid @RequestBody InvoiceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InvoiceResponseDto>> getInvoices(
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) InvoiceType invoiceType) {
        return ResponseEntity.ok(invoiceService.getInvoiceByFilters(paymentStatus, invoiceType));
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InvoiceResponseDto>> getInvoicesByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(invoiceService.getInvoiceByCustomerId(customerId));
    }

    @PutMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> updateInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceRequestDto dto) {
        return ResponseEntity.ok(invoiceService.updateInvoice(invoiceId, dto));
    }

    @PutMapping("/{invoiceId}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> confirmInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.confirmInvoice(invoiceId));
    }

    @PutMapping("/{invoiceId}/payment-status")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> updatePaymentStatus(
            @PathVariable Long invoiceId,
            @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(invoiceService.updatePaymentStatus(invoiceId, status));
    }

    @DeleteMapping("/{invoiceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long invoiceId) {
        invoiceService.deleteInvoice(invoiceId);
        return ResponseEntity.noContent().build();
    }
}
