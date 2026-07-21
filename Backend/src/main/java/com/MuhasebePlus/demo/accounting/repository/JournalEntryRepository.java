package com.MuhasebePlus.demo.accounting.repository;

import com.MuhasebePlus.demo.accounting.entity.JournalEntry;
import com.MuhasebePlus.demo.accounting.entity.JournalSourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByCompanyCompanyIdAndEntryIdAndIsDeletedFalse(Long companyId, Long entryId);

    Optional<JournalEntry> findByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
            Long companyId, JournalSourceType sourceType, Long sourceId);

    Page<JournalEntry> findByCompanyCompanyIdAndIsDeletedFalseOrderByEntryDateDescEntryIdDesc(
            Long companyId, Pageable pageable);

    List<JournalEntry> findByCompanyCompanyIdAndEntryDateBetweenAndIsDeletedFalseOrderByEntryDateAscEntryIdAsc(
            Long companyId, LocalDate startDate, LocalDate endDate);

    boolean existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
            Long companyId, JournalSourceType sourceType, Long sourceId);
}
