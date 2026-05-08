package com.MuhasebePlus.demo.stock.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.stock.entity.StockMovement;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductIdAndCompanyCompanyIdAndIsDeletedFalseOrderByCreatedAtDesc(Integer productId, Long companyId);

    List<StockMovement> findByCompanyCompanyIdAndIsDeletedFalseOrderByCreatedAtDesc(Long companyId);

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m WHERE m.productId = :productId AND m.company.companyId = :companyId AND m.isDeleted = false")
    Optional<Integer> sumQuantityByProductId(@Param("productId") Integer productId, @Param("companyId") Long companyId);

    List<StockMovement> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<StockMovement> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);
}
