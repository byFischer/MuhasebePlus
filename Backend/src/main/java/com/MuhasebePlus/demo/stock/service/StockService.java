package com.MuhasebePlus.demo.stock.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.MuhasebePlus.demo.stock.dto.request.StockAdjustmentRequestDto;
import com.MuhasebePlus.demo.stock.dto.request.StockCheckItemDto;
import com.MuhasebePlus.demo.stock.dto.request.StockRequestDto;
import com.MuhasebePlus.demo.stock.dto.response.StockCheckResultDto;
import com.MuhasebePlus.demo.stock.dto.response.StockResponseDto;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;


    //PUBLIC METODLAR 

    public StockResponseDto createStock(StockRequestDto dto) {
        if (productRepository.findByProductIdAndIsDeletedFalse(dto.productId()).isEmpty()) {
            throw new RuntimeException("Product not found or inactive: " + dto.productId());
        }

        if (stockRepository.existsByProductId(dto.productId())) {
            throw new RuntimeException("Stock already exists for productId: " + dto.productId());
        }

        Stock stock = new Stock();
        stock.setProductId(dto.productId());
        stock.setQuantity(dto.quantity());
        stock.setMinQuantity(dto.minQuantity());
        stock.setDeleted(false);
        stock.setLastCountDate(LocalDateTime.now());

        Stock saved = stockRepository.save(stock);
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

        if (invoiceLineItemRepository.existsByProductIdAndIsDeletedFalse(productId)) {
            throw new RuntimeException(
                "Stock is used in active invoice line items; cannot delete: productId=" + productId);
        }

        stock.setDeleted(true);
        stockRepository.save(stock);
    }

    public StockResponseDto restoreStock(Integer productId) {
        Stock stock = stockRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Stock not found for productId: " + productId));

        if (productRepository.findByProductIdAndIsDeletedFalse(productId).isEmpty()) {
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
        return stockRepository.findActiveStocks().stream()
            .map(this::toResponseDto)
            .toList();
    }

    public List<StockResponseDto> getLowStockItems() {
        return stockRepository.findLowStockItems().stream()
            .map(this::toResponseDto)
            .toList();
    }


    //PUBLIC METODLAR 

    public StockResponseDto addStock(Integer productId, StockAdjustmentRequestDto dto) {
        Stock stock = findActiveStockByProductId(productId);

        int current = stock.getQuantity() != null ? stock.getQuantity() : 0;
        stock.setQuantity(current + dto.amount());

        Stock updated = stockRepository.save(stock);
        return toResponseDto(updated);
    }

    public StockResponseDto removeStock(Integer productId, StockAdjustmentRequestDto dto) {
        Stock stock = findActiveStockByProductId(productId);

        int current = stock.getQuantity() != null ? stock.getQuantity() : 0;
        if (current < dto.amount()) {
            throw new RuntimeException(
                "Insufficient stock for productId " + productId
                + ": requested " + dto.amount() + ", available " + current);
        }

        stock.setQuantity(current - dto.amount());

        Stock updated = stockRepository.save(stock);
        return toResponseDto(updated);
    }

    public StockResponseDto setQuantity(Integer productId, Integer newQuantity) {
        if (newQuantity == null || newQuantity < 0) {
            throw new RuntimeException("Quantity must be non-negative");
        }

        Stock stock = findActiveStockByProductId(productId);
        stock.setQuantity(newQuantity);
        stock.setLastCountDate(LocalDateTime.now());

        Stock updated = stockRepository.save(stock);
        return toResponseDto(updated);
    }


    //PUBLIC METODLAR - FATURA ENTEGRASYONU (FAT.1)

    public List<StockCheckResultDto> checkStock(List<StockCheckItemDto> items) {
        List<StockCheckResultDto> results = new ArrayList<>();

        for (StockCheckItemDto item : items) {
            Stock stock = stockRepository.findByProductId(item.productId())
                .filter(s -> !s.isDeleted())
                .orElse(null);

            int available = (stock != null && stock.getQuantity() != null) ? stock.getQuantity() : 0;
            boolean sufficient = stock != null && available >= item.quantity();

            results.add(new StockCheckResultDto(
                item.productId(),
                item.quantity(),
                available,
                sufficient
            ));
        }

        return results;
    }

    public void decreaseStock(List<StockCheckItemDto> items) {
        List<StockCheckResultDto> checks = checkStock(items);

        for (StockCheckResultDto check : checks) {
            if (!check.isSufficient()) {
                throw new RuntimeException(
                    "Insufficient stock for productId " + check.productId()
                    + ": requested " + check.requestedQuantity()
                    + ", available " + check.availableQuantity());
            }
        }

        for (StockCheckItemDto item : items) {
            Stock stock = findActiveStockByProductId(item.productId());
            stock.setQuantity(stock.getQuantity() - item.quantity());
            stockRepository.save(stock);
        }
    }


    //PUBLIC METODLAR - SAYIM

    public StockResponseDto markCounted(Integer productId, Integer countedQuantity) {
        return setQuantity(productId, countedQuantity);
    }

    public void scheduleCount(Integer productId, LocalDateTime plannedDate) {
        // TODO: scheduler modülü hazır olduğunda ScheduleTask tablosuna yazılacak.
        // Şimdilik sadece ürünün aktif bir stok satırı olduğunu doğruluyoruz.
        findActiveStockByProductId(productId);
    }


    //PRIVATE METODLAR

    private Stock findActiveStockByProductId(Integer productId) {
        return stockRepository.findByProductId(productId)
            .filter(s -> !s.isDeleted())
            .orElseThrow(() -> new RuntimeException("Stock not found for productId: " + productId));
    }

    private StockResponseDto toResponseDto(Stock stock) {
        Product product = productRepository.findById(stock.getProductId()).orElse(null);

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
