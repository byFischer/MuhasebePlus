package com.MuhasebePlus.demo.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.NewProductRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceLineItemResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.stock.entity.MovementType;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.service.StockMovementService;
import com.MuhasebePlus.demo.stock.service.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService implements HardDeletable {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final ProductService productService;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final SystemLogService systemLogService;


    //PUBLIC METODLAR - CRUD

    public InvoiceResponseDto createInvoice(InvoiceRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();

        if (invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId(dto.invoiceNumber(), companyId)) {
            throw new RuntimeException("This invoice number is already used in your company: " + dto.invoiceNumber());
        }

        Customer customer = validateCustomer(dto.customerId());
        Map<Integer, Product> productMap = resolveProducts(dto.lineItems(), dto.invoiceType());

        Invoice invoice = new Invoice();
        invoice.setCompany(companyRepository.getReferenceById(companyId));
        invoice.setInvoiceNumber(dto.invoiceNumber());
        invoice.setCustomerId(dto.customerId());
        invoice.setInvoiceType(dto.invoiceType());
        invoice.setDueDate(dto.dueDate());
        invoice.setPaymentStatus(PaymentStatus.pending);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        List<InvoiceLineItem> savedLineItems = saveLineItems(savedInvoice.getInvoiceId(), dto.lineItems(), productMap);
        applyTotals(savedInvoice, savedLineItems);
        Invoice finalInvoice = invoiceRepository.save(savedInvoice);

        for (InvoiceLineItemRequestDto item : dto.lineItems()) {
            Integer resolvedProductId = item.productId() != null ? item.productId()
                    : productMap.values().stream()
                            .filter(p -> p.getBarcode().equals(item.newProduct().barcode()))
                            .findFirst().map(Product::getProductId).orElse(null);
            if (resolvedProductId == null) continue;

            if (dto.invoiceType() == InvoiceType.sale) {
                stockMovementService.recordMovement(resolvedProductId, -item.quantity(),
                        MovementType.SALE, "INVOICE", savedInvoice.getInvoiceId(), null, null);
            } else if (dto.invoiceType() == InvoiceType.purchase) {
                Product p = productMap.get(resolvedProductId);
                stockMovementService.recordMovement(resolvedProductId, item.quantity(),
                        MovementType.PURCHASE, "INVOICE", savedInvoice.getInvoiceId(),
                        p != null ? p.getCostPrice() : null, null);
            }
        }

        systemLogService.log(LogLevel.INFO, "Fatura oluşturuldu: " + dto.invoiceNumber());
        return toResponseDto(finalInvoice, customer, savedLineItems, productMap);
    }

    public InvoiceResponseDto confirmInvoice(Long invoiceId) {
        Invoice invoice = findActiveInvoiceById(invoiceId);
        Long companyId = companyContext.getCurrentCompanyId();

        if (invoice.getPaymentStatus() != PaymentStatus.draft) {
            throw new RuntimeException("Only draft invoices can be confirmed. Current status: "
                + invoice.getPaymentStatus());
        }

        List<InvoiceLineItem> lineItems = lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoiceId, companyId);
        if (lineItems.isEmpty()) {
            throw new RuntimeException("Draft invoice has no line items: " + invoiceId);
        }

        if (invoice.getInvoiceType() == InvoiceType.sale) {
            for (InvoiceLineItem li : lineItems) {
                stockMovementService.recordMovement(li.getProductId(), -li.getQuantity(),
                        MovementType.SALE, "INVOICE", invoiceId, null, null);
            }
        }

        invoice.setPaymentStatus(PaymentStatus.pending);
        Invoice confirmed = invoiceRepository.save(invoice);

        systemLogService.log(LogLevel.INFO, "Taslak fatura onaylandı: " + invoice.getInvoiceNumber());
        Map<Integer, Product> productMap = batchLoadProducts(List.of(lineItems), companyId);
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyId(invoice.getCustomerId(), companyId).orElse(null);
        return toResponseDto(confirmed, customer, lineItems, productMap);
    }

    public List<InvoiceResponseDto> getAllInvoices() {
        Long companyId = companyContext.getCurrentCompanyId();

        List<Invoice> invoices = invoiceRepository.findAllActiveWithCustomer(companyId);
        Map<Long, List<InvoiceLineItem>> linesByInvoice = batchLoadLineItems(invoiceIds(invoices), companyId);
        Map<Integer, Product> productMap = batchLoadProducts(linesByInvoice.values(), companyId);

        return invoices.stream()
            .map(inv -> toResponseDto(inv, inv.getCustomer(),
                                      linesByInvoice.getOrDefault(inv.getInvoiceId(), List.of()),
                                      productMap))
            .toList();
    }

    public InvoiceResponseDto getInvoiceById(Long invoiceId) {
        Invoice invoice = findActiveInvoiceById(invoiceId);
        return toResponseDtoWithLines(invoice);
    }

    public InvoiceResponseDto updateInvoice(Long invoiceId, InvoiceRequestDto dto) {
        Invoice invoice = findActiveInvoiceById(invoiceId);
        Long companyId = companyContext.getCurrentCompanyId();

        if (invoice.getPaymentStatus() == PaymentStatus.paid) {
            throw new RuntimeException("Paid invoices cannot be updated: " + invoiceId);
        }

        if (!invoice.getInvoiceNumber().equals(dto.invoiceNumber())
                && invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId(dto.invoiceNumber(), companyId)) {
            throw new RuntimeException("This invoice number is already used in your company: " + dto.invoiceNumber());
        }

        validateCustomer(dto.customerId());

        invoice.setInvoiceNumber(dto.invoiceNumber());
        invoice.setCustomerId(dto.customerId());
        invoice.setInvoiceType(dto.invoiceType());
        invoice.setDueDate(dto.dueDate());

        Invoice updated = invoiceRepository.save(invoice);
        return toResponseDtoWithLines(updated);
    }

    public void deleteInvoice(Long invoiceId) {
        Invoice invoice = findActiveInvoiceById(invoiceId);

        if (invoice.getPaymentStatus() == PaymentStatus.paid) {
            throw new RuntimeException("Paid invoices cannot be deleted: " + invoiceId);
        }
        if (invoice.getPaymentStatus() == PaymentStatus.partially_paid) {
            throw new BusinessException("Üzerinde ödeme bulunan faturalar silinemez. Önce ödemeleri silin.");
        }

        Long companyId = companyContext.getCurrentCompanyId();
        List<InvoiceLineItem> lineItems = lineItemRepository
                .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoiceId, companyId);
        lineItems.forEach(li -> li.setDeleted(true));
        lineItemRepository.saveAll(lineItems);

        stockMovementService.recordReverseMovementsForInvoice(invoiceId);

        invoice.setDeleted(true);
        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        systemLogService.log(LogLevel.WARNING, "Fatura silindi: " + invoice.getInvoiceNumber());
    }

    public InvoiceResponseDto restoreInvoice(Long invoiceId) {
        Invoice invoice = findInvoiceById(invoiceId);

        if (!invoice.isDeleted()) {
            throw new RuntimeException("Invoice is not deleted: " + invoiceId);
        }

        invoice.setDeleted(false);
        invoice.setDeletedAt(null);
        Invoice restored = invoiceRepository.save(invoice);
        return toResponseDtoWithLines(restored);
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<Invoice> expired = invoiceRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (Invoice invoice : expired) {
            lineItemRepository.deleteByInvoiceId(invoice.getInvoiceId());
            invoiceRepository.delete(invoice);
        }
        return expired.size();
    }

    public List<InvoiceResponseDto> getInvoiceByCustomerId(Long customerId) {
        Long companyId = companyContext.getCurrentCompanyId();

        List<Invoice> invoices = invoiceRepository.findByCustomerIdWithCustomer(customerId, companyId);
        Map<Long, List<InvoiceLineItem>> linesByInvoice = batchLoadLineItems(invoiceIds(invoices), companyId);
        Map<Integer, Product> productMap = batchLoadProducts(linesByInvoice.values(), companyId);

        return invoices.stream()
            .map(inv -> toResponseDto(inv, inv.getCustomer(),
                                      linesByInvoice.getOrDefault(inv.getInvoiceId(), List.of()),
                                      productMap))
            .toList();
    }

    public List<InvoiceResponseDto> getInvoiceByFilters(PaymentStatus paymentStatus, InvoiceType invoiceType) {
        Long companyId = companyContext.getCurrentCompanyId();

        List<Invoice> invoices = invoiceRepository.findByFiltersWithCustomer(companyId, paymentStatus, invoiceType);
        Map<Long, List<InvoiceLineItem>> linesByInvoice = batchLoadLineItems(invoiceIds(invoices), companyId);
        Map<Integer, Product> productMap = batchLoadProducts(linesByInvoice.values(), companyId);

        return invoices.stream()
            .map(inv -> toResponseDto(inv, inv.getCustomer(),
                                      linesByInvoice.getOrDefault(inv.getInvoiceId(), List.of()),
                                      productMap))
            .toList();
    }

    public InvoiceResponseDto updatePaymentStatus(Long invoiceId, PaymentStatus paymentStatus) {
        Invoice invoice = findActiveInvoiceById(invoiceId);

        if (paymentStatus == PaymentStatus.draft) {
            throw new RuntimeException("Cannot set payment status to draft manually.");
        }
        if (paymentStatus == PaymentStatus.paid || paymentStatus == PaymentStatus.partially_paid) {
            throw new BusinessException("Use /payments endpoint to manage payment status");
        }
        if (invoice.getPaymentStatus() == PaymentStatus.draft && paymentStatus == PaymentStatus.pending) {
            throw new RuntimeException("Use /confirm endpoint to transition a draft invoice to pending.");
        }

        invoice.setPaymentStatus(paymentStatus);
        Invoice updated = invoiceRepository.save(invoice);
        return toResponseDtoWithLines(updated);
    }


    //PRIVATE METODLAR - VALIDATION

    private Customer validateCustomer(Long customerId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .orElseThrow(() -> new RuntimeException("Customer not found or inactive for your company: " + customerId));
    }

    private Map<Integer, Product> resolveProducts(List<InvoiceLineItemRequestDto> items, InvoiceType invoiceType) {
        Long companyId = companyContext.getCurrentCompanyId();
        Map<Integer, Product> map = new HashMap<>();
        Set<String> newBarcodes = new HashSet<>();

        for (InvoiceLineItemRequestDto item : items) {
            if (item.productId() != null) {
                if (map.containsKey(item.productId())) continue;
                Product product = productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(item.productId(), companyId)
                    .orElseThrow(() -> new RuntimeException("Product not found or inactive for your company: " + item.productId()));
                map.put(item.productId(), product);
            } else if (item.newProduct() != null) {
                if (invoiceType != InvoiceType.purchase) {
                    throw new BusinessException("Satış faturasında yeni ürün eklenemez");
                }
                NewProductRequestDto np = item.newProduct();
                if (newBarcodes.contains(np.barcode())) {
                    throw new BusinessException("Aynı request içinde aynı barcode tekrar edilemez: " + np.barcode());
                }
                if (productRepository.existsByBarcodeAndCompanyCompanyIdAndIsDeletedFalse(np.barcode(), companyId)) {
                    throw new BusinessException("Bu barcode zaten mevcut: " + np.barcode());
                }
                newBarcodes.add(np.barcode());

                Product newProduct = productService.createProductEntity(new com.MuhasebePlus.demo.stock.dto.request.ProductRequestDto(
                        np.barcode(), np.name(), np.description(), np.unit(),
                        np.salePrice(), np.vatRate(), np.costPrice(), 0, np.minQuantity()));
                map.put(newProduct.getProductId(), newProduct);
            }
        }
        return map;
    }



    //PRIVATE METODLAR - BATCH LOADING

    private List<Long> invoiceIds(List<Invoice> invoices) {
        return invoices.stream().map(Invoice::getInvoiceId).toList();
    }

    private Map<Long, List<InvoiceLineItem>> batchLoadLineItems(List<Long> ids, Long companyId) {
        if (ids.isEmpty()) return Map.of();
        return lineItemRepository.findByInvoiceIdInAndCompanyAndActive(ids, companyId)
            .stream().collect(Collectors.groupingBy(InvoiceLineItem::getInvoiceId));
    }

    private Map<Integer, Product> batchLoadProducts(Collection<List<InvoiceLineItem>> groups, Long companyId) {
        List<Integer> productIds = groups.stream()
            .flatMap(List::stream)
            .map(InvoiceLineItem::getProductId)
            .distinct()
            .toList();
        if (productIds.isEmpty()) return Map.of();
        return productRepository.findByProductIdInAndCompanyCompanyId(productIds, companyId)
            .stream().collect(Collectors.toMap(Product::getProductId, Function.identity()));
    }


    //PRIVATE METODLAR - LINE ITEM & TOPLAM HESAP

    private List<InvoiceLineItem> saveLineItems(
            Long invoiceId,
            List<InvoiceLineItemRequestDto> items,
            Map<Integer, Product> productMap) {

        Long companyId = companyContext.getCurrentCompanyId();
        List<InvoiceLineItem> saved = new ArrayList<>();

        for (InvoiceLineItemRequestDto req : items) {
            Integer pid = req.productId();
            if (pid == null && req.newProduct() != null) {
                pid = productMap.values().stream()
                        .filter(p -> p.getBarcode().equals(req.newProduct().barcode()))
                        .findFirst().map(Product::getProductId).orElse(null);
            }
            if (pid == null) continue;

            Product product = productMap.get(pid);
            if (product == null) continue;

            BigDecimal unitPrice = product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO;
            BigDecimal vatRate   = product.getVatRate()   != null ? product.getVatRate()   : BigDecimal.ZERO;
            BigDecimal quantity  = BigDecimal.valueOf(req.quantity());

            BigDecimal lineNet   = round2(quantity.multiply(unitPrice));
            BigDecimal lineVat   = round2(lineNet.multiply(vatRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal lineTotal = round2(lineNet.add(lineVat));

            InvoiceLineItem li = new InvoiceLineItem();
            li.setCompany(companyRepository.getReferenceById(companyId));
            li.setInvoiceId(invoiceId);
            li.setProductId(pid);
            li.setQuantity(req.quantity());
            li.setUnitPrice(unitPrice);
            li.setVatRate(vatRate);
            li.setLineTotal(lineTotal);
            li.setDeleted(false);

            saved.add(lineItemRepository.save(li));
        }
        return saved;
    }

    private void applyTotals(Invoice invoice, List<InvoiceLineItem> lineItems) {
        BigDecimal subtotal  = BigDecimal.ZERO;
        BigDecimal vatAmount = BigDecimal.ZERO;

        for (InvoiceLineItem li : lineItems) {
            BigDecimal quantity = BigDecimal.valueOf(li.getQuantity());
            BigDecimal net      = round2(quantity.multiply(li.getUnitPrice()));
            BigDecimal vat      = round2(net.multiply(li.getVatRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

            subtotal  = subtotal.add(net);
            vatAmount = vatAmount.add(vat);
        }

        invoice.setSubtotal(round2(subtotal));
        invoice.setVatAmount(round2(vatAmount));
        invoice.setTotalAmount(round2(subtotal.add(vatAmount)));
    }

    private BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }


    //PRIVATE METODLAR - LOOKUP & DTO MAPPING

    private Invoice findInvoiceById(Long invoiceId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));

        if (!invoice.getCompany().getCompanyId().equals(companyId)) {
            throw new RuntimeException("Bu kaydı görüntüleme yetkiniz yok");
        }

        return invoice;
    }

    private Invoice findActiveInvoiceById(Long invoiceId) {
        Invoice invoice = findInvoiceById(invoiceId);
        if (invoice.isDeleted()) {
            throw new RuntimeException("Invoice not found with id: " + invoiceId);
        }
        return invoice;
    }

    private InvoiceResponseDto toResponseDtoWithLines(Invoice invoice) {
        Long companyId = companyContext.getCurrentCompanyId();

        Customer customer = invoice.getCustomer() != null
            ? invoice.getCustomer()
            : customerRepository.findByCustomerIdAndCompanyCompanyId(invoice.getCustomerId(), companyId).orElse(null);

        List<InvoiceLineItem> lineItems = lineItemRepository
            .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoice.getInvoiceId(), companyId);

        Map<Integer, Product> productMap = batchLoadProducts(List.of(lineItems), companyId);

        return toResponseDto(invoice, customer, lineItems, productMap);
    }

    private InvoiceResponseDto toResponseDto(Invoice invoice, Customer customer,
                                              List<InvoiceLineItem> lineItems,
                                              Map<Integer, Product> productMap) {
        List<InvoiceLineItemResponseDto> lineItemDtos = lineItems.stream()
            .map(li -> toLineItemResponseDto(li, productMap))
            .toList();

        return new InvoiceResponseDto(
            invoice.getInvoiceId(),
            invoice.getInvoiceNumber(),
            invoice.getCustomerId(),
            customer != null ? customer.getName() : null,
            invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null,
            invoice.getDueDate(),
            invoice.getPaymentStatus() != null ? invoice.getPaymentStatus().name() : null,
            invoice.getSubtotal(),
            invoice.getVatAmount(),
            invoice.getTotalAmount(),
            lineItemDtos,
            invoice.getCreatedAt(),
            invoice.getUpdatedAt(),
            invoice.isDeleted()
        );
    }

    private InvoiceLineItemResponseDto toLineItemResponseDto(InvoiceLineItem li, Map<Integer, Product> productMap) {
        Product product = productMap.get(li.getProductId());

        return new InvoiceLineItemResponseDto(
            li.getLineItemId(),
            li.getProductId(),
            product != null ? product.getName() : null,
            product != null ? product.getBarcode() : null,
            li.getQuantity(),
            li.getUnitPrice(),
            li.getVatRate(),
            li.getLineTotal()
        );
    }
}
