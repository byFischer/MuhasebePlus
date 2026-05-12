package com.MuhasebePlus.demo.invoice.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceSeriesRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceSeriesResponseDto;
import com.MuhasebePlus.demo.invoice.entity.InvoiceSeries;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.service.InvoiceService;
import com.MuhasebePlus.demo.invoice.service.PdfInvoiceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final PdfInvoiceService pdfInvoiceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> createInvoice(@Valid @RequestBody InvoiceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createInvoice(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Page<InvoiceResponseDto>> getInvoices(
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) InvoiceType invoiceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "invoiceDate", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getInvoicesPaged(paymentStatus, invoiceType, startDate, endDate, search, pageable));
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

    @PutMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> cancelInvoice(
            @PathVariable Long invoiceId,
            @RequestParam(required = false, defaultValue = "İptal edildi") String reason) {
        return ResponseEntity.ok(invoiceService.cancelInvoice(invoiceId, reason));
    }

    @PostMapping("/{invoiceId}/return")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceResponseDto> createReturnInvoice(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createReturnInvoice(invoiceId, dto));
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

    @PutMapping("/{invoiceId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InvoiceResponseDto> restoreInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.restoreInvoice(invoiceId));
    }

    @GetMapping("/{invoiceId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long invoiceId) {
        byte[] pdf = pdfInvoiceService.generatePdf(invoiceId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"fatura-" + invoiceId + ".pdf\"")
                .body(pdf);
    }

    // INVOICE SERIES ENDPOINTS

    @PostMapping("/series")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<InvoiceSeriesResponseDto> createSeries(@Valid @RequestBody InvoiceSeriesRequestDto dto) {
        InvoiceSeries series = invoiceService.createSeries(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSeriesResponse(series));
    }

    @GetMapping("/series")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<InvoiceSeriesResponseDto>> getSeries() {
        List<InvoiceSeries> series = invoiceService.getSeries();
        return ResponseEntity.ok(series.stream().map(this::toSeriesResponse).toList());
    }

    @GetMapping("/series/next-number")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<String> getNextInvoiceNumber(
            @RequestParam InvoiceType invoiceType,
            @RequestParam String seriesCode) {
        return ResponseEntity.ok(invoiceService.assignNextInvoiceNumber(invoiceType, seriesCode));
    }

    // COLLECTION PROMISE ENDPOINTS

    @PostMapping("/{invoiceId}/promises")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<com.MuhasebePlus.demo.invoice.dto.response.CollectionPromiseResponseDto> createPromise(
            @PathVariable Long invoiceId,
            @Valid @RequestBody com.MuhasebePlus.demo.invoice.dto.request.CollectionPromiseRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.createCollectionPromise(invoiceId, dto));
    }

    @GetMapping("/{invoiceId}/promises")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<List<com.MuhasebePlus.demo.invoice.dto.response.CollectionPromiseResponseDto>> getPromises(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getCollectionPromises(invoiceId));
    }

    @PutMapping("/{invoiceId}/promises/{promiseId}/fulfill")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<Void> fulfillPromise(@PathVariable Long invoiceId, @PathVariable Long promiseId) {
        invoiceService.fulfillCollectionPromise(promiseId);
        return ResponseEntity.noContent().build();
    }

    private InvoiceSeriesResponseDto toSeriesResponse(InvoiceSeries s) {
        return new InvoiceSeriesResponseDto(
            s.getSeriesId(),
            s.getSeriesCode(),
            s.getInvoiceType().name(),
            s.getPrefix(),
            s.getYear(),
            s.getLastSequenceNumber(),
            s.isActive(),
            s.getCreatedAt(),
            s.getUpdatedAt()
        );
    }
}
