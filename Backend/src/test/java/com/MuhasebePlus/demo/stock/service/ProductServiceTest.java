package com.MuhasebePlus.demo.stock.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.stock.dto.request.ProductRequestDto;
import com.MuhasebePlus.demo.stock.dto.response.ProductResponseDto;
import com.MuhasebePlus.demo.stock.entity.MovementType;
import com.MuhasebePlus.demo.stock.entity.Product;
import com.MuhasebePlus.demo.stock.entity.Stock;
import com.MuhasebePlus.demo.stock.repository.ProductRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private StockRepository stockRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;
    @Mock private StockMovementService stockMovementService;

    @InjectMocks
    private ProductService service;

    private static final Long COMPANY_ID = 1L;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
    }

    // Tests duplicate barcode protection before creating product and stock rows.
    @Test
    void createProductEntity_whenBarcodeExists_throwsAndDoesNotSave() {
        ProductRequestDto dto = request("PRD-1", 0, 3);
        when(productRepository.existsByBarcodeAndCompanyCompanyIdAndIsDeletedFalse("PRD-1", COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createProductEntity(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("barcode");

        verify(productRepository, never()).save(any());
        verify(stockRepository, never()).save(any());
    }

    // Tests product creation creates stock row and opening stock movement.
    @Test
    void createProduct_whenInitialQuantityExists_createsStockAndOpeningMovement() {
        ProductRequestDto dto = request("PRD-1", 12, 3);
        Stock stock = stock(5, 0, 3);
        when(productRepository.existsByBarcodeAndCompanyCompanyIdAndIsDeletedFalse("PRD-1", COMPANY_ID))
                .thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            Product product = inv.getArgument(0);
            product.setProductId(5);
            return product;
        });
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(stock));

        ProductResponseDto result = service.createProduct(dto);

        assertThat(result.productId()).isEqualTo(5);
        assertThat(result.minQuantity()).isEqualTo(3);
        verify(stockMovementService).recordMovement(
                eq(5),
                eq(12),
                eq(MovementType.OPENING_BALANCE),
                eq("INITIAL"),
                org.mockito.ArgumentMatchers.isNull(),
                eq(dto.costPrice()),
                any());
    }

    // Tests product listing maps stock quantity and min quantity.
    @Test
    void getAllProducts_whenProductsExist_mapsStockValues() {
        Product product = product(5, "PRD-1");
        Stock stock = stock(5, 8, 2);
        when(productRepository.findAllByCompanyCompanyIdAndIsDeletedFalseOrderByProductIdDesc(COMPANY_ID))
                .thenReturn(List.of(product));
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(stock));

        List<ProductResponseDto> result = service.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).quantity()).isEqualTo(8);
        assertThat(result.get(0).minQuantity()).isEqualTo(2);
    }

    // Tests product lookup rejects records that belong to another company.
    @Test
    void getProductById_whenCompanyDoesNotMatch_throwsAccessDenied() {
        Company otherCompany = new Company();
        otherCompany.setCompanyId(99L);
        Product product = product(5, "PRD-1");
        product.setCompany(otherCompany);
        when(productRepository.findById(5)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.getProductById(5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("yetkiniz");
    }

    // Tests duplicate barcode protection during update.
    @Test
    void updateProduct_whenNewBarcodeBelongsToAnotherProduct_throws() {
        Product product = product(5, "PRD-1");
        ProductRequestDto dto = request("PRD-2", 0, 3);
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        when(productRepository.existsByBarcodeAndProductIdNotAndCompanyCompanyIdAndIsDeletedFalse("PRD-2", 5, COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateProduct(5, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("barcode");

        verify(productRepository, never()).save(any());
    }

    // Tests successful update changes commercial values and maps response.
    @Test
    void updateProduct_whenValid_updatesCommercialFields() {
        Product product = product(5, "PRD-1");
        ProductRequestDto dto = request("PRD-2", 0, 4);
        Stock stock = stock(5, 6, 4);
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        when(productRepository.existsByBarcodeAndProductIdNotAndCompanyCompanyIdAndIsDeletedFalse("PRD-2", 5, COMPANY_ID))
                .thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(stock));

        ProductResponseDto result = service.updateProduct(5, dto);

        assertThat(result.barcode()).isEqualTo("PRD-2");
        assertThat(result.name()).isEqualTo("Urun PRD-2");
        assertThat(result.quantity()).isEqualTo(6);
    }

    // Tests active invoice line usage blocks product deletion.
    @Test
    void softDeleteProduct_whenUsedByActiveInvoiceLine_throwsAndDoesNotDelete() {
        Product product = product(5, "PRD-1");
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        when(invoiceLineItemRepository.existsByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.softDeleteProduct(5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invoice");

        assertThat(product.isDeleted()).isFalse();
        verify(productRepository, never()).save(any());
    }

    // Tests soft delete cascades to existing stock row.
    @Test
    void softDeleteProduct_whenUnused_marksProductAndStockDeleted() {
        Product product = product(5, "PRD-1");
        Stock stock = stock(5, 6, 2);
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        when(invoiceLineItemRepository.existsByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(stock));

        service.softDeleteProduct(5);

        assertThat(product.isDeleted()).isTrue();
        assertThat(stock.isDeleted()).isTrue();
        verify(stockRepository).save(stock);
    }

    // Tests restore refuses barcode conflicts with active products.
    @Test
    void restoreProduct_whenBarcodeConflictExists_throwsAndDoesNotSave() {
        Product product = product(5, "PRD-1");
        product.setDeleted(true);
        when(productRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(product));
        when(productRepository.existsByBarcodeAndProductIdNotAndCompanyCompanyIdAndIsDeletedFalse("PRD-1", 5, COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.restoreProduct(5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("restore");

        verify(productRepository, never()).save(any());
    }

    // Tests restore clears product and stock deletion flags.
    @Test
    void restoreProduct_whenNoConflict_clearsProductAndStockDeletedFlags() {
        Product product = product(5, "PRD-1");
        product.setDeleted(true);
        product.setDeletedAt(LocalDateTime.of(2026, 5, 1, 10, 0));
        Stock stock = stock(5, 6, 2);
        stock.setDeleted(true);
        when(productRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(product));
        when(productRepository.existsByBarcodeAndProductIdNotAndCompanyCompanyIdAndIsDeletedFalse("PRD-1", 5, COMPANY_ID))
                .thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(stock));

        ProductResponseDto result = service.restoreProduct(5);

        assertThat(result.isDeleted()).isFalse();
        assertThat(product.getDeletedAt()).isNull();
        assertThat(stock.isDeleted()).isFalse();
    }

    // Tests expired products delete stock rows before deleting product rows.
    @Test
    void hardDeleteExpired_whenExpiredProductsExist_deletesStockRowsAndProducts() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);
        Product first = product(1, "A");
        Product second = product(2, "B");
        when(productRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff))
                .thenReturn(List.of(first, second));

        int deletedCount = service.hardDeleteExpired(cutoff);

        assertThat(deletedCount).isEqualTo(2);
        verify(stockRepository).deleteByProductId(1);
        verify(stockRepository).deleteByProductId(2);
        verify(productRepository).delete(first);
        verify(productRepository).delete(second);
    }

    private ProductRequestDto request(String barcode, Integer initialQuantity, Integer minQuantity) {
        return new ProductRequestDto(
                barcode,
                "Urun " + barcode,
                "Aciklama",
                "adet",
                new BigDecimal("120.00"),
                new BigDecimal("20.00"),
                new BigDecimal("70.00"),
                initialQuantity,
                minQuantity
        );
    }

    private Product product(Integer id, String barcode) {
        Product product = new Product();
        product.setProductId(id);
        product.setCompany(company);
        product.setBarcode(barcode);
        product.setName("Urun " + barcode);
        product.setDescription("Aciklama");
        product.setUnit("adet");
        product.setSalePrice(new BigDecimal("120.00"));
        product.setVatRate(new BigDecimal("20.00"));
        product.setCostPrice(new BigDecimal("70.00"));
        product.setDeleted(false);
        return product;
    }

    private Stock stock(Integer productId, Integer quantity, Integer minQuantity) {
        Stock stock = new Stock();
        stock.setCompany(company);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setMinQuantity(minQuantity);
        stock.setDeleted(false);
        return stock;
    }
}
