package com.MuhasebePlus.demo.stock.service;

import java.util.List;

import org.springframework.stereotype.Service;

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


    //PUBLIC METOTLAR

    public ProductResponseDto createProduct(ProductRequestDto dto) {
        
        if(productRepository.existsByBarcodeAndIsDeletedFalse(dto.barcode())) {
            throw new RuntimeException("A product with the same barcode already exists: " + dto.barcode());
        }

        Product product = new Product();
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
        List<Product> products = productRepository.findAllByIsDeletedFalseOrderByProductIdDesc();
        return products.stream().map(this::toResponseDto).toList();
    }

    public ProductResponseDto getProductById(Integer id) {
        Product p = findProductEntityById(id);
        return toResponseDto(p);
    }

    public ProductResponseDto getProductByBarcode(String barcode) {
        Product product = productRepository.findByBarcodeAndIsDeletedFalse(barcode)
            .orElseThrow(() -> new RuntimeException("Product not found with barcode: " + barcode));
        return toResponseDto(product);
    }

    public ProductResponseDto updateProduct(Integer id, ProductRequestDto dto) {
        Product product = findProductEntityById(id);

        if (!product.getBarcode().equals(dto.barcode()) &&
                productRepository.existsByBarcodeAndProductIdNotAndIsDeletedFalse(dto.barcode(), id)) {
            throw new RuntimeException("A product with the same barcode already exists: " + dto.barcode());
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

        if (invoiceLineItemRepository.existsByProductIdAndIsDeletedFalse(id)) {
            throw new RuntimeException(
                "Product is used in active invoice line items; cannot delete: " + id);
        }

        product.setDeleted(true);
        productRepository.save(product);

        // İlgili stok satırı varsa onu da soft-delete et (cascade)
        stockRepository.findByProductId(id).ifPresent(stock -> {
            stock.setDeleted(true);
            stockRepository.save(stock);
        });
    }

    public ProductResponseDto restoreProduct(Integer id) {
        // 1) Silinmiş olanı da bulabilmek için isDeleted filtresiz findById
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // 3) Aynı barkoda sahip başka aktif (silinmemiş) ürün varsa restore engellenir
        if (productRepository.existsByBarcodeAndProductIdNotAndIsDeletedFalse(product.getBarcode(), id)) {
            throw new RuntimeException(
                "Cannot restore product; another active product already uses barcode: " + product.getBarcode()
            );
        }

        // 2) Ürünü geri yükle
        product.setDeleted(false);
        Product restored = productRepository.save(product);

        // 4) İlgili stok satırı varsa onu da restore et (cascade)
        stockRepository.findByProductId(id).ifPresent(stock -> {
            stock.setDeleted(false);
            stockRepository.save(stock);
        });

        return toResponseDto(restored);
    }


//PRIVATE METOTLAR

private Product findProductEntityById(Integer productId) {
    return productRepository.findById(productId)
        .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
}

private ProductResponseDto toResponseDto(Product product) {
    Stock stock = stockRepository.findByProductId(product.getProductId())
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
