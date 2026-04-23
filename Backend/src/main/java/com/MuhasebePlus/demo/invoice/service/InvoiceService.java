package com.MuhasebePlus.demo.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.repository.CustomerRepository;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceLineItemRequestDto;
import com.MuhasebePlus.demo.invoice.dto.request.InvoiceRequestDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceLineItemResponseDto;
import com.MuhasebePlus.demo.invoice.dto.response.InvoiceResponseDto;
import com.MuhasebePlus.demo.invoice.entity.Invoice;
import com.MuhasebePlus.demo.invoice.entity.InvoiceLineItem;
import com.MuhasebePlus.demo.invoice.entity.InvoiceType;
import com.MuhasebePlus.demo.invoice.entity.PaymentStatus;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceRepository;
import com.MuhasebePlus.demo.stock.dto.request.StockCheckItemDto;
import com.MuhasebePlus.demo.stock.dto.response.StockCheckResultDto;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.service.StockService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;


    //PUBLIC METODLAR - CRUD

    public InvoiceResponseDto createInvoice(InvoiceRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        
        // 1) Fatura numarası mükerrer mi?
        if (invoiceRepository.existsByInvoiceNumberAndCompanyCompanyId(dto.invoiceNumber(), companyId)) {
            throw new RuntimeException("This invoice number is already used in your company: " + dto.invoiceNumber());
        }

        // 2) checkCustomerExists
        validateCustomer(dto.customerId());

        // 3) checkProductsExist
        Map<Integer, Product> productMap = fetchAndValidateProducts(dto.lineItems());

        // 4) checkStock (sadece sale için)
        boolean stockSufficient = true;
        if (dto.invoiceType() == InvoiceType.sale) {
            List<StockCheckResultDto> checks = stockService.checkStock(toStockCheckList(dto.lineItems()));
            stockSufficient = checks.stream().allMatch(StockCheckResultDto::isSufficient);
        }

        // 5) Invoice header kaydet
        Invoice invoice = new Invoice();
        invoice.setCompany(companyRepository.getReferenceById(companyId));
        invoice.setInvoiceNumber(dto.invoiceNumber());
        invoice.setCustomerId(dto.customerId());
        invoice.setInvoiceType(dto.invoiceType());
        invoice.setDueDate(dto.dueDate());
        invoice.setPaymentStatus(stockSufficient ? PaymentStatus.pending : PaymentStatus.draft);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 6) Line items kaydet + toplamları hesapla
        List<InvoiceLineItem> savedLineItems = saveLineItems(savedInvoice.getInvoiceId(), dto.lineItems(), productMap);
        applyTotals(savedInvoice, savedLineItems);
        Invoice finalInvoice = invoiceRepository.save(savedInvoice);

        // 7) Stok yeterliyse düş (sale için), yetersizse DRAFT olarak bırak
        if (stockSufficient && dto.invoiceType() == InvoiceType.sale) {
            stockService.decreaseStock(toStockCheckList(dto.lineItems()));
        }

        return toResponseDto(finalInvoice, savedLineItems);
    }

    public InvoiceResponseDto confirmInvoice(Long invoiceId) {
        Invoice invoice = findInvoiceById(invoiceId);
        Long companyId = companyContext.getCurrentCompanyId();

        if (invoice.getPaymentStatus() != PaymentStatus.draft) {
            throw new RuntimeException("Only draft invoices can be confirmed. Current status: "
                + invoice.getPaymentStatus());
        }

        List<InvoiceLineItem> lineItems = lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoiceId, companyId);
        if (lineItems.isEmpty()) {
            throw new RuntimeException("Draft invoice has no line items: " + invoiceId);
        }

        // Sale faturası değilse sadece status geçişi (stok etkilenmez)
        if (invoice.getInvoiceType() == InvoiceType.sale) {
            List<StockCheckItemDto> checkList = lineItems.stream()
                .map(li -> new StockCheckItemDto(li.getProductId(), li.getQuantity()))
                .toList();

            List<StockCheckResultDto> checks = stockService.checkStock(checkList);
            boolean sufficient = checks.stream().allMatch(StockCheckResultDto::isSufficient);

            if (!sufficient) {
                throw new RuntimeException("Stock is still insufficient for this draft invoice: " + invoiceId);
            }

            stockService.decreaseStock(checkList);
        }

        invoice.setPaymentStatus(PaymentStatus.pending);
        Invoice confirmed = invoiceRepository.save(invoice);

        return toResponseDto(confirmed, lineItems);
    }

    public List<InvoiceResponseDto> getAllInvoices() {
        Long companyId = companyContext.getCurrentCompanyId();
        return invoiceRepository.findByCompanyCompanyId(companyId).stream()
            .map(this::toResponseDtoWithLines)
            .toList();
    }

    public InvoiceResponseDto getInvoiceById(Long invoiceId) {
        Invoice invoice = findInvoiceById(invoiceId);
        return toResponseDtoWithLines(invoice);
    }

    public InvoiceResponseDto updateInvoice(Long invoiceId, InvoiceRequestDto dto) {
        Invoice invoice = findInvoiceById(invoiceId);
        Long companyId = companyContext.getCurrentCompanyId();

        if (invoice.getPaymentStatus() == PaymentStatus.paid) {
            throw new RuntimeException("Paid invoices cannot be updated: " + invoiceId);
        }

        // Invoice number değişiyorsa mükerrerlik kontrolü
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
        Invoice invoice = findInvoiceById(invoiceId);

        if (invoice.getPaymentStatus() == PaymentStatus.paid) {
            throw new RuntimeException("Paid invoices cannot be deleted: " + invoiceId);
        }

        invoiceRepository.delete(invoice);
    }

    public List<InvoiceResponseDto> getInvoiceByCustomerId(Long customerId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return invoiceRepository.findByCustomerIdAndCompanyCompanyId(customerId, companyId).stream()
            .map(this::toResponseDtoWithLines)
            .toList();
    }

    public List<InvoiceResponseDto> getInvoiceByFilters(PaymentStatus paymentStatus, InvoiceType invoiceType) {
        Long companyId = companyContext.getCurrentCompanyId();
        
        if (paymentStatus != null && invoiceType != null) {
            return invoiceRepository.findByPaymentStatusAndInvoiceTypeAndCompanyCompanyId(paymentStatus, invoiceType, companyId).stream()
                .map(this::toResponseDtoWithLines)
                .toList();
        } else if (paymentStatus != null) {
            return invoiceRepository.findByPaymentStatusAndCompanyCompanyId(paymentStatus, companyId).stream()
                .map(this::toResponseDtoWithLines)
                .toList();
        } else if (invoiceType != null) {
            return invoiceRepository.findByInvoiceTypeAndCompanyCompanyId(invoiceType, companyId).stream()
                .map(this::toResponseDtoWithLines)
                .toList();
        }
        return getAllInvoices();
    }

    public InvoiceResponseDto updatePaymentStatus(Long invoiceId, PaymentStatus paymentStatus) {
        Invoice invoice = findInvoiceById(invoiceId);

        // DRAFT'a manuel geçiş yasak; DRAFT'tan PENDING'e sadece confirmInvoice üzerinden.
        if (paymentStatus == PaymentStatus.draft) {
            throw new RuntimeException("Cannot set payment status to draft manually.");
        }
        if (invoice.getPaymentStatus() == PaymentStatus.draft && paymentStatus == PaymentStatus.pending) {
            throw new RuntimeException("Use /confirm endpoint to transition a draft invoice to pending.");
        }

        invoice.setPaymentStatus(paymentStatus);
        Invoice updated = invoiceRepository.save(invoice);
        return toResponseDtoWithLines(updated);
    }


    //PRIVATE METODLAR - VALIDATION

    private void validateCustomer(Long customerId) {
        Long companyId = companyContext.getCurrentCompanyId();
        customerRepository.findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(customerId, companyId)
            .orElseThrow(() -> new RuntimeException("Customer not found or inactive for your company: " + customerId));
    }

    private Map<Integer, Product> fetchAndValidateProducts(List<InvoiceLineItemRequestDto> items) {
        Long companyId = companyContext.getCurrentCompanyId();
        Map<Integer, Product> map = new HashMap<>();
        
        for (InvoiceLineItemRequestDto item : items) {
            if (map.containsKey(item.productId())) {
                continue;
            }
            Product product = productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(item.productId(), companyId)
                .orElseThrow(() -> new RuntimeException(
                    "Product not found or inactive for your company: " + item.productId()));
            map.put(item.productId(), product);
        }
        return map;
    }

    private List<StockCheckItemDto> toStockCheckList(List<InvoiceLineItemRequestDto> items) {
        return items.stream()
            .map(i -> new StockCheckItemDto(i.productId(), i.quantity()))
            .toList();
    }


    //PRIVATE METODLAR - LINE ITEM & TOPLAM HESAP

    private List<InvoiceLineItem> saveLineItems(
            Long invoiceId,
            List<InvoiceLineItemRequestDto> items,
            Map<Integer, Product> productMap) {

        Long companyId = companyContext.getCurrentCompanyId();
        List<InvoiceLineItem> saved = new ArrayList<>();
        
        for (InvoiceLineItemRequestDto req : items) {
            Product product = productMap.get(req.productId());

            BigDecimal unitPrice = product.getSalePrice() != null ? product.getSalePrice() : BigDecimal.ZERO;
            BigDecimal vatRate   = product.getVatRate()   != null ? product.getVatRate()   : BigDecimal.ZERO;
            BigDecimal quantity  = BigDecimal.valueOf(req.quantity());

            BigDecimal lineNet   = round2(quantity.multiply(unitPrice));
            BigDecimal lineVat   = round2(lineNet.multiply(vatRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal lineTotal = round2(lineNet.add(lineVat));

            InvoiceLineItem li = new InvoiceLineItem();
            li.setCompany(companyRepository.getReferenceById(companyId));
            li.setInvoiceId(invoiceId);
            li.setProductId(req.productId());
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

    private InvoiceResponseDto toResponseDtoWithLines(Invoice invoice) {
        Long companyId = companyContext.getCurrentCompanyId();
        List<InvoiceLineItem> lineItems = lineItemRepository.findByInvoiceIdAndCompanyCompanyIdAndIsDeletedFalse(invoice.getInvoiceId(), companyId);
        return toResponseDto(invoice, lineItems);
    }

    private InvoiceResponseDto toResponseDto(Invoice invoice, List<InvoiceLineItem> lineItems) {
        Long companyId = companyContext.getCurrentCompanyId();
        Customer customer = customerRepository.findByCustomerIdAndCompanyCompanyId(invoice.getCustomerId(), companyId).orElse(null);

        List<InvoiceLineItemResponseDto> lineItemDtos = lineItems.stream()
            .map(this::toLineItemResponseDto)
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
            invoice.getUpdatedAt()
        );
    }

    private InvoiceLineItemResponseDto toLineItemResponseDto(InvoiceLineItem li) {
        Long companyId = companyContext.getCurrentCompanyId();
        Product product = productRepository.findByProductIdAndCompanyCompanyId(li.getProductId(), companyId).orElse(null);
        
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
