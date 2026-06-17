package com.MuhasebePlus.demo.financial.service;

import com.MuhasebePlus.demo.common.service.CompanyContext;
import com.MuhasebePlus.demo.company.entity.Company;
import com.MuhasebePlus.demo.company.repository.CompanyRepository;
import com.MuhasebePlus.demo.financial.dto.request.ManualMatchRequestDto;
import com.MuhasebePlus.demo.financial.dto.response.BankStatementResponseDto;
import com.MuhasebePlus.demo.financial.entity.BankAccount;
import com.MuhasebePlus.demo.financial.entity.BankStatement;
import com.MuhasebePlus.demo.financial.entity.BankStatementLine;
import com.MuhasebePlus.demo.financial.entity.ReconciliationMatch;
import com.MuhasebePlus.demo.financial.entity.Transaction;
import com.MuhasebePlus.demo.financial.repository.BankAccountRepository;
import com.MuhasebePlus.demo.financial.repository.BankStatementLineRepository;
import com.MuhasebePlus.demo.financial.repository.BankStatementRepository;
import com.MuhasebePlus.demo.financial.repository.ReconciliationMatchRepository;
import com.MuhasebePlus.demo.financial.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconciliationServiceTest {

    @Mock private BankStatementRepository statementRepository;
    @Mock private BankStatementLineRepository lineRepository;
    @Mock private ReconciliationMatchRepository matchRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private CompanyContext companyContext;
    @Mock private CompanyRepository companyRepository;

    @InjectMocks
    private ReconciliationService service;

    private static final Long COMPANY_ID = 1L;
    private static final Long ACCOUNT_ID = 22L;
    private static final Long STATEMENT_ID = 33L;

    private Company company;
    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setCompanyId(COMPANY_ID);
        company.setCompanyName("Test A.S.");

        bankAccount = new BankAccount();
        bankAccount.setAccountId(ACCOUNT_ID);
        bankAccount.setCompany(company);
        bankAccount.setBankName("Test Bank");

        when(companyContext.getCurrentCompanyId()).thenReturn(COMPANY_ID);
        when(companyRepository.getReferenceById(COMPANY_ID)).thenReturn(company);
        when(statementRepository.save(any())).thenAnswer(inv -> {
            BankStatement statement = inv.getArgument(0);
            if (statement.getId() == null) {
                statement.setId(STATEMENT_ID);
            }
            return statement;
        });
        when(lineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // Lists statements without loading detail lines.
    @Test
    void getAll_mapsStatementSummaries() {
        BankStatement statement = statement();
        when(statementRepository.findByCompanyCompanyIdOrderByCreatedAtDesc(COMPANY_ID))
                .thenReturn(List.of(statement));
        when(lineRepository.countByBankStatementId(STATEMENT_ID)).thenReturn(2L);
        when(lineRepository.countByBankStatementIdAndIsMatchedTrue(STATEMENT_ID)).thenReturn(1L);

        List<BankStatementResponseDto> result = service.getAll();

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(STATEMENT_ID);
            assertThat(dto.bankAccountName()).isEqualTo("Test Bank");
            assertThat(dto.totalLines()).isEqualTo(2);
            assertThat(dto.matchedLines()).isEqualTo(1);
            assertThat(dto.lines()).isEmpty();
        });
    }

    // Loads statement detail lines and attached transaction ids.
    @Test
    void getById_whenStatementExists_includesLineDtosAndMatches() {
        BankStatement statement = statement();
        BankStatementLine line = line(101L, statement, "1250.00", false);
        when(statementRepository.findByIdAndCompanyCompanyId(STATEMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(statement));
        when(lineRepository.countByBankStatementId(STATEMENT_ID)).thenReturn(1L);
        when(lineRepository.countByBankStatementIdAndIsMatchedTrue(STATEMENT_ID)).thenReturn(0L);
        when(lineRepository.findByBankStatementId(STATEMENT_ID)).thenReturn(List.of(line));
        when(matchRepository.findByStatementLineIdAndCompanyId(101L, COMPANY_ID))
                .thenReturn(Optional.of(match(line, 901L, "MANUAL")));

        BankStatementResponseDto result = service.getById(STATEMENT_ID);

        assertThat(result.lines()).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(101L);
            assertThat(dto.matchedTransactionId()).isEqualTo(901L);
        });
    }

    // Rejects lookup when the statement does not belong to the current company.
    @Test
    void getById_whenMissing_throwsRuntimeException() {
        when(statementRepository.findByIdAndCompanyCompanyId(STATEMENT_ID, COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(STATEMENT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ekstre");
    }

    // Imports CSV statement lines and persists parsed rows.
    @Test
    void importCsv_whenValidFile_savesStatementAndParsedLines() {
        String csv = "date;description;amount;balance\n"
                + "20/05/2026;Tahsilat;1250;3000\n"
                + "2026-05-21;Odeme;-250;2750\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "statement.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyId(ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        ArgumentCaptor<List<BankStatementLine>> linesCaptor = ArgumentCaptor.captor();

        BankStatementResponseDto result = service.importCsv(
                file,
                ACCOUNT_ID,
                LocalDate.of(2026, 5, 31),
                null,
                new BigDecimal("2750.00")
        );

        assertThat(result.id()).isEqualTo(STATEMENT_ID);
        verify(lineRepository).saveAll(linesCaptor.capture());
        assertThat(linesCaptor.getValue()).hasSize(2);
        assertThat(linesCaptor.getValue().get(0).getTransactionDate()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(linesCaptor.getValue().get(0).getAmount()).isEqualByComparingTo("1250");
        assertThat(linesCaptor.getValue().get(1).getAmount()).isEqualByComparingTo("-250");
    }

    // Imports XLSX statement rows through the spreadsheet parser path.
    @Test
    void importCsv_whenSpreadsheetContentType_savesParsedXlsxRows() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "statement.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes()
        );
        when(bankAccountRepository.findByAccountIdAndCompanyCompanyId(ACCOUNT_ID, COMPANY_ID))
                .thenReturn(Optional.of(bankAccount));
        ArgumentCaptor<List<BankStatementLine>> linesCaptor = ArgumentCaptor.captor();

        service.importCsv(
                file,
                ACCOUNT_ID,
                LocalDate.of(2026, 5, 31),
                new BigDecimal("1000.00"),
                new BigDecimal("3500.00")
        );

        verify(lineRepository).saveAll(linesCaptor.capture());
        assertThat(linesCaptor.getValue()).singleElement().satisfies(line -> {
            assertThat(line.getTransactionDate()).isEqualTo(LocalDate.of(2026, 5, 22));
            assertThat(line.getDescription()).isEqualTo("Xlsx Tahsilat");
            assertThat(line.getAmount()).isEqualByComparingTo("1250");
            assertThat(line.getBalance()).isEqualByComparingTo("3500");
        });
    }

    // Auto-matches same amount transactions within the configured date tolerance.
    @Test
    void autoMatch_whenTransactionWithinTolerance_createsAutoMatchAndCompletesStatement() {
        BankStatement statement = statement();
        BankStatementLine line = line(101L, statement, "1250.00", false);
        Transaction transaction = transaction(901L, "1250.00", LocalDate.of(2026, 5, 22));
        when(statementRepository.findByIdAndCompanyCompanyId(STATEMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(statement));
        when(lineRepository.findByBankStatementIdAndIsMatchedFalse(STATEMENT_ID)).thenReturn(List.of(line));
        when(transactionRepository.findByAccountIdAndCompanyCompanyIdAndIsDeletedFalseOrderByTransactionDateDesc(ACCOUNT_ID, COMPANY_ID))
                .thenReturn(List.of(transaction));
        when(matchRepository.findAll()).thenReturn(List.of());
        when(lineRepository.countByBankStatementId(STATEMENT_ID)).thenReturn(1L);
        when(lineRepository.countByBankStatementIdAndIsMatchedTrue(STATEMENT_ID)).thenReturn(1L);
        when(lineRepository.findByBankStatementId(STATEMENT_ID)).thenReturn(List.of(line));
        when(matchRepository.findByStatementLineIdAndCompanyId(101L, COMPANY_ID))
                .thenReturn(Optional.of(match(line, 901L, "AUTO")));

        BankStatementResponseDto result = service.autoMatch(STATEMENT_ID);

        assertThat(line.isMatched()).isTrue();
        assertThat(statement.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.matchedLines()).isEqualTo(1);
        verify(lineRepository).save(line);
        verify(matchRepository).save(any(ReconciliationMatch.class));
        verify(statementRepository).save(statement);
    }

    // Manual match replaces old match data and stores the selected transaction id.
    @Test
    void manualMatch_whenLineExists_replacesMatchAndCompletesStatement() {
        BankStatement statement = statement();
        BankStatementLine line = line(101L, statement, "1250.00", false);
        when(lineRepository.findById(101L)).thenReturn(Optional.of(line));
        when(lineRepository.countByBankStatementId(STATEMENT_ID)).thenReturn(1L);
        when(lineRepository.countByBankStatementIdAndIsMatchedTrue(STATEMENT_ID)).thenReturn(1L);
        when(statementRepository.findByIdAndCompanyCompanyId(STATEMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(statement));
        when(lineRepository.findByBankStatementId(STATEMENT_ID)).thenReturn(List.of(line));
        when(matchRepository.findByStatementLineIdAndCompanyId(101L, COMPANY_ID))
                .thenReturn(Optional.of(match(line, 901L, "MANUAL")));

        BankStatementResponseDto result = service.manualMatch(new ManualMatchRequestDto(101L, 901L));

        assertThat(line.isMatched()).isTrue();
        assertThat(statement.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.lines()).singleElement().satisfies(dto -> assertThat(dto.matchedTransactionId()).isEqualTo(901L));
        verify(matchRepository).deleteByStatementLineId(101L);
        verify(matchRepository).save(any(ReconciliationMatch.class));
    }

    // Unmatching a line clears match state and moves statement back to in progress.
    @Test
    void unmatch_whenLineExists_clearsMatchAndReopensStatement() {
        BankStatement statement = statement();
        statement.setStatus("COMPLETED");
        BankStatementLine line = line(101L, statement, "1250.00", true);
        when(lineRepository.findById(101L)).thenReturn(Optional.of(line));
        when(statementRepository.findByIdAndCompanyCompanyId(STATEMENT_ID, COMPANY_ID))
                .thenReturn(Optional.of(statement));
        when(lineRepository.countByBankStatementId(STATEMENT_ID)).thenReturn(1L);
        when(lineRepository.countByBankStatementIdAndIsMatchedTrue(STATEMENT_ID)).thenReturn(0L);
        when(lineRepository.findByBankStatementId(STATEMENT_ID)).thenReturn(List.of(line));

        BankStatementResponseDto result = service.unmatch(101L);

        assertThat(line.isMatched()).isFalse();
        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        verify(matchRepository).deleteByStatementLineId(101L);
        verify(statementRepository).save(statement);
    }

    // Rejects manual match when the statement line cannot be found.
    @Test
    void manualMatch_whenLineMissing_throwsRuntimeException() {
        when(lineRepository.findById(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.manualMatch(new ManualMatchRequestDto(101L, 901L)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Sat");
    }

    private BankStatement statement() {
        BankStatement statement = new BankStatement();
        statement.setId(STATEMENT_ID);
        statement.setCompany(company);
        statement.setBankAccount(bankAccount);
        statement.setStatementDate(LocalDate.of(2026, 5, 31));
        statement.setOpeningBalance(new BigDecimal("1000.00"));
        statement.setClosingBalance(new BigDecimal("2250.00"));
        statement.setStatus("IN_PROGRESS");
        return statement;
    }

    private BankStatementLine line(Long id, BankStatement statement, String amount, boolean matched) {
        BankStatementLine line = new BankStatementLine();
        line.setId(id);
        line.setBankStatement(statement);
        line.setTransactionDate(LocalDate.of(2026, 5, 20));
        line.setDescription("Banka hareketi");
        line.setAmount(new BigDecimal(amount));
        line.setBalance(new BigDecimal("2250.00"));
        line.setMatched(matched);
        return line;
    }

    private Transaction transaction(Long id, String amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(id);
        transaction.setCompany(company);
        transaction.setAccountId(ACCOUNT_ID);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTransactionDate(date);
        transaction.setDeleted(false);
        return transaction;
    }

    private ReconciliationMatch match(BankStatementLine line, Long transactionId, String type) {
        return ReconciliationMatch.builder()
                .companyId(COMPANY_ID)
                .statementLine(line)
                .transactionId(transactionId)
                .matchType(type)
                .build();
    }

    private byte[] xlsxBytes() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("statement");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("date");
            header.createCell(1).setCellValue("description");
            header.createCell(2).setCellValue("amount");
            header.createCell(3).setCellValue("balance");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("22.05.2026");
            row.createCell(1).setCellValue("Xlsx Tahsilat");
            row.createCell(2).setCellValue("1250");
            row.createCell(3).setCellValue("3500");

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
