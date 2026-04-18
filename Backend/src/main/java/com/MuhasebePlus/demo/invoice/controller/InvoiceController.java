package com.MuhasebePlus.demo.invoice.controller;

import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> createInvoice(@Valid @RequestBody InvoiceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InvoiceResponseDto>> getAllInvoices(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) InvoiceType type) {
        return ResponseEntity.ok(invoiceService.getInvoiceByFilters(status, type));
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceId));
    }

    @PutMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> updateInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceRequestDto dto) {
        return ResponseEntity.ok(invoiceService.updateInvoice(invoiceId, dto));
    }

    @DeleteMapping("/{invoiceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long invoiceId) {
        invoiceService.deleteInvoiceById(invoiceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InvoiceResponseDto>> getInvoicesByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(invoiceService.getInvoiceByCustomerId(customerId));
    }

    @PutMapping("/{invoiceId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> updatePaymentStatus(
            @PathVariable Long invoiceId,
            @RequestParam PaymentStatus status) {
        return ResponseEntity.ok(invoiceService.updatePaymentStatus(invoiceId, status));
    }
}
