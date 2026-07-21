package com.MuhasebePlus.demo.stock.service;

import com.MuhasebePlus.demo.common.exception.BusinessException;
import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.log.service.SystemLogService;
import com.MuhasebePlus.demo.stock.dto.request.StockMovementRequestDto;
import com.MuhasebePlus.demo.stock.entity.MovementType;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.entity.StockMovement;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
import com.MuhasebePlus.demo.stock.repository.StockMovementRepository;
import com.MuhasebePlus.demo.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockMovementServiceTest {

    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private StockRepository stockRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private SystemLogService systemLogService;

    @InjectMocks
    private StockMovementService service;

    private static final Long COMPANY_ID = 1L;
    private static final Integer PRODUCT_ID = 10;

    private Company company;
    private Product product;
    private Stock stock;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);

        product = Product.builder().productId(PRODUCT_ID).company(company).build();
        product.setName("Test Ürünü");

        stock = Stock.builder()
                .stockId(1).company(company).productId(PRODUCT_ID).quantity(100).minQuantity(0).build();
        stock.setDeleted(false);

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(PRODUCT_ID, COMPANY_ID))
                .thenReturn(Optional.of(product));
        when(stockRepository.findByProductIdAndCompanyCompanyId(PRODUCT_ID, COMPANY_ID))
                .thenReturn(Optional.of(stock));
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(stockMovementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── recordMovement ────────────────────────────────────────────────────────

    @Test
    void recordMovement_whenProductNotFound_throwsBusinessException() {
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(PRODUCT_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordMovement(PRODUCT_ID, -10,
                MovementType.ADJUSTMENT_OUT, "MANUAL", null, null, "test"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ürün bulunamadı");
    }

    @Test
    void recordMovement_whenStockDoesNotExist_autoCreatesNewStock() {
        when(stockRepository.findByProductIdAndCompanyCompanyId(PRODUCT_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        service.recordMovement(PRODUCT_ID, 10, MovementType.ADJUSTMENT_IN, "MANUAL", null, null, "İlk bakiye");

        verify(stockRepository, atLeast(1)).save(argThat(s ->
                s.getProductId().equals(PRODUCT_ID) && s.getCompany().getCompanyId().equals(COMPANY_ID)));
    }

    @Test
    void recordMovement_decreasingType_withInsufficientStock_throwsBusinessException() {
        stock.setQuantity(5);

        assertThatThrownBy(() -> service.recordMovement(PRODUCT_ID, -10,
                MovementType.ADJUSTMENT_OUT, "MANUAL", null, null, "Yetersiz"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yetersiz stok");
    }

    @Test
    void recordMovement_decreasingType_withSufficientStock_updatesQuantity() {
        stock.setQuantity(50);

        service.recordMovement(PRODUCT_ID, -20, MovementType.ADJUSTMENT_OUT, "MANUAL", null, null, "Azaltma");

        assertThat(stock.getQuantity()).isEqualTo(30);
        verify(stockRepository).save(stock);
    }

    @Test
    void recordMovement_purchaseType_bypassesNegativeStockCheck() {
        // PURCHASE INCREASING_TYPES içinde → negatif bakiye kontrolü atlanır
        stock.setQuantity(0);

        assertThatNoException().isThrownBy(() ->
                service.recordMovement(PRODUCT_ID, -999, MovementType.PURCHASE,
                        "INVOICE", 1L, new BigDecimal("10.00"), null));
    }

    @Test
    void recordMovement_savesMovementWithCorrectFields() {
        service.recordMovement(PRODUCT_ID, 5, MovementType.ADJUSTMENT_IN, "MANUAL", null,
                new BigDecimal("25.00"), "Test sebebi");

        verify(stockMovementRepository).save(argThat(m ->
                m.getProductId().equals(PRODUCT_ID) &&
                m.getQuantity() == 5 &&
                m.getMovementType() == MovementType.ADJUSTMENT_IN &&
                "MANUAL".equals(m.getSourceType())
        ));
    }

    // ── createManualMovement ──────────────────────────────────────────────────

    @Test
    void createManualMovement_whenTypeIsPurchase_throwsBusinessException() {
        var dto = new StockMovementRequestDto(PRODUCT_ID, 10, MovementType.PURCHASE, "sebep", null);

        assertThatThrownBy(() -> service.createManualMovement(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("manuel olarak girilemez");
    }

    @Test
    void createManualMovement_whenTypeIsSale_throwsBusinessException() {
        var dto = new StockMovementRequestDto(PRODUCT_ID, 10, MovementType.SALE, "sebep", null);

        assertThatThrownBy(() -> service.createManualMovement(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("manuel olarak girilemez");
    }

    @Test
    void createManualMovement_whenReasonIsNull_throwsBusinessException() {
        var dto = new StockMovementRequestDto(PRODUCT_ID, 10, MovementType.ADJUSTMENT_IN, null, null);

        assertThatThrownBy(() -> service.createManualMovement(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sebep zorunludur");
    }

    @Test
    void createManualMovement_whenReasonIsBlank_throwsBusinessException() {
        var dto = new StockMovementRequestDto(PRODUCT_ID, 10, MovementType.ADJUSTMENT_IN, "   ", null);

        assertThatThrownBy(() -> service.createManualMovement(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sebep zorunludur");
    }

    @Test
    void createManualMovement_adjustmentIn_usesPositiveSignedQuantity() {
        var dto = new StockMovementRequestDto(PRODUCT_ID, 15, MovementType.ADJUSTMENT_IN, "Stok düzeltme", null);

        service.createManualMovement(dto);

        verify(stockMovementRepository).save(argThat(m -> m.getQuantity() == 15));
    }

    @Test
    void createManualMovement_adjustmentOut_usesNegativeSignedQuantityAndUpdatesStock() {
        stock.setQuantity(100);
        var dto = new StockMovementRequestDto(PRODUCT_ID, 30, MovementType.ADJUSTMENT_OUT, "Fire", null);

        service.createManualMovement(dto);

        verify(stockMovementRepository).save(argThat(m -> m.getQuantity() == -30));
        assertThat(stock.getQuantity()).isEqualTo(70);
    }

    // ── recordReverseMovementsForInvoice ──────────────────────────────────────

    @Test
    void recordReverseMovementsForInvoice_createsOneReversePerOriginalMovement() {
        StockMovement m1 = StockMovement.builder()
                .productId(PRODUCT_ID).quantity(10).movementType(MovementType.SALE)
                .sourceType("INVOICE").sourceId(5L).build();
        StockMovement m2 = StockMovement.builder()
                .productId(PRODUCT_ID).quantity(5).movementType(MovementType.SALE)
                .sourceType("INVOICE").sourceId(5L).build();
        when(stockMovementRepository.findBySourceTypeAndSourceId("INVOICE", 5L))
                .thenReturn(List.of(m1, m2));

        service.recordReverseMovementsForInvoice(5L);

        verify(stockMovementRepository, atLeast(2)).save(argThat(m ->
                "INVOICE_REVERSE".equals(m.getSourceType()) && m.getSourceId().equals(5L)
        ));
    }

    @Test
    void recordReverseMovementsForInvoice_whenNoMovements_doesNothing() {
        when(stockMovementRepository.findBySourceTypeAndSourceId("INVOICE", 99L))
                .thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.recordReverseMovementsForInvoice(99L));
    }
}
