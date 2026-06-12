package com.MuhasebePlus.demo.stock.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.invoice.repository.InvoiceLineItemRepository;
import com.MuhasebePlus.demo.stock.dto.request.StockRequestDto;
import com.MuhasebePlus.demo.stock.dto.response.StockResponseDto;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockServiceTest {

    @Mock private StockRepository stockRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private StockService service;

    private static final Long COMPANY_ID = 1L;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(stockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // Rejects stock creation when product does not exist for the current company.
    @Test
    void createStock_whenProductMissing_throwsAndDoesNotSave() {
        StockRequestDto dto = request(5, 10, 3);
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createStock(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");

        verify(stockRepository, never()).save(any());
    }

    // Rejects duplicate stock rows for the same product and company.
    @Test
    void createStock_whenStockAlreadyExists_throwsAndDoesNotSave() {
        StockRequestDto dto = request(5, 10, 3);
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(Optional.of(product(5)));
        when(stockRepository.existsByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createStock(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(stockRepository, never()).save(any());
    }

    // Creates stock for an active product and maps product details in the response.
    @Test
    void createStock_whenValid_savesStockAndMapsProductDetails() {
        Product product = product(5);
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(Optional.of(product));
        when(productRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(product));
        when(stockRepository.existsByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(false);

        StockResponseDto result = service.createStock(request(5, 10, 3));

        assertThat(result.productId()).isEqualTo(5);
        assertThat(result.productName()).isEqualTo("Urun 5");
        assertThat(result.barcode()).isEqualTo("BR-5");
        assertThat(result.isLowStock()).isFalse();
        verify(stockRepository).save(any(Stock.class));
    }

    // Updates only the minimum stock quantity and keeps current stock quantity unchanged.
    @Test
    void updateStock_whenActive_updatesMinQuantity() {
        Stock stock = stock(5, 7, 2);
        Product product = product(5);
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(stock));
        when(productRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(product));

        StockResponseDto result = service.updateStock(5, request(5, 99, 8));

        assertThat(result.quantity()).isEqualTo(7);
        assertThat(result.minQuantity()).isEqualTo(8);
        assertThat(stock.getMinQuantity()).isEqualTo(8);
    }

    // Blocks stock deletion when active invoice lines still reference the product.
    @Test
    void softDeleteStock_whenUsedInInvoiceLine_throwsAndLeavesActive() {
        Stock stock = stock(5, 7, 2);
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(stock));
        when(invoiceLineItemRepository.existsByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.softDeleteStock(5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invoice");

        assertThat(stock.isDeleted()).isFalse();
        verify(stockRepository, never()).save(stock);
    }

    // Soft deletes unused stock rows.
    @Test
    void softDeleteStock_whenUnused_marksDeleted() {
        Stock stock = stock(5, 7, 2);
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(stock));
        when(invoiceLineItemRepository.existsByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(false);

        service.softDeleteStock(5);

        assertThat(stock.isDeleted()).isTrue();
        verify(stockRepository).save(stock);
    }

    // Rejects restoring stock while the parent product is inactive or missing.
    @Test
    void restoreStock_whenProductInactive_throwsAndDoesNotSave() {
        Stock stock = stock(5, 7, 2);
        stock.setDeleted(true);
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(stock));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.restoreStock(5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("restore product first");

        verify(stockRepository, never()).save(stock);
    }

    // Restores a deleted stock row when the product is active.
    @Test
    void restoreStock_whenProductActive_clearsDeletedFlag() {
        Stock stock = stock(5, 7, 10);
        stock.setDeleted(true);
        Product product = product(5);
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(stock));
        when(productRepository.findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(5, COMPANY_ID))
                .thenReturn(Optional.of(product));
        when(productRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID))
                .thenReturn(Optional.of(product));

        StockResponseDto result = service.restoreStock(5);

        assertThat(result.isDeleted()).isFalse();
        assertThat(result.isLowStock()).isTrue();
        assertThat(stock.isDeleted()).isFalse();
    }

    // Rejects access to stock rows that belong to another company.
    @Test
    void getStockByProductId_whenCompanyDoesNotMatch_throwsAccessDenied() {
        Company otherCompany = new Company();
        otherCompany.setCompanyId(99L);
        Stock stock = stock(5, 7, 2);
        stock.setCompany(otherCompany);
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> service.getStockByProductId(5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("yetkiniz");
    }

    // Rejects soft-deleted stock rows from active lookup.
    @Test
    void getStockByProductId_whenDeleted_throwsNotFound() {
        Stock stock = stock(5, 7, 2);
        stock.setDeleted(true);
        when(stockRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> service.getStockByProductId(5))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock not found");
    }

    // Lists active stock rows and maps low-stock flags.
    @Test
    void getAllActiveStocks_mapsProductAndLowStockState() {
        Stock normal = stock(5, 12, 3);
        Stock low = stock(6, 2, 5);
        when(stockRepository.findActiveStocks(COMPANY_ID)).thenReturn(List.of(normal, low));
        when(productRepository.findByProductIdAndCompanyCompanyId(5, COMPANY_ID)).thenReturn(Optional.of(product(5)));
        when(productRepository.findByProductIdAndCompanyCompanyId(6, COMPANY_ID)).thenReturn(Optional.of(product(6)));

        List<StockResponseDto> result = service.getAllActiveStocks();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isLowStock()).isFalse();
        assertThat(result.get(1).isLowStock()).isTrue();
    }

    // Lists low stock rows supplied by the repository.
    @Test
    void getLowStockItems_mapsRepositoryRows() {
        Stock low = stock(6, 2, 5);
        when(stockRepository.findLowStockItems(COMPANY_ID)).thenReturn(List.of(low));
        when(productRepository.findByProductIdAndCompanyCompanyId(6, COMPANY_ID)).thenReturn(Optional.of(product(6)));

        List<StockResponseDto> result = service.getLowStockItems();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(6);
            assertThat(item.isLowStock()).isTrue();
        });
    }

    private StockRequestDto request(Integer productId, Integer quantity, Integer minQuantity) {
        return new StockRequestDto(productId, quantity, minQuantity);
    }

    private Product product(Integer productId) {
        Product product = new Product();
        product.setProductId(productId);
        product.setCompany(company);
        product.setName("Urun " + productId);
        product.setBarcode("BR-" + productId);
        product.setDeleted(false);
        return product;
    }

    private Stock stock(Integer productId, Integer quantity, Integer minQuantity) {
        Stock stock = new Stock();
        stock.setStockId(productId * 10);
        stock.setCompany(company);
        stock.setProductId(productId);
        stock.setQuantity(quantity);
        stock.setMinQuantity(minQuantity);
        stock.setLastCountDate(LocalDateTime.of(2026, 5, productId, 10, 0));
        stock.setDeleted(false);
        return stock;
    }
}
