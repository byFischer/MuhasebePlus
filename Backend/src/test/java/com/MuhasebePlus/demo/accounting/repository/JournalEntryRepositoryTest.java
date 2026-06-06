package com.MuhasebePlus.demo.accounting.repository;

import com.MuhasebePlus.demo.TestcontainersConfiguration;
import com.MuhasebePlus.demo.accounting.entity.JournalEntry;
import com.MuhasebePlus.demo.accounting.entity.JournalSourceType;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class JournalEntryRepositoryTest {

    @Autowired JournalEntryRepository entryRepository;
    @Autowired CompanyRepository companyRepository;

    private Company company;
    private Company otherCompany;

    @BeforeEach
    void setUp() {
        company = companyRepository.save(Company.builder()
                .companyName("Test A.Ş.").taxNumber("1111111111").build());
        otherCompany = companyRepository.save(Company.builder()
                .companyName("Diğer A.Ş.").taxNumber("2222222222").build());
    }

    // ── Soft-delete filtering ─────────────────────────────────────────────────

    @Test
    void list_softDeletedEntry_notReturned() {
        entryRepository.save(entry(company, LocalDate.now(), false));
        entryRepository.save(entry(company, LocalDate.now(), true));

        Page<JournalEntry> page = entryRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByEntryDateDescEntryIdDesc(
                        company.getCompanyId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).isDeleted()).isFalse();
    }

    // ── Tenant isolation ──────────────────────────────────────────────────────

    @Test
    void list_tenantIsolation_onlyOwnCompanyReturned() {
        entryRepository.save(entry(company, LocalDate.now(), false));
        entryRepository.save(entry(otherCompany, LocalDate.now(), false));

        Page<JournalEntry> page = entryRepository
                .findByCompanyCompanyIdAndIsDeletedFalseOrderByEntryDateDescEntryIdDesc(
                        company.getCompanyId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCompany().getCompanyId())
                .isEqualTo(company.getCompanyId());
    }

    // ── Date range query ──────────────────────────────────────────────────────

    @Test
    void findByDateRange_onlyReturnsDatesWithinRange() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);

        entryRepository.save(entry(company, LocalDate.of(2025, 6, 15), false));
        entryRepository.save(entry(company, LocalDate.of(2024, 12, 31), false));

        List<JournalEntry> result = entryRepository
                .findByCompanyCompanyIdAndEntryDateBetweenAndIsDeletedFalseOrderByEntryDateAscEntryIdAsc(
                        company.getCompanyId(), start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntryDate()).isEqualTo(LocalDate.of(2025, 6, 15));
    }

    // ── Source type / ID lookup ───────────────────────────────────────────────

    @Test
    void findBySourceTypeAndSourceId_whenReversed_returnsEmpty() {
        JournalEntry reversed = entry(company, LocalDate.now(), false);
        reversed.setSourceType(JournalSourceType.INVOICE);
        reversed.setSourceId(42L);
        reversed.setReversed(true);
        entryRepository.save(reversed);

        Optional<JournalEntry> result = entryRepository
                .findByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                        company.getCompanyId(), JournalSourceType.INVOICE, 42L);

        assertThat(result).isEmpty();
    }

    @Test
    void existsBySourceTypeAndSourceId_whenActiveExists_returnsTrue() {
        JournalEntry e = entry(company, LocalDate.now(), false);
        e.setSourceType(JournalSourceType.PAYMENT);
        e.setSourceId(99L);
        entryRepository.save(e);

        boolean exists = entryRepository
                .existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                        company.getCompanyId(), JournalSourceType.PAYMENT, 99L);

        assertThat(exists).isTrue();
    }

    @Test
    void existsBySourceTypeAndSourceId_whenNotExists_returnsFalse() {
        assertThat(entryRepository
                .existsByCompanyCompanyIdAndSourceTypeAndSourceIdAndIsDeletedFalseAndIsReversedFalse(
                        company.getCompanyId(), JournalSourceType.PAYMENT, 999L))
                .isFalse();
    }

    // ── Yardımcı ──────────────────────────────────────────────────────────────

    private JournalEntry entry(Company c, LocalDate date, boolean deleted) {
        JournalEntry e = new JournalEntry();
        e.setCompany(c);
        e.setEntryNumber("FIS-2025-0001");
        e.setEntryDate(date);
        e.setSourceType(JournalSourceType.MANUAL);
        e.setDeleted(deleted);
        return e;
    }
}
