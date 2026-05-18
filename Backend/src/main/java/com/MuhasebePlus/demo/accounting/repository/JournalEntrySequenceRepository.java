package com.MuhasebePlus.demo.accounting.repository;

import com.MuhasebePlus.demo.accounting.entity.JournalEntrySequence;
import com.MuhasebePlus.demo.accounting.entity.JournalEntrySequenceId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JournalEntrySequenceRepository extends JpaRepository<JournalEntrySequence, JournalEntrySequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM JournalEntrySequence s WHERE s.companyId = :companyId AND s.year = :year")
    Optional<JournalEntrySequence> findByCompanyIdAndYearForUpdate(
            @Param("companyId") Long companyId, @Param("year") int year);
}
