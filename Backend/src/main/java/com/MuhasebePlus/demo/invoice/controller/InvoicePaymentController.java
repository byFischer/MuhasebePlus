package com.MuhasebePlus.demo.invoice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.MuhasebePlus.demo.invoice.dto.request.InvoicePaymentRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoicePaymentResponseDto;
import com.MuhasebePlus.demo.invoice.service.InvoicePaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoicePaymentController {

    private final InvoicePaymentService invoicePaymentService;

    @PostMapping("/{invoiceId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoicePaymentResponseDto> createPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoicePaymentRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoicePaymentService.createPayment(invoiceId, dto));
    }

    @GetMapping("/{invoiceId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InvoicePaymentResponseDto>> getPayments(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoicePaymentService.getPaymentsByInvoiceId(invoiceId));
    }

    @DeleteMapping("/{invoiceId}/payments/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(
            @PathVariable Long invoiceId,
            @PathVariable Long paymentId) {
        invoicePaymentService.deletePayment(paymentId);
        return ResponseEntity.noContent().build();
    }
}
