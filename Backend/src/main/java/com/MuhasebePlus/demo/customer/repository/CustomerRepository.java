package com.MuhasebePlus.demo.customer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerType;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByTaxNumber(String taxNumber);

    Optional<Customer> findByCustomerIdAndIsDeletedFalse(Long customerId);

    boolean existsByTaxNumber(String taxNumber);
    boolean existsByTaxNumberAndIsDeletedFalse(String taxNumber);

    List<Customer> findByIsDeletedFalse();
    List<Customer> findByIsDeletedTrue();
    List<Customer> findByType(CustomerType type);
    List<Customer> findByTypeAndIsDeletedFalse(CustomerType type);

    // Arama (CARI.1 Müşteri Arama use case)
    @Query("SELECT c FROM Customer c WHERE c.isDeleted = false AND " +
        "(LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.taxNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.city) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Customer> searchActive(@Param("q") String query);
}
