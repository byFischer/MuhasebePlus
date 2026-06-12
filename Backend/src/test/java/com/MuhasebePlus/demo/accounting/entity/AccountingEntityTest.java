package com.MuhasebePlus.demo.accounting.entity;

import com.MuhasebePlus.demo.company.entity.Company;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingEntityTest {

    @Test
    void chartOfAccount_defaultsAllArgsAndEqualsWork() {
        Company company = company();
        ChartOfAccount parent = new ChartOfAccount();
        parent.setAccountId(1L);
        parent.setAccountCode("120");

        ChartOfAccount account = new ChartOfAccount(
                2L,
                company,
                "120.00.001",
                "ABC Musteri",
                AccountType.ASSET,
                1L,
                parent,
                false,
                true,
                "Aciklama");

        assertThat(account.getAccountId()).isEqualTo(2L);
        assertThat(account.getCompany()).isEqualTo(company);
        assertThat(account.getAccountCode()).isEqualTo("120.00.001");
        assertThat(account.getAccountName()).isEqualTo("ABC Musteri");
        assertThat(account.getAccountType()).isEqualTo(AccountType.ASSET);
        assertThat(account.getParentId()).isEqualTo(1L);
        assertThat(account.getParent()).isEqualTo(parent);
        assertThat(account.isLeaf()).isFalse();
        assertThat(account.isSystem()).isTrue();
        assertThat(account.getDescription()).isEqualTo("Aciklama");

        ChartOfAccount same = new ChartOfAccount(
                2L, company, "120.00.001", "ABC Musteri", AccountType.ASSET,
                1L, parent, false, true, "Aciklama");
        ChartOfAccount different = new ChartOfAccount();
        different.setAccountId(3L);

        assertThat(account).isEqualTo(account);
        assertThat(account).isEqualTo(same);
        assertThat(account).isNotEqualTo(different);
        assertThat(account).isNotEqualTo("account");

        ChartOfAccount defaults = new ChartOfAccount();
        assertThat(defaults.isLeaf()).isTrue();
        assertThat(defaults.isSystem()).isFalse();
    }

    @Test
    void journalEntry_defaultsAllArgsAndSoftDeleteFieldsWork() {
        Company company = company();
        List<JournalEntryLine> lines = new ArrayList<>();
        JournalEntry entry = new JournalEntry(
                10L,
                company,
                "FIS-2026-0001",
                LocalDate.of(2026, 5, 1),
                "Manuel fis",
                JournalSourceType.MANUAL,
                null,
                true,
                11L,
                lines);
        LocalDateTime deletedAt = LocalDateTime.of(2026, 5, 2, 10, 0);
        entry.setDeleted(true);
        entry.setDeletedAt(deletedAt);

        assertThat(entry.getEntryId()).isEqualTo(10L);
        assertThat(entry.getCompany()).isEqualTo(company);
        assertThat(entry.getEntryNumber()).isEqualTo("FIS-2026-0001");
        assertThat(entry.getEntryDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(entry.getDescription()).isEqualTo("Manuel fis");
        assertThat(entry.getSourceType()).isEqualTo(JournalSourceType.MANUAL);
        assertThat(entry.isReversed()).isTrue();
        assertThat(entry.getReversedEntryId()).isEqualTo(11L);
        assertThat(entry.getLines()).isSameAs(lines);
        assertThat(entry.isDeleted()).isTrue();
        assertThat(entry.getDeletedAt()).isEqualTo(deletedAt);

        JournalEntry defaults = new JournalEntry();
        assertThat(defaults.isReversed()).isFalse();
        assertThat(defaults.getLines()).isEmpty();
    }

    @Test
    void journalEntryLine_defaultsAllArgsAndEntryIdWork() {
        Company company = company();
        JournalEntry entry = new JournalEntry();
        entry.setEntryId(10L);
        ChartOfAccount account = new ChartOfAccount();
        account.setAccountId(100L);
        account.setAccountCode("100");

        JournalEntryLine line = new JournalEntryLine(
                55L,
                company,
                entry,
                100L,
                account,
                new BigDecimal("250.00"),
                BigDecimal.ZERO,
                "Kasa borcu",
                3);

        assertThat(line.getLineId()).isEqualTo(55L);
        assertThat(line.getCompany()).isEqualTo(company);
        assertThat(line.getEntry()).isEqualTo(entry);
        assertThat(line.getEntryId()).isEqualTo(10L);
        assertThat(line.getAccountId()).isEqualTo(100L);
        assertThat(line.getAccount()).isEqualTo(account);
        assertThat(line.getDebitAmount()).isEqualByComparingTo("250.00");
        assertThat(line.getCreditAmount()).isEqualByComparingTo("0.00");
        assertThat(line.getDescription()).isEqualTo("Kasa borcu");
        assertThat(line.getLineOrder()).isEqualTo(3);

        JournalEntryLine defaults = new JournalEntryLine();
        assertThat(defaults.getDebitAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(defaults.getCreditAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(defaults.getLineOrder()).isZero();
        assertThat(defaults.getEntryId()).isNull();
    }

    @Test
    void journalEntrySequence_defaultsAllArgsAndIdEqualityWork() {
        Company company = company();
        JournalEntrySequence sequence = new JournalEntrySequence(1L, company, 2026, 42);

        assertThat(sequence.getCompanyId()).isEqualTo(1L);
        assertThat(sequence.getCompany()).isEqualTo(company);
        assertThat(sequence.getYear()).isEqualTo(2026);
        assertThat(sequence.getLastSeq()).isEqualTo(42);

        JournalEntrySequence defaults = new JournalEntrySequence();
        assertThat(defaults.getLastSeq()).isZero();

        JournalEntrySequenceId id = new JournalEntrySequenceId(1L, 2026);
        JournalEntrySequenceId same = new JournalEntrySequenceId(1L, 2026);
        JournalEntrySequenceId differentCompany = new JournalEntrySequenceId(2L, 2026);
        JournalEntrySequenceId differentYear = new JournalEntrySequenceId(1L, 2025);

        assertThat(id).isEqualTo(id);
        assertThat(id).isEqualTo(same);
        assertThat(id).hasSameHashCodeAs(same);
        assertThat(id).isNotEqualTo(differentCompany);
        assertThat(id).isNotEqualTo(differentYear);
        assertThat(id).isNotEqualTo(null);
        assertThat(id).isNotEqualTo("id");
    }

    private Company company() {
        Company company = new Company();
        company.setCompanyId(1L);
        company.setCompanyName("Test A.S.");
        return company;
    }
}
