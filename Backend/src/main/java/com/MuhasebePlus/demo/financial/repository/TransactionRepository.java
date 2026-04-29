package com.MuhasebePlus.demo.financial.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionIdAndCompanyCompanyIdAndIsDeletedFalse(Long transactionId, Long companyId);

    Optional<Transaction> findByTransactionIdAndCompanyCompanyId(Long transactionId, Long companyId);

    List<Transaction> findByCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDescTransactionIdDesc(Long companyId);

    List<Transaction> findByAccountIdAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
            Long accountId, Long companyId);

    List<Transaction> findByTransactionTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
            TransactionType transactionType, Long companyId);

    List<Transaction> findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
            LocalDate startDate, LocalDate endDate, Long companyId);

    boolean existsByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(Long accountId, Long companyId);

    List<Transaction> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.transactionType = :income THEN t.amount ELSE -t.amount END), 0) " +
           "FROM Transaction t " +
           "WHERE t.accountId = :accountId AND t.company.companyId = :companyId AND t.isDeleted = false")
    BigDecimal calculateBalanceForAccount(
            @Param("accountId") Long accountId,
            @Param("companyId") Long companyId,
            @Param("income") TransactionType income);

    @Query("SELECT t.accountId, " +
           "COALESCE(SUM(CASE WHEN t.transactionType = :income THEN t.amount ELSE -t.amount END), 0) " +
           "FROM Transaction t " +
           "WHERE t.company.companyId = :companyId AND t.isDeleted = false " +
           "GROUP BY t.accountId")
    List<Object[]> calculateBalancesByCompany(
            @Param("companyId") Long companyId,
            @Param("income") TransactionType income);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionType = :type " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "AND t.company.companyId = :companyId AND t.isDeleted = false")
    BigDecimal sumByTypeAndDateRange(
            @Param("type") TransactionType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("companyId") Long companyId);
}
