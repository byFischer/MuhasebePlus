package com.MuhasebePlus.demo.stock.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.stock.dto.request.StockRequestDto;
import com.MuhasebePlus.demo.stock.dto.response.StockResponseDto;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;


    //PUBLIC METODLAR 

    public StockResponseDto createStock(StockRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        
        if (productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(dto.productId(), companyId).isEmpty()) {
            throw new RuntimeException("Product not found or inactive for your company: " + dto.productId());
        }

        if (stockRepository.existsByProductIdAndCompanyCompanyId(dto.productId(), companyId)) {
            throw new RuntimeException("Stock already exists for productId: " + dto.productId());
        }

        Stock stock = new Stock();
        stock.setCompany(companyRepository.getReferenceById(companyId));
        stock.setProductId(dto.productId());
        stock.setQuantity(dto.quantity());
        stock.setMinQuantity(dto.minQuantity());
        stock.setDeleted(false);
        stock.setLastCountDate(LocalDateTime.now());

        Stock saved = stockRepository.save(stock);
        log.info("Stock created productId={} companyId={}", dto.productId(), companyId);
        return toResponseDto(saved);
    }

    public StockResponseDto updateStock(Integer productId, StockRequestDto dto) {
        Stock stock = findActiveStockByProductId(productId);

        stock.setMinQuantity(dto.minQuantity());

        Stock updated = stockRepository.save(stock);
        return toResponseDto(updated);
    }

    public void softDeleteStock(Integer productId) {
        Stock stock = findActiveStockByProductId(productId);
        Long companyId = companyContext.getCurrentCompanyId();

        if (invoiceLineItemRepository.existsByProductIdAndCompanyCompanyIdAndIsDeletedFalse(productId, companyId)) {
            throw new RuntimeException(
                "Stock is used in active invoice line items; cannot delete: productId=" + productId);
        }

        stock.setDeleted(true);
        stockRepository.save(stock);
        log.warn("Stock soft-deleted productId={} companyId={}", productId, companyId);
    }

    public StockResponseDto restoreStock(Integer productId) {
        Long companyId = companyContext.getCurrentCompanyId();
        
        Stock stock = stockRepository.findByProductIdAndCompanyCompanyId(productId, companyId)
            .orElseThrow(() -> new RuntimeException("Stock not found for productId: " + productId));

        if (productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(productId, companyId).isEmpty()) {
            throw new RuntimeException(
                "Cannot restore stock for deleted product — restore product first: " + productId);
        }

        stock.setDeleted(false);
        Stock restored = stockRepository.save(stock);

        return toResponseDto(restored);
    }

    public StockResponseDto getStockByProductId(Integer productId) {
        Stock stock = findActiveStockByProductId(productId);
        return toResponseDto(stock);
    }

    public List<StockResponseDto> getAllActiveStocks() {
        Long companyId = companyContext.getCurrentCompanyId();
        return stockRepository.findActiveStocks(companyId).stream()
            .map(this::toResponseDto)
            .toList();
    }

    public List<StockResponseDto> getLowStockItems() {
        Long companyId = companyContext.getCurrentCompanyId();
        return stockRepository.findLowStockItems(companyId).stream()
            .map(this::toResponseDto)
            .toList();
    }


    //PRIVATE METODLAR

    private Stock findActiveStockByProductId(Integer productId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Stock stock = stockRepository.findByProductIdAndCompanyCompanyId(productId, companyId)
            .orElseThrow(() -> new RuntimeException("Stock not found for productId: " + productId));
            
        if (!stock.getCompany().getCompanyId().equals(companyId)) {
            throw new RuntimeException("Bu kaydı görüntüleme yetkiniz yok");
        }
        
        if (stock.isDeleted()) {
            throw new RuntimeException("Stock not found for productId: " + productId);
        }
        
        return stock;
    }

    private StockResponseDto toResponseDto(Stock stock) {
        Long companyId = companyContext.getCurrentCompanyId();
        Product product = productRepository.findByProductIdAndCompanyCompanyId(stock.getProductId(), companyId).orElse(null);

        boolean lowStock = stock.getMinQuantity() != null
            && stock.getQuantity() != null
            && stock.getQuantity() < stock.getMinQuantity();

        return new StockResponseDto(
            stock.getStockId(),
            stock.getProductId(),
            product != null ? product.getName() : null,
            product != null ? product.getBarcode() : null,
            stock.getQuantity(),
            stock.getMinQuantity(),
            lowStock,
            stock.getLastCountDate(),
            stock.isDeleted(),
            stock.getCreatedAt(),
            stock.getUpdatedAt()
        );
    }

}
