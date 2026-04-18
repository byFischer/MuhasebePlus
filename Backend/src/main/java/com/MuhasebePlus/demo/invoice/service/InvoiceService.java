package com.MuhasebePlus.demo.invoice.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

   /* @Autowired
    private CustomerRepository customerRepository;
   */ 

    // CRUD Operations
    public InvoiceResponseDto createInvoice(InvoiceRequestDto dto){
        if (invoiceRepository.existsByInvoiceNumber(dto.invoiceNumber())) {
            throw new RuntimeException("This invoice number is already used: " + dto.invoiceNumber());
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(dto.invoiceNumber());
        invoice.setCustomerId(dto.customerId());
        invoice.setInvoiceType(dto.invoiceType());
        invoice.setDueDate(dto.dueDate());
        invoice.setPaymentStatus(dto.paymentStatus());
        invoice.setSubtotal(dto.subtotal());
        invoice.setVatAmount(dto.vatAmount());
        invoice.setTotalAmount(dto.totalAmount());
        
        return toResponseDto(invoiceRepository.save(invoice));
    }

    public List<InvoiceResponseDto> getAllInvoices(){
        return invoiceRepository.findAll().stream()
            .map(this::toResponseDto)
            .collect(Collectors.toList());
    }

    public InvoiceResponseDto getInvoiceById(Long invoiceId) {
        return toResponseDto(findInvoiceById(invoiceId));
    }

    public InvoiceResponseDto updateInvoice(Long invoiceId, InvoiceRequestDto dto) {
        Invoice invoice = findInvoiceById(invoiceId);
            invoice.setInvoiceNumber(dto.invoiceNumber());
            invoice.setCustomerId(dto.customerId());
            invoice.setInvoiceType(dto.invoiceType());
            invoice.setDueDate(dto.dueDate());
            invoice.setPaymentStatus(dto.paymentStatus());
            invoice.setSubtotal(dto.subtotal());
            invoice.setVatAmount(dto.vatAmount());
            invoice.setTotalAmount(dto.totalAmount());

            return toResponseDto(invoiceRepository.save(invoice));
    }

    public void deleteInvoiceById(Long invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new RuntimeException("Invoice not found with id: " + invoiceId);
        }
        invoiceRepository.deleteById(invoiceId);
    }

    public List<InvoiceResponseDto> getInvoiceByCustomerId(Long customerId) {
        return invoiceRepository.findByCustomerId(customerId).stream()
            .map(this::toResponseDto)
            .collect(Collectors.toList());
    }
    public List<InvoiceResponseDto> getInvoiceByFilters(PaymentStatus paymentStatus, InvoiceType invoiceType) {
        if(paymentStatus != null && invoiceType != null){
            return invoiceRepository.findByPaymentStatusAndInvoiceType(paymentStatus, invoiceType).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        }
        else if(paymentStatus != null){
            return invoiceRepository.findByPaymentStatus(paymentStatus).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        }
        else if(invoiceType != null){
            return invoiceRepository.findByInvoiceType(invoiceType).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        }
        else{
            return getAllInvoices();
        }
    }
    public InvoiceResponseDto updatePaymentStatus(Long invoiceId, PaymentStatus paymentStatus) {
        Invoice invoice = findInvoiceById(invoiceId);
        invoice.setPaymentStatus(paymentStatus);
        return toResponseDto(invoiceRepository.save(invoice));
    }

// Helper methods
    private Invoice findInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
    }
    
    private InvoiceResponseDto toResponseDto(Invoice invoice) {
        return new InvoiceResponseDto(
            invoice.getInvoiceId(),
            invoice.getInvoiceNumber(),
            invoice.getCustomerId(),
            invoice.getInvoiceType().name(),
            invoice.getDueDate(),
            invoice.getPaymentStatus().name(),
            invoice.getSubtotal(),
            invoice.getVatAmount(),
            invoice.getTotalAmount()
        );
    }
}
