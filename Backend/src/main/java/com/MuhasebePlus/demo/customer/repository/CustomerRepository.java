package com.MuhasebePlus.demo.customer.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.MuhasebePlus.demo.customer.entity.Customer;
import com.MuhasebePlus.demo.customer.entity.CustomerType;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByTaxNumberAndCompanyCompanyId(String taxNumber, Long companyId);

    Optional<Customer> findByCustomerIdAndCompanyCompanyIdAndIsDeletedFalse(Long customerId, Long companyId);

    boolean existsByTaxNumberAndCompanyCompanyId(String taxNumber, Long companyId);
    boolean existsByTaxNumberAndCompanyCompanyIdAndIsDeletedFalse(String taxNumber, Long companyId);

    boolean existsByAccountCodeAndCompanyCompanyIdAndIsDeletedFalse(String accountCode, Long companyId);

    List<Customer> findByCompanyCompanyIdAndIsDeletedFalse(Long companyId);
    Page<Customer> findByCompanyCompanyIdAndIsDeletedFalse(Long companyId, Pageable pageable);

    long countByCompanyCompanyIdAndIsDeletedFalse(Long companyId);

    List<Customer> findByCompanyCompanyIdAndIsDeletedTrue(Long companyId);

    List<Customer> findByTypeAndCompanyCompanyId(CustomerType type, Long companyId);
    List<Customer> findByTypeAndCompanyCompanyIdAndIsDeletedFalse(CustomerType type, Long companyId);
    Page<Customer> findByTypeAndCompanyCompanyIdAndIsDeletedFalse(CustomerType type, Long companyId, Pageable pageable);

    Optional<Customer> findByCustomerIdAndCompanyCompanyId(Long customerId, Long companyId);
    boolean existsByCustomerIdAndCompanyCompanyId(Long customerId, Long companyId);

    List<Customer> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);

    @Query("SELECT c FROM Customer c WHERE c.company.companyId = :companyId AND c.isDeleted = false AND " +
        "(LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.taxNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.city) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.accountCode) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Customer> searchActive(@Param("companyId") Long companyId, @Param("q") String query);

    @Query(value = "SELECT c FROM Customer c WHERE c.company.companyId = :companyId AND c.isDeleted = false AND " +
        "(LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.taxNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.city) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.accountCode) LIKE LOWER(CONCAT('%', :q, '%')))",
        countQuery = "SELECT COUNT(c) FROM Customer c WHERE c.company.companyId = :companyId AND c.isDeleted = false AND " +
        "(LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.taxNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.city) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
        " LOWER(c.accountCode) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Customer> searchActivePage(@Param("companyId") Long companyId, @Param("q") String query, Pageable pageable);
}
