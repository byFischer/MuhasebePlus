package com.MuhasebePlus.demo.stock.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.stock.dto.request.ProductRequestDto;
import com.MuhasebePlus.demo.stock.dto.response.ProductResponseDto;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final CompanyContext companyContext;
    private final CompanyRepository companyRepository;

    //PUBLIC METOTLAR

    public ProductResponseDto createProduct(ProductRequestDto dto) {
        Long companyId = companyContext.getCurrentCompanyId();
        
        if(productRepository.existsByBarcodeAndCompanyCompanyIdAndIsDeletedFalse(dto.barcode(), companyId)) {
            throw new RuntimeException("A product with the same barcode already exists in your company: " + dto.barcode());
        }

        Product product = new Product();
        product.setCompany(companyRepository.getReferenceById(companyId));
        product.setBarcode(dto.barcode());
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setUnit(dto.unit());
        product.setSalePrice(dto.salePrice());
        product.setVatRate(dto.vatRate());
        product.setCostPrice(dto.costPrice());
        product.setDeleted(false);

        Product saved = productRepository.save(product);

        return toResponseDto(saved);
    }

    public List<ProductResponseDto> getAllProducts() {
        Long companyId = companyContext.getCurrentCompanyId();
        List<Product> products = productRepository.findAllByCompanyCompanyIdAndIsDeletedFalseOrderByProductIdDesc(companyId);
        return products.stream().map(this::toResponseDto).toList();
    }

    public ProductResponseDto getProductById(Integer id) {
        Product p = findProductEntityById(id);
        return toResponseDto(p);
    }

    public ProductResponseDto getProductByBarcode(String barcode) {
        Long companyId = companyContext.getCurrentCompanyId();
        Product product = productRepository.findByBarcodeAndCompanyCompanyIdAndIsDeletedFalse(barcode, companyId)
            .orElseThrow(() -> new RuntimeException("Product not found with barcode: " + barcode));
        return toResponseDto(product);
    }

    public ProductResponseDto updateProduct(Integer id, ProductRequestDto dto) {
        Product product = findProductEntityById(id);
        Long companyId = companyContext.getCurrentCompanyId();

        if (!product.getBarcode().equals(dto.barcode()) &&
                productRepository.existsByBarcodeAndProductIdNotAndCompanyCompanyIdAndIsDeletedFalse(dto.barcode(), id, companyId)) {
            throw new RuntimeException("A product with the same barcode already exists in your company: " + dto.barcode());
        }

        product.setBarcode(dto.barcode());
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setUnit(dto.unit());
        product.setSalePrice(dto.salePrice());
        product.setVatRate(dto.vatRate());
        product.setCostPrice(dto.costPrice());

        Product updated = productRepository.save(product);

        return toResponseDto(updated);
    }

    public void softDeleteProduct(Integer id) {
        Product product = findProductEntityById(id);
        Long companyId = companyContext.getCurrentCompanyId();

        if (invoiceLineItemRepository.existsByProductIdAndCompanyCompanyIdAndIsDeletedFalse(id, companyId)) {
            throw new RuntimeException(
                "Product is used in active invoice line items; cannot delete: " + id);
        }

        product.setDeleted(true);
        productRepository.save(product);

        // İlgili stok satırı varsa onu da soft-delete et (cascade)
        stockRepository.findByProductIdAndCompanyCompanyId(id, companyId).ifPresent(stock -> {
            stock.setDeleted(true);
            stockRepository.save(stock);
        });
    }

    public ProductResponseDto restoreProduct(Integer id) {
        Long companyId = companyContext.getCurrentCompanyId();
        
        // 1) Silinmiş olanı da bulabilmek için findByProductIdAndCompanyCompanyId kullanıyoruz (isDeleted filtresiz)
        Product product = productRepository.findByProductIdAndCompanyCompanyId(id, companyId)
            .orElseThrow(() -> new RuntimeException("Product not found or access denied for id: " + id));

        // 3) Aynı barkoda sahip başka aktif (silinmemiş) ürün varsa restore engellenir
        if (productRepository.existsByBarcodeAndProductIdNotAndCompanyCompanyIdAndIsDeletedFalse(product.getBarcode(), id, companyId)) {
            throw new RuntimeException(
                "Cannot restore product; another active product already uses barcode: " + product.getBarcode()
            );
        }

        // 2) Ürünü geri yükle
        product.setDeleted(false);
        Product restored = productRepository.save(product);

        // 4) İlgili stok satırı varsa onu da restore et (cascade)
        stockRepository.findByProductIdAndCompanyCompanyId(id, companyId).ifPresent(stock -> {
            stock.setDeleted(false);
            stockRepository.save(stock);
        });

        return toResponseDto(restored);
    }


    //PRIVATE METOTLAR

    private Product findProductEntityById(Integer productId) {
        Long companyId = companyContext.getCurrentCompanyId();
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            
        if (!product.getCompany().getCompanyId().equals(companyId)) {
            throw new RuntimeException("Bu kaydı görüntüleme yetkiniz yok");
        }
        
        if (product.isDeleted()) {
            throw new RuntimeException("Product not found with id: " + productId);
        }
        
        return product;
    }

    private ProductResponseDto toResponseDto(Product product) {
        Long companyId = companyContext.getCurrentCompanyId();
        Stock stock = stockRepository.findByProductIdAndCompanyCompanyId(product.getProductId(), companyId)
            .orElse(null);
            
        return new ProductResponseDto(
            product.getProductId(),
            product.getBarcode(),
            product.getName(),
            product.getDescription(),
            product.getUnit(),
            product.getSalePrice(),
            product.getVatRate(),
            product.getCostPrice(),
            stock != null ? stock.getQuantity() : null,
            stock != null ? stock.getMinQuantity() : null,
            product.isDeleted(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

}
