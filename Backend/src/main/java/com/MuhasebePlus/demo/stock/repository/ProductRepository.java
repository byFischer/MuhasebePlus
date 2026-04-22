package com.MuhasebePlus.demo.stock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.stock.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findByProductIdAndIsDeletedFalse(Integer productId);

    Optional<Product> findByBarcodeAndIsDeletedFalse(String barcode);

    boolean existsByBarcodeAndIsDeletedFalse(String barcode);

    boolean existsByBarcodeAndProductIdNotAndIsDeletedFalse(String barcode, Integer productId);

    List<Product> findAllByIsDeletedFalseOrderByProductIdDesc();
}
