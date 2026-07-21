package com.MuhasebePlus.demo.stock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.stock.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findByProductIdAndCompanyCompanyIdAndIsDeletedFalse(Integer productId, Long companyId);

    Optional<Product> findByBarcodeAndCompanyCompanyIdAndIsDeletedFalse(String barcode, Long companyId);

    boolean existsByBarcodeAndCompanyCompanyIdAndIsDeletedFalse(String barcode, Long companyId);

    boolean existsByBarcodeAndProductIdNotAndCompanyCompanyIdAndIsDeletedFalse(String barcode, Integer productId, Long companyId);

    List<Product> findAllByCompanyCompanyIdAndIsDeletedFalseOrderByProductIdDesc(Long companyId);
    
    Optional<Product> findByProductIdAndCompanyCompanyId(Integer productId, Long companyId);

    List<Product> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);

    List<Product> findByProductIdInAndCompanyCompanyId(List<Integer> productIds, Long companyId);
}
