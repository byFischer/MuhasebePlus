package com.MuhasebePlus.demo.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.stock.dto.response.StockCheckResultDto;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.service.StockService;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceLineItemRepository lineItemRepository;
    @Mock CustomerRepository customerRepository;
    @Mock ProductRepository productRepository;
    @Mock StockService stockService;

    @InjectMocks
    InvoiceService invoiceService;

    private Customer sampleCustomer;
    private Product sampleProduct;
    private Invoice savedInvoice;
    private InvoiceRequestDto validRequest;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer();
        sampleCustomer.setCustomerId(10L);
        sampleCustomer.setName("ACME Ltd.");
        sampleCustomer.setDeleted(false);

        sampleProduct = new Product();
        sampleProduct.setProductId(1);
        sampleProduct.setBarcode("BAR-1");
        sampleProduct.setName("Widget");
        sampleProduct.setSalePrice(new BigDecimal("10.00"));
        sampleProduct.setVatRate(new BigDecimal("20.00"));
        sampleProduct.setDeleted(false);

        savedInvoice = new Invoice();
        savedInvoice.setInvoiceId(1L);
        savedInvoice.setInvoiceNumber("INV-001");
        savedInvoice.setCustomerId(10L);
        savedInvoice.setInvoiceType(InvoiceType.sale);
        savedInvoice.setDueDate(LocalDate.of(2026, 6, 1));
        savedInvoice.setPaymentStatus(PaymentStatus.pending);

        validRequest = new InvoiceRequestDto(
            "INV-001",
            10L,
            InvoiceType.sale,
            LocalDate.of(2026, 6, 1),
            List.of(new InvoiceLineItemRequestDto(1, 10))
        );
    }

    @Test
    void createInvoice_SufficientStock_ShouldCreatePendingInvoice() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(false);
        when(customerRepository.findByCustomerIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findByProductIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleProduct));
        when(stockService.checkStock(anyList()))
            .thenReturn(List.of(new StockCheckResultDto(1, 10, 100, true)));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);
        when(lineItemRepository.save(any(InvoiceLineItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));

        InvoiceResponseDto result = invoiceService.createInvoice(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.invoiceNumber()).isEqualTo("INV-001");
        assertThat(result.paymentStatus()).isEqualTo("pending");
        assertThat(result.customerName()).isEqualTo("ACME Ltd.");
        assertThat(result.lineItems()).hasSize(1);
        verify(stockService).decreaseStock(anyList());
    }

    @Test
    void createInvoice_InsufficientStock_ShouldCreateDraftInvoice() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(false);
        when(customerRepository.findByCustomerIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findByProductIdAndIsDeletedFalse(1)).thenReturn(Optional.of(sampleProduct));
        when(stockService.checkStock(anyList()))
            .thenReturn(List.of(new StockCheckResultDto(1, 10, 5, false)));

        Invoice draftSaved = new Invoice();
        draftSaved.setInvoiceId(1L);
        draftSaved.setInvoiceNumber("INV-001");
        draftSaved.setCustomerId(10L);
        draftSaved.setInvoiceType(InvoiceType.sale);
        draftSaved.setDueDate(LocalDate.of(2026, 6, 1));
        draftSaved.setPaymentStatus(PaymentStatus.draft);

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(draftSaved);
        when(lineItemRepository.save(any(InvoiceLineItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));

        InvoiceResponseDto result = invoiceService.createInvoice(validRequest);

        assertThat(result.paymentStatus()).isEqualTo("draft");
        verify(stockService, never()).decreaseStock(anyList());
    }

    @Test
    void createInvoice_DuplicateInvoiceNumber_ShouldThrow() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createInvoice(validRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("INV-001");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void createInvoice_CustomerNotFound_ShouldThrow() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(false);
        when(customerRepository.findByCustomerIdAndIsDeletedFalse(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(validRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Customer");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void createInvoice_ProductNotFound_ShouldThrow() {
        when(invoiceRepository.existsByInvoiceNumber("INV-001")).thenReturn(false);
        when(customerRepository.findByCustomerIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findByProductIdAndIsDeletedFalse(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(validRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Product");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void confirmInvoice_Draft_SufficientStock_ShouldTransitionToPending() {
        Invoice draft = new Invoice();
        draft.setInvoiceId(1L);
        draft.setInvoiceNumber("INV-001");
        draft.setCustomerId(10L);
        draft.setInvoiceType(InvoiceType.sale);
        draft.setDueDate(LocalDate.of(2026, 6, 1));
        draft.setPaymentStatus(PaymentStatus.draft);

        InvoiceLineItem li = new InvoiceLineItem();
        li.setLineItemId(1);
        li.setInvoiceId(1L);
        li.setProductId(1);
        li.setQuantity(10);
        li.setUnitPrice(new BigDecimal("10.00"));
        li.setVatRate(new BigDecimal("20.00"));
        li.setLineTotal(new BigDecimal("120.00"));

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(lineItemRepository.findByInvoiceIdAndIsDeletedFalse(1L)).thenReturn(List.of(li));
        when(stockService.checkStock(anyList()))
            .thenReturn(List.of(new StockCheckResultDto(1, 10, 100, true)));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));

        InvoiceResponseDto result = invoiceService.confirmInvoice(1L);

        assertThat(result.paymentStatus()).isEqualTo("pending");
        verify(stockService).decreaseStock(anyList());
    }

    @Test
    void confirmInvoice_NotDraft_ShouldThrow() {
        Invoice pending = new Invoice();
        pending.setInvoiceId(1L);
        pending.setPaymentStatus(PaymentStatus.pending);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> invoiceService.confirmInvoice(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("draft");

        verify(stockService, never()).decreaseStock(anyList());
    }

    @Test
    void getInvoiceById_Success() {
        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(savedInvoice));
        when(lineItemRepository.findByInvoiceIdAndIsDeletedFalse(1L)).thenReturn(List.of());
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        InvoiceResponseDto result = invoiceService.getInvoiceById(1L);

        assertThat(result).isNotNull();
        assertThat(result.invoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void getInvoiceById_NotFound_ShouldThrow() {
        when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("999");
    }

    @Test
    void getAllInvoices_Success() {
        when(invoiceRepository.findAll()).thenReturn(List.of(savedInvoice));
        when(lineItemRepository.findByInvoiceIdAndIsDeletedFalse(1L)).thenReturn(List.of());
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        List<InvoiceResponseDto> result = invoiceService.getAllInvoices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).invoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void updateInvoice_Paid_ShouldThrow() {
        Invoice paid = new Invoice();
        paid.setInvoiceId(1L);
        paid.setInvoiceNumber("INV-001");
        paid.setPaymentStatus(PaymentStatus.paid);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> invoiceService.updateInvoice(1L, validRequest))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Paid");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void updateInvoice_Pending_ShouldUpdateHeader() {
        Invoice existing = new Invoice();
        existing.setInvoiceId(1L);
        existing.setInvoiceNumber("INV-001");
        existing.setCustomerId(10L);
        existing.setInvoiceType(InvoiceType.sale);
        existing.setDueDate(LocalDate.of(2026, 6, 1));
        existing.setPaymentStatus(PaymentStatus.pending);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.findByCustomerIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(sampleCustomer));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineItemRepository.findByInvoiceIdAndIsDeletedFalse(1L)).thenReturn(List.of());
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        InvoiceRequestDto updateDto = new InvoiceRequestDto(
            "INV-001", 10L, InvoiceType.sale, LocalDate.of(2026, 12, 1),
            List.of(new InvoiceLineItemRequestDto(1, 10))
        );

        InvoiceResponseDto result = invoiceService.updateInvoice(1L, updateDto);

        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void deleteInvoice_NotPaid_ShouldDelete() {
        Invoice pending = new Invoice();
        pending.setInvoiceId(1L);
        pending.setPaymentStatus(PaymentStatus.pending);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(pending));

        invoiceService.deleteInvoice(1L);

        verify(invoiceRepository).deleteById(1L);
    }

    @Test
    void deleteInvoice_Paid_ShouldThrow() {
        Invoice paid = new Invoice();
        paid.setInvoiceId(1L);
        paid.setPaymentStatus(PaymentStatus.paid);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> invoiceService.deleteInvoice(1L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Paid");

        verify(invoiceRepository, never()).deleteById(any());
    }

    @Test
    void updatePaymentStatus_Pending_ToPaid_ShouldWork() {
        Invoice pending = new Invoice();
        pending.setInvoiceId(1L);
        pending.setCustomerId(10L);
        pending.setPaymentStatus(PaymentStatus.pending);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lineItemRepository.findByInvoiceIdAndIsDeletedFalse(1L)).thenReturn(List.of());
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        InvoiceResponseDto result = invoiceService.updatePaymentStatus(1L, PaymentStatus.paid);

        assertThat(result.paymentStatus()).isEqualTo("paid");
    }

    @Test
    void updatePaymentStatus_ToDraftManually_ShouldThrow() {
        Invoice pending = new Invoice();
        pending.setInvoiceId(1L);
        pending.setPaymentStatus(PaymentStatus.pending);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> invoiceService.updatePaymentStatus(1L, PaymentStatus.draft))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("draft");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void updatePaymentStatus_DraftToPending_ShouldThrow() {
        Invoice draft = new Invoice();
        draft.setInvoiceId(1L);
        draft.setPaymentStatus(PaymentStatus.draft);

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> invoiceService.updatePaymentStatus(1L, PaymentStatus.pending))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("confirm");

        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void getInvoiceByFilters_BothNull_ShouldReturnAll() {
        when(invoiceRepository.findAll()).thenReturn(List.of(savedInvoice));
        when(lineItemRepository.findByInvoiceIdAndIsDeletedFalse(1L)).thenReturn(List.of());
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        List<InvoiceResponseDto> result = invoiceService.getInvoiceByFilters(null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getInvoiceByFilters_OnlyPaymentStatus_ShouldReturnFiltered() {
        when(invoiceRepository.findByPaymentStatus(PaymentStatus.pending)).thenReturn(List.of(savedInvoice));
        when(lineItemRepository.findByInvoiceIdAndIsDeletedFalse(1L)).thenReturn(List.of());
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        List<InvoiceResponseDto> result = invoiceService.getInvoiceByFilters(PaymentStatus.pending, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).paymentStatus()).isEqualTo("pending");
    }

    @Test
    void getInvoiceByCustomerId_ShouldReturnList() {
        when(invoiceRepository.findByCustomerId(10L)).thenReturn(List.of(savedInvoice));
        when(lineItemRepository.findByInvoiceIdAndIsDeletedFalse(1L)).thenReturn(List.of());
        when(customerRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));

        List<InvoiceResponseDto> result = invoiceService.getInvoiceByCustomerId(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).customerId()).isEqualTo(10L);
    }
}
