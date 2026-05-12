package com.MuhasebePlus.demo.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerStatus;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.financial.entity.Currency;
import com.MuhasebePlus.demo.invoice.dto.request.CollectionPromiseRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.NewProductRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.CollectionPromiseResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceLineItemResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.CollectionPromise;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceSeries;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.CollectionPromiseRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceSeriesRepository;
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
    private final InvoiceSeriesRepository seriesRepository;
    private final CollectionPromiseRepository promiseRepository;


    // PUBLIC METODLAR - INVOICE CRUD

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
        invoice.setInvoiceDate(dto.invoiceDate() != null ? dto.invoiceDate() : LocalDate.now());
        invoice.setDueDate(dto.dueDate());
        invoice.setPaymentStatus(PaymentStatus.pending);
        invoice.setCancelled(false);

        invoice.setCurrency(dto.currency() != null ? dto.currency() : Currency.TRY);
        invoice.setExchangeRate(dto.exchangeRate() != null ? dto.exchangeRate() : BigDecimal.ONE);
        invoice.setDiscountType(dto.discountType());
        invoice.setDiscountAmount(dto.discountAmount() != null ? dto.discountAmount() : BigDecimal.ZERO);
        invoice.setWithholdingTaxAmount(BigDecimal.ZERO);
        invoice.setDescription(dto.description());
        invoice.setDeliveryAddress(dto.deliveryAddress());

        Invoice savedInvoice = invoiceRepository.save(invoice);

        List<InvoiceLineItem> savedLineItems = saveLineItems(savedInvoice.getInvoiceId(), dto.lineItems(), productMap);
        applyTotals(savedInvoice, savedLineItems);
        Invoice finalInvoice = invoiceRepository.save(savedInvoice);

        recordStockMovements(dto.lineItems(), savedInvoice, productMap);

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
        invoice.setInvoiceDate(dto.invoiceDate() != null ? dto.invoiceDate() : invoice.getInvoiceDate());
        invoice.setDueDate(dto.dueDate());
        invoice.setCurrency(dto.currency() != null ? dto.currency() : invoice.getCurrency());
        invoice.setExchangeRate(dto.exchangeRate() != null ? dto.exchangeRate() : invoice.getExchangeRate());
        invoice.setDiscountType(dto.discountType());
        invoice.setDiscountAmount(dto.discountAmount() != null ? dto.discountAmount() : invoice.getDiscountAmount());
        invoice.setDescription(dto.description());
        invoice.setDeliveryAddress(dto.deliveryAddress());

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

    public InvoiceResponseDto cancelInvoice(Long invoiceId, String reason) {
        Invoice invoice = findActiveInvoiceById(invoiceId);

        if (invoice.isCancelled()) {
            throw new BusinessException("Fatura zaten iptal edilmiş: " + invoiceId);
        }
        if (invoice.getPaymentStatus() == PaymentStatus.paid || invoice.getPaymentStatus() == PaymentStatus.partially_paid) {
            throw new BusinessException("Üzerinde ödeme bulunan faturalar iptal edilemez.");
        }

        stockMovementService.recordReverseMovementsForInvoice(invoiceId);

        invoice.setCancelled(true);
        invoice.setCancellationReason(reason);
        invoice.setCancelledAt(LocalDateTime.now());

        Long companyId = companyContext.getCurrentCompanyId();
        List<InvoiceLineItem> lineItems = lineItemRepository
                .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoiceId, companyId);
        lineItems.forEach(li -> li.setDeleted(true));
        lineItemRepository.saveAll(lineItems);

        Invoice cancelled = invoiceRepository.save(invoice);
        systemLogService.log(LogLevel.WARNING, "Fatura iptal edildi: " + cancelled.getInvoiceNumber() + " Sebep: " + reason);

        Map<Integer, Product> productMap = batchLoadProducts(List.of(lineItems), companyId);
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyId(cancelled.getCustomerId(), companyId).orElse(null);
        return toResponseDto(cancelled, customer, lineItems, productMap);
    }

    public InvoiceResponseDto createReturnInvoice(Long originalInvoiceId, InvoiceRequestDto returnDto) {
        Invoice original = findActiveInvoiceById(originalInvoiceId);
        Long companyId = companyContext.getCurrentCompanyId();

        if (original.isCancelled()) {
            throw new BusinessException("İptal edilmiş faturaya iade yapılamaz.");
        }

        InvoiceType returnType = original.getInvoiceType() == InvoiceType.sale ? InvoiceType.purchase : InvoiceType.sale;

        Invoice returnInvoice = new Invoice();
        returnInvoice.setCompany(companyRepository.getReferenceById(companyId));
        returnInvoice.setInvoiceNumber(returnDto.invoiceNumber());
        returnInvoice.setCustomerId(original.getCustomerId());
        returnInvoice.setInvoiceType(returnType);
        returnInvoice.setInvoiceDate(LocalDate.now());
        returnInvoice.setDueDate(LocalDate.now());
        returnInvoice.setPaymentStatus(PaymentStatus.pending);
        returnInvoice.setReferenceInvoiceId(originalInvoiceId);
        returnInvoice.setCancelled(false);
        returnInvoice.setCurrency(original.getCurrency());
        returnInvoice.setExchangeRate(original.getExchangeRate());
        returnInvoice.setDescription("İade faturası - Orijinal: " + original.getInvoiceNumber());
        returnInvoice.setDeliveryAddress(returnDto.deliveryAddress());

        List<InvoiceLineItem> originalLines = lineItemRepository
                .findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(originalInvoiceId, companyId);

        Invoice saved = invoiceRepository.save(returnInvoice);
        List<InvoiceLineItem> returnLines = new ArrayList<>();
        Map<Integer, Product> productMap = new HashMap<>();

        for (InvoiceLineItem ol : originalLines) {
            Product product = productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(ol.getProductId(), companyId).orElse(null);
            if (product == null) continue;
            productMap.put(ol.getProductId(), product);

            InvoiceLineItem rl = new InvoiceLineItem();
            rl.setCompany(companyRepository.getReferenceById(companyId));
            rl.setInvoiceId(saved.getInvoiceId());
            rl.setProductId(ol.getProductId());
            rl.setQuantity(ol.getQuantity());
            rl.setUnitPrice(ol.getUnitPrice());
            rl.setVatRate(ol.getVatRate());
            rl.setLineTotal(ol.getLineTotal());
            rl.setDiscountRate(ol.getDiscountRate());
            rl.setWithholdingTaxRate(ol.getWithholdingTaxRate());
            rl.setDeleted(false);
            returnLines.add(lineItemRepository.save(rl));

            MovementType mvt = returnType == InvoiceType.sale ? MovementType.SALE : MovementType.PURCHASE;
            int qty = returnType == InvoiceType.sale ? -ol.getQuantity() : ol.getQuantity();
            stockMovementService.recordMovement(ol.getProductId(), qty, mvt, "INVOICE", saved.getInvoiceId(), product.getCostPrice(), null);
        }

        applyTotals(saved, returnLines);
        Invoice finalInvoice = invoiceRepository.save(saved);

        systemLogService.log(LogLevel.INFO, "İade faturası oluşturuldu: " + finalInvoice.getInvoiceNumber());
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyId(finalInvoice.getCustomerId(), companyId).orElse(null);
        return toResponseDto(finalInvoice, customer, returnLines, productMap);
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


    // PUBLIC METODLAR - QUERIES

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

    public Page<InvoiceResponseDto> getInvoicesPaged(PaymentStatus paymentStatus, InvoiceType invoiceType,
                                                      LocalDate startDate, LocalDate endDate,
                                                      String search, Pageable pageable) {
        Long companyId = companyContext.getCurrentCompanyId();

        Page<Invoice> page;
        if (search != null && !search.isBlank()) {
            page = invoiceRepository.searchInvoices(companyId, search, pageable);
        } else {
            page = invoiceRepository.findByFiltersWithCustomerPage(companyId, paymentStatus, invoiceType, startDate, endDate, pageable);
        }

        List<Long> invoiceIds = page.getContent().stream().map(Invoice::getInvoiceId).toList();
        Map<Long, List<InvoiceLineItem>> linesByInvoice = batchLoadLineItems(invoiceIds, companyId);
        Map<Integer, Product> productMap = batchLoadProducts(linesByInvoice.values(), companyId);

        List<InvoiceResponseDto> dtos = page.getContent().stream()
            .map(inv -> toResponseDto(inv, inv.getCustomer(),
                                      linesByInvoice.getOrDefault(inv.getInvoiceId(), List.of()),
                                      productMap))
            .toList();

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
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


    // PUBLIC METODLAR - INVOICE SERIES

    public InvoiceSeries createSeries(com.MuhasebePlus.demo.invoice.dto.request.InvoiceSeriesRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();

        if (seriesRepository.existsByCompanyCompanyIdAndSeriesCodeAndInvoiceType(companyId, dto.seriesCode(), dto.invoiceType())) {
            throw new RuntimeException("A series with this code and type already exists.");
        }

        InvoiceSeries series = InvoiceSeries.builder()
            .company(companyRepository.getReferenceById(companyId))
            .seriesCode(dto.seriesCode())
            .invoiceType(dto.invoiceType())
            .prefix(dto.prefix())
            .year(dto.year())
            .lastSequenceNumber(0L)
            .isActive(true)
            .build();

        return seriesRepository.save(series);
    }

    public List<InvoiceSeries> getSeries() {
        Long companyId = companyContext.getCurrentCompanyId();
        return seriesRepository.findByCompanyCompanyIdAndIsActiveTrue(companyId);
    }

    public String assignNextInvoiceNumber(InvoiceType invoiceType, String seriesCode) {
        Long companyId = companyContext.getCurrentCompanyId();
        InvoiceSeries series = seriesRepository.findByCompanyCompanyIdAndSeriesCodeAndInvoiceType(companyId, seriesCode, invoiceType)
            .orElseThrow(() -> new RuntimeException("Active series not found: " + seriesCode));

        series.setLastSequenceNumber(series.getLastSequenceNumber() + 1);
        seriesRepository.save(series);

        String prefix = series.getPrefix() != null ? series.getPrefix() : seriesCode;
        return prefix + "-" + String.format("%06d", series.getLastSequenceNumber());
    }


    // COLLECTION PROMISE METHODS

    public CollectionPromiseResponseDto createCollectionPromise(Long invoiceId, CollectionPromiseRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        findActiveInvoiceById(invoiceId);

        CollectionPromise promise = CollectionPromise.builder()
            .company(companyRepository.getReferenceById(companyId))
            .invoiceId(invoiceId)
            .promisedDate(dto.promisedDate())
            .promisedAmount(dto.promisedAmount())
            .notes(dto.notes())
            .fulfilled(false)
            .build();

        CollectionPromise saved = promiseRepository.save(promise);
        systemLogService.log(LogLevel.INFO, "Tahsilat sözü oluşturuldu - Fatura: " + invoiceId + " Tarih: " + dto.promisedDate());
        return toPromiseDto(saved);
    }

    public List<CollectionPromiseResponseDto> getCollectionPromises(Long invoiceId) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<CollectionPromise> promises = promiseRepository.findByInvoiceIdAndCompanyCompanyId(invoiceId, companyId);
        return promises.stream().map(this::toPromiseDto).toList();
    }

    public void fulfillCollectionPromise(Long promiseId) {
        CollectionPromise promise = promiseRepository.findById(promiseId)
            .orElseThrow(() -> new RuntimeException("Collection promise not found: " + promiseId));
        promise.setFulfilled(true);
        promise.setFulfilledAt(LocalDate.now());
        promiseRepository.save(promise);
    }

    private CollectionPromiseResponseDto toPromiseDto(CollectionPromise p) {
        return new CollectionPromiseResponseDto(
            p.getPromiseId(),
            p.getInvoiceId(),
            p.getPromisedDate(),
            p.getPromisedAmount(),
            p.getNotes(),
            p.isFulfilled(),
            p.getFulfilledAt(),
            p.getCreatedAt()
        );
    }


    // PRIVATE METODLAR - VALIDATION

    private Customer validateCustomer(Long customerId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .orElseThrow(() -> new RuntimeException("Customer not found or inactive for your company: " + customerId));

        if (customer.getStatus() == CustomerStatus.BLOCKED) {
            throw new BusinessException("Bu müşteri bloke durumda, fatura kesilemez.");
        }
        if (customer.getStatus() == CustomerStatus.PASSIVE) {
            throw new BusinessException("Bu müşteri pasif durumda, fatura kesilemez.");
        }

        return customer;
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


    // PRIVATE METODLAR - BATCH LOADING

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


    // PRIVATE METODLAR - LINE ITEM & TOPLAM HESAP

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
            BigDecimal discountRate = req.discountRate() != null ? req.discountRate() : BigDecimal.ZERO;
            BigDecimal wtRate      = req.withholdingTaxRate() != null ? req.withholdingTaxRate() : BigDecimal.ZERO;

            BigDecimal lineNet       = round2(quantity.multiply(unitPrice));
            BigDecimal lineDiscount  = round2(lineNet.multiply(discountRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal afterDiscount = lineNet.subtract(lineDiscount);
            BigDecimal lineVat       = round2(afterDiscount.multiply(vatRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal lineTotal     = round2(afterDiscount.add(lineVat));

            InvoiceLineItem li = new InvoiceLineItem();
            li.setCompany(companyRepository.getReferenceById(companyId));
            li.setInvoiceId(invoiceId);
            li.setProductId(pid);
            li.setQuantity(req.quantity());
            li.setUnitPrice(unitPrice);
            li.setVatRate(vatRate);
            li.setLineTotal(lineTotal);
            li.setDiscountRate(discountRate);
            li.setWithholdingTaxRate(wtRate);
            li.setDeleted(false);

            saved.add(lineItemRepository.save(li));
        }
        return saved;
    }

    private void applyTotals(Invoice invoice, List<InvoiceLineItem> lineItems) {
        BigDecimal subtotal  = BigDecimal.ZERO;
        BigDecimal vatAmount = BigDecimal.ZERO;
        BigDecimal wtAmount  = BigDecimal.ZERO;

        for (InvoiceLineItem li : lineItems) {
            BigDecimal quantity = BigDecimal.valueOf(li.getQuantity());
            BigDecimal net      = round2(quantity.multiply(li.getUnitPrice()));
            BigDecimal liDiscountRate = li.getDiscountRate() != null ? li.getDiscountRate() : BigDecimal.ZERO;
            BigDecimal liDiscount = round2(net.multiply(liDiscountRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal afterDiscount = net.subtract(liDiscount);
            BigDecimal vat = round2(afterDiscount.multiply(li.getVatRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

            subtotal  = subtotal.add(afterDiscount);
            vatAmount = vatAmount.add(vat);

            BigDecimal liWtRate = li.getWithholdingTaxRate() != null ? li.getWithholdingTaxRate() : BigDecimal.ZERO;
            if (liWtRate.compareTo(BigDecimal.ZERO) > 0) {
                wtAmount = wtAmount.add(round2(afterDiscount.multiply(liWtRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
            }
        }

        BigDecimal invoiceDiscount = BigDecimal.ZERO;
        if (invoice.getDiscountType() != null && invoice.getDiscountAmount() != null && invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            if (invoice.getDiscountType().name().equals("PERCENTAGE")) {
                invoiceDiscount = round2(subtotal.multiply(invoice.getDiscountAmount()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            } else {
                invoiceDiscount = invoice.getDiscountAmount();
            }
        }

        BigDecimal finalSubtotal = round2(subtotal.subtract(invoiceDiscount));
        BigDecimal totalAmount  = round2(finalSubtotal.add(vatAmount).subtract(wtAmount));

        invoice.setSubtotal(finalSubtotal);
        invoice.setVatAmount(round2(vatAmount));
        invoice.setWithholdingTaxAmount(round2(wtAmount));
        invoice.setTotalAmount(totalAmount);
    }

    private void recordStockMovements(List<InvoiceLineItemRequestDto> items, Invoice savedInvoice, Map<Integer, Product> productMap) {
        for (InvoiceLineItemRequestDto item : items) {
            Integer resolvedProductId = item.productId() != null ? item.productId()
                    : productMap.values().stream()
                            .filter(p -> p.getBarcode().equals(item.newProduct().barcode()))
                            .findFirst().map(Product::getProductId).orElse(null);
            if (resolvedProductId == null) continue;

            if (savedInvoice.getInvoiceType() == InvoiceType.sale) {
                stockMovementService.recordMovement(resolvedProductId, -item.quantity(),
                        MovementType.SALE, "INVOICE", savedInvoice.getInvoiceId(), null, null);
            } else if (savedInvoice.getInvoiceType() == InvoiceType.purchase) {
                Product p = productMap.get(resolvedProductId);
                stockMovementService.recordMovement(resolvedProductId, item.quantity(),
                        MovementType.PURCHASE, "INVOICE", savedInvoice.getInvoiceId(),
                        p != null ? p.getCostPrice() : null, null);
            }
        }
    }

    private BigDecimal round2(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }


    // PRIVATE METODLAR - LOOKUP & DTO MAPPING

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

        BigDecimal totalAmountTry = invoice.getTotalAmount();
        if (invoice.getCurrency() != null && invoice.getCurrency() != Currency.TRY && invoice.getExchangeRate() != null) {
            totalAmountTry = round2(invoice.getTotalAmount().multiply(invoice.getExchangeRate()));
        }

        return new InvoiceResponseDto(
            invoice.getInvoiceId(),
            invoice.getInvoiceNumber(),
            invoice.getCustomerId(),
            customer != null ? customer.getName() : null,
            invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null,
            invoice.getInvoiceDate(),
            invoice.getDueDate(),
            invoice.getPaymentStatus() != null ? invoice.getPaymentStatus().name() : null,
            invoice.getSubtotal(),
            invoice.getVatAmount(),
            invoice.getTotalAmount(),
            lineItemDtos,
            invoice.getCreatedAt(),
            invoice.getUpdatedAt(),
            invoice.isDeleted(),
            invoice.getCurrency() != null ? invoice.getCurrency().name() : null,
            invoice.getExchangeRate(),
            totalAmountTry,
            invoice.getDiscountType() != null ? invoice.getDiscountType().name() : null,
            invoice.getDiscountAmount(),
            invoice.getWithholdingTaxAmount(),
            invoice.getDescription(),
            invoice.getDeliveryAddress(),
            invoice.isCancelled(),
            invoice.getCancellationReason(),
            invoice.getReferenceInvoiceId(),
            invoice.getSeries() != null ? invoice.getSeries().getSeriesCode() : null,
            invoice.getSequenceNumber()
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
            li.getLineTotal(),
            li.getDiscountRate(),
            li.getWithholdingTaxRate()
        );
    }
}
