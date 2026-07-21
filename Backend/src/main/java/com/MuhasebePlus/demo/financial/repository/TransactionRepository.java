package com.MuhasebePlus.demo.financial.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.entity.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByTransactionIdAndCompanyCompanyIdAndIsDeletedFalse(Long transactionId, Long companyId);

    Optional<Transaction> findByTransactionIdAndCompanyCompanyId(Long transactionId, Long companyId);

    List<Transaction> findByCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDescTransactionIdDesc(Long companyId);

    @Query("SELECT t FROM Transaction t " +
           "WHERE (t.accountId = :accountId OR t.transferAccountId = :accountId) " +
           "AND t.company.companyId = :companyId AND t.isDeleted = false " +
           "ORDER BY t.transactionDate DESC, t.transactionId DESC")
    List<Transaction> findByAccountIdAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
            @Param("accountId") Long accountId, @Param("companyId") Long companyId);

    List<Transaction> findByTransactionTypeAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
            TransactionType transactionType, Long companyId);

    List<Transaction> findByTransactionDateBetweenAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(
            LocalDate startDate, LocalDate endDate, Long companyId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t " +
           "WHERE (t.accountId = :accountId OR t.transferAccountId = :accountId) " +
           "AND t.company.companyId = :companyId AND t.isDeleted = false")
    boolean existsByAccountIdAndCompanyCompanyIdAndIsDeletedFalse(
            @Param("accountId") Long accountId, @Param("companyId") Long companyId);

    List<Transaction> findByIsDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);

    @Query("SELECT COALESCE(SUM(CASE " +
           "WHEN t.accountId = :accountId THEN CASE WHEN t.transactionType = :income THEN t.amount ELSE -t.amount END " +
           "WHEN t.transferAccountId = :accountId THEN t.amount " +
           "ELSE 0 END), 0) " +
           "FROM Transaction t " +
           "WHERE (t.accountId = :accountId OR t.transferAccountId = :accountId) " +
           "AND t.company.companyId = :companyId AND t.isDeleted = false")
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
