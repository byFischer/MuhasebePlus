package com.MuhasebePlus.demo.financial;

import com.MuhasebePlus.demo.financial.dto.request.BudgetRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BankStatementLineDto;
import com.MuhasebePlus.demo.financial.dto.response.BudgetResponseDto;
import com.MuhasebePlus.demo.financial.entity.BankConstants;
import com.MuhasebePlus.demo.financial.entity.BankStatement;
import com.MuhasebePlus.demo.financial.entity.BankStatementLine;
import com.MuhasebePlus.demo.financial.entity.ReconciliationMatch;
import com.MuhasebePlus.demo.financial.entity.TransactionType;
import com.MuhasebePlus.demo.financial.mapper.BankAccountMapper;
import com.MuhasebePlus.demo.financial.mapper.TransactionMapper;
import com.MuhasebePlus.demo.financial.mapper.TransactionTemplateMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialDtoEntityTest {

    @Test
    void budgetRecordsAndMarkerMappersExposeValues() {
        BudgetRequestDto request = new BudgetRequestDto(
                2026,
                5,
                "Kira",
                TransactionType.EXPENSE,
                new BigDecimal("1000.00"),
                "Aylik"
        );
        BudgetResponseDto response = new BudgetResponseDto(
                1L,
                request.year(),
                request.month(),
                request.category(),
                request.transactionType(),
                request.plannedAmount(),
                request.notes(),
                null,
                null
        );

        assertThat(response.category()).isEqualTo("Kira");
        assertThat(response.plannedAmount()).isEqualByComparingTo("1000.00");
        assertThat(new BankAccountMapper()).isNotNull();
        assertThat(new TransactionMapper()).isNotNull();
        assertThat(new TransactionTemplateMapper()).isNotNull();
        assertThat(BankConstants.BANK_NAMES).contains("Halkbank");
    }

    @Test
    void bankStatementLifecycleAndLineDtoExposeValues() {
        BankStatement statement = BankStatement.builder()
                .id(10L)
                .statementDate(LocalDate.of(2026, 1, 31))
                .build();
        BankStatementLine line = BankStatementLine.builder()
                .id(20L)
                .bankStatement(statement)
                .transactionDate(LocalDate.of(2026, 1, 15))
                .description("EFT")
                .amount(new BigDecimal("500.00"))
                .balance(new BigDecimal("1500.00"))
                .build();
        statement.getLines().add(line);

        ReflectionTestUtils.invokeMethod(statement, "onCreate");
        ReflectionTestUtils.invokeMethod(statement, "onUpdate");

        BankStatementLineDto dto = new BankStatementLineDto(
                line.getId(),
                line.getTransactionDate(),
                line.getDescription(),
                line.getAmount(),
                line.getBalance(),
                line.isMatched(),
                99L
        );

        assertThat(statement.getCreatedAt()).isNotNull();
        assertThat(statement.getUpdatedAt()).isNotNull();
        assertThat(statement.getImportDate()).isNotNull();
        assertThat(statement.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(statement.getOpeningBalance()).isEqualByComparingTo("0");
        assertThat(dto.matchedTransactionId()).isEqualTo(99L);
    }

    @Test
    void reconciliationMatchDefaultsMatchedAtOnCreate() {
        ReconciliationMatch match = ReconciliationMatch.builder()
                .id(1L)
                .companyId(1L)
                .transactionId(50L)
                .statementLine(new BankStatementLine())
                .build();

        ReflectionTestUtils.invokeMethod(match, "onCreate");

        assertThat(match.getMatchType()).isEqualTo("MANUAL");
        assertThat(match.getMatchedAt()).isNotNull();
    }
}
