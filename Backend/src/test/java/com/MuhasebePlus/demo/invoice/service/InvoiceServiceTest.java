package com.MuhasebePlus.demo.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {
    
    @Mock 
    InvoiceRepository invoiceRepository;

    @InjectMocks 
    InvoiceService invoiceService;

    private Invoice sampleInvoice;
    private Invoice sampleInvoice2;
    private Invoice sampleInvoice3;
    private InvoiceRequestDto sampleInvoiceRequestDto;

    @BeforeEach
    void setUp() {
        sampleInvoice = new Invoice();
        sampleInvoice.setInvoiceId(1L);
        sampleInvoice.setInvoiceNumber("INV-001");
        sampleInvoice.setCustomerId(10L);
        sampleInvoice.setInvoiceType(InvoiceType.sale);
        sampleInvoice.setDueDate(LocalDate.of(2026, 6, 1));
        sampleInvoice.setPaymentStatus(PaymentStatus.pending);
        sampleInvoice.setSubtotal(new BigDecimal("100.00"));
        sampleInvoice.setVatAmount(new BigDecimal("20.00"));
        sampleInvoice.setTotalAmount(new BigDecimal("120.00"));

        sampleInvoice2 = new Invoice();
        sampleInvoice2.setInvoiceId(2L);
        sampleInvoice2.setInvoiceNumber("INV-002");
        sampleInvoice2.setCustomerId(10L);
        sampleInvoice2.setInvoiceType(InvoiceType.purchase);
        sampleInvoice2.setDueDate(LocalDate.of(2026, 7, 1));
        sampleInvoice2.setPaymentStatus(PaymentStatus.paid);
        sampleInvoice2.setSubtotal(new BigDecimal("200.00"));
        sampleInvoice2.setVatAmount(new BigDecimal("40.00"));
        sampleInvoice2.setTotalAmount(new BigDecimal("240.00"));

        sampleInvoice3 = new Invoice();
        sampleInvoice3.setInvoiceId(3L);
        sampleInvoice3.setInvoiceNumber("INV-003");
        sampleInvoice3.setCustomerId(20L);
        sampleInvoice3.setInvoiceType(InvoiceType.sale);
        sampleInvoice3.setDueDate(LocalDate.of(2026, 8, 1));
        sampleInvoice3.setPaymentStatus(PaymentStatus.overdue);
        sampleInvoice3.setSubtotal(new BigDecimal("300.00"));
        sampleInvoice3.setVatAmount(new BigDecimal("60.00"));
        sampleInvoice3.setTotalAmount(new BigDecimal("360.00"));

        sampleInvoiceRequestDto = new InvoiceRequestDto("INV-001", 10L, InvoiceType.sale, LocalDate.of(2026, 6, 1), 
        PaymentStatus.pending, new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00"));

    }

    @Test
    void createInvoice_Success(){
        // Arrange
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sampleInvoice);

        // Act

        InvoiceResponseDto result = invoiceService.createInvoice(sampleInvoiceRequestDto);

        // Assert

        assertThat(result).isNotNull();
        assertThat(result.invoiceNumber()).isEqualTo("INV-001");
        assertThat(result.customerId()).isEqualTo(10L);
        assertThat(result.invoiceType()).isEqualTo("sale");
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(result.paymentStatus()).isEqualTo("pending");
        assertThat(result.subtotal()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.vatAmount()).isEqualTo(new BigDecimal("20.00"));
        assertThat(result.totalAmount()).isEqualTo(new BigDecimal("120.00"));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createInvoice_DuplicateInvoiceNumber() {
    // Arrange
    when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> invoiceService.createInvoice(sampleInvoiceRequestDto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("INV-001");

    verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void getAllInvoices_Success() {
        // Arrange
        when(invoiceRepository.findAll()).thenReturn(List.of(sampleInvoice, sampleInvoice2, sampleInvoice3));

        // Act
        List<InvoiceResponseDto> result = invoiceService.getAllInvoices();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).invoiceNumber()).isEqualTo("INV-001");
        assertThat(result.get(1).invoiceNumber()).isEqualTo("INV-002");
        assertThat(result.get(2).invoiceNumber()).isEqualTo("INV-003");
        verify(invoiceRepository).findAll();
    }
    @Test
    void getAllInvoices_EmptyList() {
        // Arrange
        when(invoiceRepository.findAll()).thenReturn(List.of());

        // Act
        List<InvoiceResponseDto> result = invoiceService.getAllInvoices();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(invoiceRepository).findAll();
    }
    @Test
    void getInvoiceById_Success() {
        // Arrange
        when(invoiceRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleInvoice));

        // Act
        InvoiceResponseDto result = invoiceService.getInvoiceById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.invoiceNumber()).isEqualTo("INV-001");
        verify(invoiceRepository).findById(1L);
    }
    @Test
    void getInvoiceById_NotFound() {
        // Arrange
        when(invoiceRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.getInvoiceById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("999");

        verify(invoiceRepository).findById(999L);
    }
    @Test
    void updateInvoice_Success() {
        // Arrange
        sampleInvoice.setPaymentStatus(PaymentStatus.paid);

        when(invoiceRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sampleInvoice);

        InvoiceRequestDto updateDto = new InvoiceRequestDto("INV-001", 10L, InvoiceType.sale, LocalDate.of(2026, 6, 1),
        PaymentStatus.paid, new BigDecimal("100.00"), new BigDecimal("20.00"), new BigDecimal("120.00"));

        // Act
        InvoiceResponseDto result = invoiceService.updateInvoice(1L, updateDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.paymentStatus()).isEqualTo("paid");
        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository).save(any(Invoice.class));
    }
    @Test
    void updateInvoice_NotFound() {
        // Arrange
        when(invoiceRepository.findById(999L)).thenReturn(java.util.Optional.empty());
        //Act & Assert
        assertThatThrownBy(() -> invoiceService.updateInvoice(999L, sampleInvoiceRequestDto))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("999");
        verify(invoiceRepository).findById(999L);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }
    @Test
    void deleteInvoiceById_Success() {
        // Arrange
        when(invoiceRepository.existsById(1L)).thenReturn(true);

        // Act
        invoiceService.deleteInvoiceById(1L);

        // Assert
        verify(invoiceRepository).existsById(1L);
        verify(invoiceRepository).deleteById(1L);
    }
    @Test
    void deleteInvoiceById_NotFound() {
        // Arrange
        when(invoiceRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.deleteInvoiceById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("999");

        verify(invoiceRepository).existsById(999L);
        verify(invoiceRepository, never()).deleteById(anyLong());
    }
    @Test
    void getInvoiceByCustomerId_Success() {
        // Arrange
        when(invoiceRepository.findByCustomerId(10L)).thenReturn(List.of(sampleInvoice, sampleInvoice2));

        // Act
        List<InvoiceResponseDto> result = invoiceService.getInvoiceByCustomerId(10L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).customerId()).isEqualTo(10L);
        assertThat(result.get(1).customerId()).isEqualTo(10L);
        verify(invoiceRepository).findByCustomerId(10L);
    }
    @Test
    void getInvoiceByCustomerId_NotFound() {
        // Arrange
        when(invoiceRepository.findByCustomerId(999L)).thenReturn(List.of());

        // Act
        List<InvoiceResponseDto> result = invoiceService.getInvoiceByCustomerId(999L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(invoiceRepository).findByCustomerId(999L);
    }
    @Test
    void getInvoiceByFilters_Success() {
        // Arrange
        when(invoiceRepository.findByPaymentStatusAndInvoiceType(PaymentStatus.pending, InvoiceType.sale))
            .thenReturn(List.of(sampleInvoice));

        // Act
        List<InvoiceResponseDto> result = invoiceService.getInvoiceByFilters(PaymentStatus.pending, InvoiceType.sale);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).invoiceNumber()).isEqualTo("INV-001");
        verify(invoiceRepository).findByPaymentStatusAndInvoiceType(PaymentStatus.pending, InvoiceType.sale);
    }
    @Test
    void getInvoiceByFilters_BothNull_ShouldReturnAll(){
        // Arrange
        when(invoiceRepository.findAll()).thenReturn(List.of(sampleInvoice, sampleInvoice2, sampleInvoice3));

        // Act
        List<InvoiceResponseDto> result = invoiceService.getInvoiceByFilters(null, null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        verify(invoiceRepository).findAll();
    }

    @Test
    void getInvoiceByFilters_OnlyPaymentStatus_ShouldReturnFiltered() {
        // Arrange
        when(invoiceRepository.findByPaymentStatus(PaymentStatus.pending))
            .thenReturn(List.of(sampleInvoice));

        // Act
        List<InvoiceResponseDto> result = invoiceService.getInvoiceByFilters(PaymentStatus.pending, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).paymentStatus()).isEqualTo("pending");
        verify(invoiceRepository).findByPaymentStatus(PaymentStatus.pending);
    }

    @Test
    void getInvoiceByFilters_OnlyInvoiceType_ShouldReturnFiltered() {
        // Arrange
        when(invoiceRepository.findByInvoiceType(InvoiceType.sale))
            .thenReturn(List.of(sampleInvoice, sampleInvoice3));

        // Act
        List<InvoiceResponseDto> result = invoiceService.getInvoiceByFilters(null, InvoiceType.sale);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).invoiceType()).isEqualTo("sale");
        assertThat(result.get(1).invoiceType()).isEqualTo("sale");
        verify(invoiceRepository).findByInvoiceType(InvoiceType.sale);
    }

    @Test
    void updatePaymentStatus_Success() {
        // Arrange
        sampleInvoice.setPaymentStatus(PaymentStatus.paid);

        when(invoiceRepository.findById(1L)).thenReturn(java.util.Optional.of(sampleInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(sampleInvoice);

        // Act
        InvoiceResponseDto result = invoiceService.updatePaymentStatus(1L, PaymentStatus.paid);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.paymentStatus()).isEqualTo("paid");
        verify(invoiceRepository).findById(1L);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void updatePaymentStatus_NotFound() {
        // Arrange
        when(invoiceRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> invoiceService.updatePaymentStatus(999L, PaymentStatus.paid))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("999");

        verify(invoiceRepository).findById(999L);
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }
}
