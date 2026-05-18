package com.MuhasebePlus.demo.accounting.repository;

import com.MuhasebePlus.demo.accounting.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, Long> {

    List<JournalEntryLine> findByEntryIdOrderByLineOrderAsc(Long entryId);

    List<JournalEntryLine> findByAccountIdAndCompanyCompanyId(Long accountId, Long companyId);

    // Used for trial balance: sum debits and credits per account within a date range
    @Query("""
            SELECT jel.accountId,
                   SUM(jel.debitAmount)  AS totalDebit,
                   SUM(jel.creditAmount) AS totalCredit
            FROM JournalEntryLine jel
            JOIN jel.entry je
            WHERE jel.company.companyId = :companyId
              AND je.entryDate BETWEEN :startDate AND :endDate
              AND je.isDeleted = false
              AND je.isReversed = false
            GROUP BY jel.accountId
            """)
    List<Object[]> sumByAccountForPeriod(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Used for general ledger: all lines for one account within a date range
    @Query("""
            SELECT jel FROM JournalEntryLine jel
            JOIN jel.entry je
            WHERE jel.company.companyId = :companyId
              AND jel.accountId = :accountId
              AND je.entryDate BETWEEN :startDate AND :endDate
              AND je.isDeleted = false
              AND je.isReversed = false
            ORDER BY je.entryDate ASC, je.entryId ASC, jel.lineOrder ASC
            """)
    List<JournalEntryLine> findForGeneralLedger(
            @Param("companyId") Long companyId,
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
