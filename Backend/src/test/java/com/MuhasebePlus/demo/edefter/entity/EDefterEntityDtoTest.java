package com.MuhasebePlus.demo.edefter.entity;

import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.edefter.dto.EDefterRunResponseDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EDefterRun entity, EDefterStatus enum ve EDefterRunResponseDto record için
 * temel kapsama testleri (builder, getter/setter, varsayılan değerler, soft-delete).
 */
class EDefterEntityDtoTest {

    @Test
    void builder_setsAllFields() {
        Company company = Company.builder().companyId(1L).companyName("Test A.Ş.").build();
        LocalDateTime generatedAt = LocalDateTime.of(2026, 3, 1, 10, 30);

        EDefterRun run = EDefterRun.builder()
                .runId(42L)
                .company(company)
                .year(2026)
                .month(3)
                .journalXml("<journal/>")
                .ledgerXml("<ledger/>")
                .journalEntryCount(5)
                .journalLineCount(12)
                .status(EDefterStatus.ARCHIVED)
                .generatedAt(generatedAt)
                .build();

        assertThat(run.getRunId()).isEqualTo(42L);
        assertThat(run.getCompany()).isSameAs(company);
        assertThat(run.getYear()).isEqualTo(2026);
        assertThat(run.getMonth()).isEqualTo(3);
        assertThat(run.getJournalXml()).isEqualTo("<journal/>");
        assertThat(run.getLedgerXml()).isEqualTo("<ledger/>");
        assertThat(run.getJournalEntryCount()).isEqualTo(5);
        assertThat(run.getJournalLineCount()).isEqualTo(12);
        assertThat(run.getStatus()).isEqualTo(EDefterStatus.ARCHIVED);
        assertThat(run.getGeneratedAt()).isEqualTo(generatedAt);
    }

    @Test
    void builder_defaultsStatusToGenerated() {
        EDefterRun run = EDefterRun.builder().year(2026).month(1).build();

        assertThat(run.getStatus()).isEqualTo(EDefterStatus.GENERATED);
    }

    @Test
    void noArgsConstructor_andSettersWork() {
        EDefterRun run = new EDefterRun();
        run.setRunId(7L);
        run.setYear(2025);
        run.setMonth(12);
        run.setJournalEntryCount(0);
        run.setStatus(EDefterStatus.GENERATED);

        assertThat(run.getRunId()).isEqualTo(7L);
        assertThat(run.getYear()).isEqualTo(2025);
        assertThat(run.getMonth()).isEqualTo(12);
        assertThat(run.getJournalEntryCount()).isZero();
    }

    @Test
    void softDelete_fieldsSetCorrectly() {
        EDefterRun run = EDefterRun.builder().year(2026).month(1).build();
        assertThat(run.isDeleted()).isFalse();

        LocalDateTime now = LocalDateTime.now();
        run.setDeleted(true);
        run.setDeletedAt(now);

        assertThat(run.isDeleted()).isTrue();
        assertThat(run.getDeletedAt()).isEqualTo(now);
    }

    @Test
    void status_enumExposesExpectedValues() {
        assertThat(EDefterStatus.values()).containsExactly(EDefterStatus.GENERATED, EDefterStatus.ARCHIVED);
        assertThat(EDefterStatus.valueOf("GENERATED")).isEqualTo(EDefterStatus.GENERATED);
        assertThat(EDefterStatus.valueOf("ARCHIVED")).isEqualTo(EDefterStatus.ARCHIVED);
    }

    @Test
    void responseDto_exposesAllComponents() {
        LocalDateTime generatedAt = LocalDateTime.of(2026, 4, 1, 9, 0);
        EDefterRunResponseDto dto = new EDefterRunResponseDto(
                10L, 2026, 4, EDefterStatus.GENERATED, 8, 16, generatedAt);

        assertThat(dto.runId()).isEqualTo(10L);
        assertThat(dto.year()).isEqualTo(2026);
        assertThat(dto.month()).isEqualTo(4);
        assertThat(dto.status()).isEqualTo(EDefterStatus.GENERATED);
        assertThat(dto.journalEntryCount()).isEqualTo(8);
        assertThat(dto.journalLineCount()).isEqualTo(16);
        assertThat(dto.generatedAt()).isEqualTo(generatedAt);
    }

    @Test
    void responseDto_equalityAndToString() {
        LocalDateTime t = LocalDateTime.of(2026, 4, 1, 9, 0);
        EDefterRunResponseDto a = new EDefterRunResponseDto(10L, 2026, 4, EDefterStatus.GENERATED, 8, 16, t);
        EDefterRunResponseDto b = new EDefterRunResponseDto(10L, 2026, 4, EDefterStatus.GENERATED, 8, 16, t);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("runId=10");
    }
}
