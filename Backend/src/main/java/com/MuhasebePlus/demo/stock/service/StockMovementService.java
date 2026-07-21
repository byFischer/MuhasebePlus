package com.MuhasebePlus.demo.stock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.scheduler.HardDeletable;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.log.entity.LogLevel;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.stock.dto.request.StockMovementRequestDto;
import com.MuhasebePlus.demo.stock.dto.response.StockMovementResponseDto;
import com.MuhasebePlus.demo.stock.entity.MovementType;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.entity.StockMovement;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockMovementRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class StockMovementService implements HardDeletable {

    private static final Set<MovementType> MANUAL_TYPES = EnumSet.of(
            MovementType.ADJUSTMENT_IN, MovementType.ADJUSTMENT_OUT,
            MovementType.RETURN_IN, MovementType.RETURN_OUT,
            MovementType.PRODUCTION_IN, MovementType.PRODUCTION_OUT,
            MovementType.OPENING_BALANCE
    );

    private static final Set<MovementType> INCREASING_TYPES = EnumSet.of(
            MovementType.PURCHASE, MovementType.RETURN_IN,
            MovementType.ADJUSTMENT_IN, MovementType.PRODUCTION_IN,
            MovementType.OPENING_BALANCE
    );

    private final StockMovementRepository stockMovementRepository;
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;
    private final SystemLogService systemLogService;

    public StockMovement recordMovement(Integer productId, int signedQuantity, MovementType type,
                                  String sourceType, Long sourceId, BigDecimal unitCost, String reason) {
        Long companyId = companyContext.getCurrentCompanyId();

        Product product = productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(productId, companyId)
                .orElseThrow(() -> new BusinessException("Ürün bulunamadı: " + productId));

        Stock stock = stockRepository.findByProductIdAndCompanyCompanyId(productId, companyId)
                .orElseGet(() -> {
                    Stock newStock = new Stock();
                    newStock.setCompany(companyRepository.getReferenceById(companyId));
                    newStock.setProductId(productId);
                    newStock.setQuantity(0);
                    newStock.setMinQuantity(0);
                    newStock.setDeleted(false);
                    return stockRepository.save(newStock);
                });

        if (!INCREASING_TYPES.contains(type) && stock.getQuantity() + signedQuantity < 0) {
            throw new BusinessException("Yetersiz stok: " + product.getName()
                    + " (Mevcut: " + stock.getQuantity() + ", İstenen: " + Math.abs(signedQuantity) + ")");
        }

        StockMovement movement = StockMovement.builder()
                .company(companyRepository.getReferenceById(companyId))
                .productId(productId)
                .quantity(signedQuantity)
                .movementType(type)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .unitCost(unitCost)
                .reason(reason)
                .build();
        movement.setDeleted(false);

        StockMovement saved = stockMovementRepository.save(movement);

        stock.setQuantity(stock.getQuantity() + signedQuantity);
        stockRepository.save(stock);

        systemLogService.log(LogLevel.INFO, "Stok hareketi: " + product.getName()
                + " | " + type + " | " + signedQuantity + " | Kaynak: " + sourceType);

        return saved;
    }

    public StockMovementResponseDto createManualMovement(StockMovementRequestDto dto) {
        if (!MANUAL_TYPES.contains(dto.movementType())) {
            throw new BusinessException("Bu hareket türü manuel olarak girilemez: " + dto.movementType());
        }

        if (dto.reason() == null || dto.reason().isBlank()) {
            throw new BusinessException("Manuel hareketlerde sebep zorunludur");
        }

        int signedQuantity = INCREASING_TYPES.contains(dto.movementType())
                ? dto.quantity()
                : -dto.quantity();

        StockMovement movement = recordMovement(dto.productId(), signedQuantity, dto.movementType(),
                "MANUAL", null, dto.unitCost(), dto.reason());

        return toResponseDto(movement);
    }

    public List<StockMovementResponseDto> getMovementsByProduct(Integer productId) {
        Long companyId = companyContext.getCurrentCompanyId();
        return stockMovementRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalseOrderByCreatedAtDesc(productId, companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public List<StockMovementResponseDto> getAllMovements() {
        Long companyId = companyContext.getCurrentCompanyId();
        return stockMovementRepository.findByCompanyCompanyIdAndIsDeletedFalseOrderByCreatedAtDesc(companyId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    public void recordReverseMovementsForInvoice(Long invoiceId) {
        List<StockMovement> movements = stockMovementRepository.findBySourceTypeAndSourceId("INVOICE", invoiceId);
        for (StockMovement m : movements) {
            recordMovement(m.getProductId(), -m.getQuantity(), m.getMovementType(),
                    "INVOICE_REVERSE", invoiceId, m.getUnitCost(), "Fatura silme - ters hareket");
        }
    }

    @Override
    public int hardDeleteExpired(LocalDateTime cutoff) {
        List<StockMovement> expired = stockMovementRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        for (StockMovement m : expired) {
            stockMovementRepository.delete(m);
        }
        return expired.size();
    }

    private StockMovementResponseDto toResponseDto(StockMovement m) {
        return new StockMovementResponseDto(
                m.getMovementId(),
                m.getProductId(),
                m.getProduct() != null ? m.getProduct().getName() : null,
                m.getQuantity(),
                m.getMovementType(),
                m.getSourceType(),
                m.getSourceId(),
                m.getUnitCost(),
                m.getReason(),
                m.getCreatedAt()
        );
    }
}
