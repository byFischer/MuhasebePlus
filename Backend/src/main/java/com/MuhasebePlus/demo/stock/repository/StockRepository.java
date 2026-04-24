package com.MuhasebePlus.demo.stock.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.stock.entity.Stock;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Integer> {

    Optional<Stock> findByProductIdAndCompanyCompanyId(Integer productId, Long companyId);

    boolean existsByProductIdAndCompanyCompanyId(Integer productId, Long companyId);

    void deleteByProductId(Integer productId);

    @Query("""
        select s
        from Stock s
        where s.company.companyId = :companyId
          and s.isDeleted = false
          and s.minQuantity is not null
          and s.quantity < s.minQuantity
        order by s.quantity asc
    """)
    List<Stock> findLowStockItems(@Param("companyId") Long companyId);

    @Query("""
        select s
        from Stock s
        where s.company.companyId = :companyId
          and s.isDeleted = false
        order by s.updatedAt desc
    """)
    List<Stock> findActiveStocks(@Param("companyId") Long companyId);
}